package net.far.repository.web.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.far.resolver.signature.DigestCalculator;
import net.far.resolver.signature.MessageComponents;
import net.far.resolver.signature.Signer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * JAX-RS response filter that signs outgoing {@code /v1/resources} responses per RFC 9421 using
 * Ed25519. Computes a SHA-256 content digest over the serialized JSON body, then signs the status
 * code, content-type, digest, and custom FAR headers. The filter explicitly sets {@code
 * Content-Type} to {@code application/json;charset=UTF-8} to guarantee the signed value matches the
 * wire header.
 */
@Provider
public class RepositorySignatureFilter implements ContainerResponseFilter {

  private static final Logger LOGGER = Logger.getLogger(RepositorySignatureFilter.class.getName());

  private static final Set<String> SKIP = Set.of("/v1/health", "/v1/peers/configuration", "/q/");

  @Inject ObjectMapper mapper;

  @Inject Signer signer;

  @ConfigProperty(name = "repository.identity", defaultValue = "https://repository.far.example.com")
  String identity;

  @Override
  public void filter(
      final ContainerRequestContext request, final ContainerResponseContext response) {
    final var path = request.getUriInfo().getPath();
    if (SKIP.stream().anyMatch(path::startsWith)) {
      return;
    }

    if (!path.startsWith("/v1/resources")) {
      return;
    }

    if (!response.hasEntity()) {
      return;
    }

    if (response.getEntity() instanceof byte[]) {
      return;
    }

    try {
      final var bytes = mapper.writeValueAsBytes(response.getEntity());
      final var digest = DigestCalculator.compute(bytes);

      final var headers = new LinkedHashMap<String, String>();
      final var resolver = response.getHeaderString("Far-Resolver");
      if (resolver != null) {
        headers.put("far-resolver", resolver);
      }
      final var namespace = response.getHeaderString("Far-Namespace");
      if (namespace != null) {
        headers.put("far-namespace", namespace);
      }

      final var type = "application/json;charset=UTF-8";

      final var components =
          new MessageComponents(response.getStatus(), null, null, null, type, digest, headers);
      final var signature = signer.sign(components);

      response.getHeaders().putSingle("Content-Type", type);
      response.getHeaders().putSingle("Content-Digest", digest);
      response.getHeaders().putSingle("Signature", "sig1=:" + signature.value() + ":");
      response.getHeaders().putSingle("Signature-Input", signature.input());
    } catch (final Exception exception) {
      LOGGER.log(Level.WARNING, "Failed to sign response for " + path, exception);
    }
  }
}
