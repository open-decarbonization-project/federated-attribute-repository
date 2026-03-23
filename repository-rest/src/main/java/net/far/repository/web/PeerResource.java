package net.far.repository.web;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.far.repository.spi.Store;
import net.far.resolver.core.PeerRegistry;
import net.far.resolver.model.Peer;
import net.far.resolver.signature.KeyRing;
import net.far.resolver.spi.DriverRegistry;
import net.far.resolver.spi.FarClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * REST resource for federated peer management. Exposes this repository's configuration (identity,
 * namespaces, public key) at {@code /v1/peers/configuration} and allows admins to add or remove
 * peers. Adding a peer fetches the remote configuration automatically and validates namespace
 * conflicts.
 */
@Path("/v1/peers")
public class PeerResource {

  private static final Logger LOGGER = Logger.getLogger(PeerResource.class.getName());

  @Inject Store store;

  @Inject PeerRegistry peers;

  @Inject FarClient client;

  @Inject KeyRing ring;

  @Inject DriverRegistry drivers;

  @ConfigProperty(name = "repository.identity", defaultValue = "https://repository.far.example.com")
  String identity;

  @ConfigProperty(name = "repository.namespaces", defaultValue = "default")
  String namespaces;

  @ConfigProperty(name = "repository.protocol.version", defaultValue = "0.1.0")
  String version;

  @ConfigProperty(name = "repository.ssrf.check", defaultValue = "true")
  boolean check;

  @ConfigProperty(name = "repository.delegation.depth", defaultValue = "5")
  int depth;

  private static void validate(final String endpoint) {
    final var uri = URI.create(endpoint);
    if (!"https".equals(uri.getScheme())) {
      throw new BadRequestException("Endpoint must use HTTPS");
    }
    try {
      final var address = InetAddress.getByName(uri.getHost());
      if (address.isLoopbackAddress()
          || address.isSiteLocalAddress()
          || address.isLinkLocalAddress()) {
        throw new BadRequestException("Endpoint must not target private addresses");
      }
    } catch (final UnknownHostException ignored) {
      throw new BadRequestException("Cannot resolve endpoint host");
    }
  }

  static boolean trusted(final String base, final String endpoint) {
    try {
      final var origin = URI.create(endpoint);
      final var target = URI.create(base);
      final var scheme = origin.getScheme() != null ? origin.getScheme() : "";
      return scheme.equalsIgnoreCase(target.getScheme())
          && origin.getHost() != null
          && origin.getHost().equalsIgnoreCase(target.getHost())
          && (origin.getPort() == target.getPort() || target.getPort() < 0 || origin.getPort() < 0);
    } catch (final Exception ignored) {
      return false;
    }
  }

  @GET
  @PermitAll
  @Produces(MediaType.APPLICATION_JSON)
  public Response list(@QueryParam("status") final String status) {
    final var all = List.copyOf(store.peers());
    if ("connected".equals(status)) {
      return Response.ok(Map.of("peers", all)).build();
    }
    if ("disconnected".equals(status)) {
      return Response.ok(Map.of("peers", List.of())).build();
    }
    return Response.ok(Map.of("peers", all)).build();
  }

  @GET
  @Path("/configuration")
  @PermitAll
  @Produces(MediaType.APPLICATION_JSON)
  public Response configuration() {
    final var result = new LinkedHashMap<String, Object>();
    result.put("identity", identity);
    result.put("protocol_version", "far/1.0");
    result.put("api_base", identity + "/v1");
    result.put("namespaces", List.copyOf(Set.of(namespaces.split(","))));
    result.put(
        "public_key",
        Map.of(
            "algorithm", "Ed25519",
            "key_id", ring.current().id(),
            "public_key_pem", ring.current().pem()));
    final var retired = new ArrayList<Map<String, Object>>();
    for (final var previous : ring.previous()) {
      retired.add(
          Map.of(
              "key_id", previous.keys().id(),
              "public_key_pem", previous.keys().pem(),
              "expires", previous.expires().toString()));
    }
    result.put("previous_keys", retired);
    result.put("delegation", Map.of("accepts_delegation", true, "max_depth", depth));
    result.put("version", version);
    result.put(
        "endpoints",
        Map.of(
            "resources", "/v1/resources",
            "namespaces", "/v1/namespaces",
            "peers", "/v1/peers",
            "health", "/v1/health"));
    return Response.ok(result).header("Cache-Control", "public, max-age=3600").build();
  }

