package net.far.repository.web.peers;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;

/**
 * Quarkus HTTP authentication mechanism that extracts RFC 9421 signature headers from inbound
 * requests and delegates verification to {@link PeerSignatureProvider}. Runs at priority 100 so it
 * takes precedence over OIDC for signed peer requests.
 */
@ApplicationScoped
public class PeerSignatureMechanism implements HttpAuthenticationMechanism {

  private static final String PREFIX = "Bearer ";

  @Override
  public int getPriority() {
    return 100;
  }

  @Override
  public Uni<SecurityIdentity> authenticate(
      final RoutingContext context, final IdentityProviderManager manager) {
    final var input = context.request().getHeader("Signature-Input");
    final var signature = context.request().getHeader("Signature");
    if (input == null || signature == null) {
      return Uni.createFrom().nullItem();
    }
    final var method = context.request().method().name();
    final var target = context.request().absoluteURI();
    final var authority =
        context.request().authority() != null ? context.request().authority().toString() : null;
    final var type = context.request().getHeader("Content-Type");
    final var digest = context.request().getHeader("Content-Digest");
    final var authorization = context.request().getHeader("Authorization");
    final var token =
        authorization != null && authorization.startsWith(PREFIX)
            ? authorization.substring(PREFIX.length()).trim()
            : null;
    final var chain = context.request().getHeader("Far-Delegation-Chain");
    final var request =
        new PeerSignatureRequest(
            input, signature, method, target, authority, type, digest, token, chain);
    return manager.authenticate(request);
  }

  @Override
  public Uni<ChallengeData> getChallenge(final RoutingContext context) {
    return Uni.createFrom().nullItem();
  }

  @Override
  public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
    return Set.of(PeerSignatureRequest.class);
  }
}
