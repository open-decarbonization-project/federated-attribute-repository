package net.far.repository.web.peers;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.far.resolver.core.PeerRegistry;
import net.far.resolver.model.Peer;
import net.far.resolver.signature.KeyManager;
import net.far.resolver.signature.MessageComponents;
import net.far.resolver.signature.TokenParser;
import net.far.resolver.signature.Verifier;

/**
 * Quarkus identity provider that verifies inbound RFC 9421 request signatures from known peers. On
 * successful verification, produces a {@link SecurityIdentity} with role "peer" and optionally
 * extracts the end-user identity from a forwarded bearer token.
 */
@ApplicationScoped
public class PeerSignatureProvider implements IdentityProvider<PeerSignatureRequest> {

  @Inject PeerRegistry peers;

  @Inject Verifier verifier;

  private static String extract(final String header) {
    var value = header;
    if (value.startsWith("sig1=:")) {
      value = value.substring(6);
    }
    if (value.endsWith(":")) {
      value = value.substring(0, value.length() - 1);
    }
    return value;
  }

  private static ArrayList<String> candidates(final Peer peer) {
    final var result = new ArrayList<String>();
    result.add(peer.key());
    final var now = Instant.now();
    for (final var previous : peer.previous()) {
      if (previous.key() != null && previous.expires() != null && previous.expires().isAfter(now)) {
        result.add(previous.key());
      }
    }
    return result;
  }

  @Override
  public Class<PeerSignatureRequest> getRequestType() {
    return PeerSignatureRequest.class;
  }

  @Override
  public Uni<SecurityIdentity> authenticate(
      final PeerSignatureRequest request, final AuthenticationRequestContext context) {
    return context.runBlocking(() -> verify(request));
  }

  private SecurityIdentity verify(final PeerSignatureRequest request) {
    final var id = verifier.keyid(request.input());
    final var signer = id.contains("#") ? id.substring(0, id.indexOf('#')) : id;
    final var peer =
        peers
            .get(signer)
            .or(() -> peers.match(signer))
            .orElseThrow(() -> new AuthenticationFailedException("Unknown peer: " + signer));

    if (peer.key() == null) {
      throw new AuthenticationFailedException("Peer has no key: " + signer);
    }

    final var value = extract(request.signature());
    final var headers = new LinkedHashMap<String, String>();
    if (request.token() != null) {
      headers.put("authorization", "Bearer " + request.token());
    }
    if (request.chain() != null) {
      headers.put("far-delegation-chain", request.chain());
    }
    final var components =
        new MessageComponents(
            request.method(),
            request.target(),
            request.authority(),
            request.type(),
            request.digest(),
            headers);

    final var candidates = candidates(peer);
    var verified = false;
    for (final var candidate : candidates) {
      try {
        verifier.verify(components, value, request.input(), KeyManager.parse(candidate));
        verified = true;
        break;
      } catch (final Exception ignored) {
      }
    }
    if (!verified) {
      throw new AuthenticationFailedException("Invalid peer signature");
    }

    final var builder =
        QuarkusSecurityIdentity.builder()
            .setPrincipal(new QuarkusPrincipal(peer.identity()))
            .addRole("peer");

    if (request.token() != null) {
      final var requester = TokenParser.parse(request.token());
      if (requester.isPresent()) {
        builder.addAttribute("requester", requester.get());
        builder.addAttribute("requester.subject", requester.get().subject());
      }
    }

    return builder.build();
  }
}