  @POST
  @RolesAllowed("repository-admin")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response add(final Map<String, String> body) {
    final var endpoint = body.get("endpoint");
    if (endpoint == null || endpoint.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse("invalid_request", "endpoint is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    if (check) {
      validate(endpoint);
    }
    final var config = client.configuration(endpoint);
    if (config.isEmpty()) {
      return Response.status(Response.Status.BAD_GATEWAY)
          .entity(new ErrorResponse("peer_unreachable", "Could not reach peer at " + endpoint))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    final var remote = config.get();
    final var remoteIdentity = (String) remote.get("identity");
    final var remoteNamespaces =
        remote.get("namespaces") instanceof List<?> list
            ? list.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet())
            : Set.<String>of();
    final var key =
        remote.get("public_key") instanceof Map<?, ?> pk ? (String) pk.get("public_key_pem") : null;
    final var remoteKeyId =
        remote.get("public_key") instanceof Map<?, ?> pk2 ? (String) pk2.get("key_id") : null;
    final var previous = new ArrayList<Peer.PeerKey>();
    if (remote.get("previous_keys") instanceof List<?> keys) {
      for (final var entry : keys) {
        if (entry instanceof Map<?, ?> pk) {
          final var id = (String) pk.get("key_id");
          final var pem = (String) pk.get("public_key_pem");
          final var expires =
              pk.get("expires") != null ? Instant.parse((String) pk.get("expires")) : null;
          previous.add(new Peer.PeerKey(id, pem, expires));
        }
      }
    }
    if (remoteIdentity == null || remoteIdentity.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse("invalid_peer", "Peer did not declare an identity"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    if (remoteIdentity.equals(identity)) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new ErrorResponse("self_reference", "Cannot add self as peer"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    final var owned = drivers.supported();
    for (final var ns : remoteNamespaces) {
      if (owned.contains(ns)) {
        return Response.status(Response.Status.CONFLICT)
            .entity(
                new ErrorResponse(
                    "namespace_conflict", "Peer claims locally owned namespace: " + ns))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }
    }
    final var existing = store.peers();
    var priority = 1;
    for (final var ns : remoteNamespaces) {
      for (final var current : existing) {
        if (current.identity().equals(remoteIdentity)) continue;
        if (current.namespaces().contains(ns)) {
          LOGGER.warning(
              () ->
                  String.format(
                      "Namespace '%s' already claimed by peer %s; new peer %s will have lower"
                          + " priority",
                      ns, current.identity(), remoteIdentity));
        }
      }
    }
    for (final var current : existing) {
      if (current.priority() >= priority) {
        priority = current.priority() + 1;
      }
    }
    final var declared = remote.get("api_base") instanceof String ab ? ab : endpoint + "/v1";
    final var base = trusted(declared, endpoint) ? declared : endpoint + "/v1";
    final var remoteDepth =
        remote.get("delegation") instanceof Map<?, ?> delegation
                && delegation.get("max_depth") instanceof Number max
            ? max.intValue()
            : 5;
    final var peer =
        new Peer(
            remoteIdentity,
            endpoint,
            remoteNamespaces,
            key,
            remoteKeyId,
            previous,
            Instant.now(),
            priority,
            true,
            base,
            remoteDepth);
    store.save(peer);
    peers.register(peer);
    return Response.status(Response.Status.CREATED).entity(peer).build();
  }

  @DELETE
  @Path("/{identity}")
  @RolesAllowed("repository-admin")
  public Response remove(@PathParam("identity") final String identity) {
    store.remove(identity);
    peers.remove(identity);
    return Response.noContent().build();
  }
}
