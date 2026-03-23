package net.far.repository.web;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.far.repository.spi.Store;
import net.far.resolver.spi.DriverRegistry;

/**
 * REST resource exposing the repository's namespace catalogue. Returns both locally owned
 * namespaces (backed by the repository driver) and delegated namespaces (discovered from connected
 * peers).
 */
@Path("/v1/namespaces")
@PermitAll
public class NamespaceResource {

  @Inject DriverRegistry drivers;

  @Inject Store store;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response list(@QueryParam("type") @DefaultValue("all") final String type) {
    final var result = new ArrayList<Map<String, Object>>();
    if (!"delegated".equals(type)) {
      for (final var namespace : drivers.supported()) {
        final var entry = new LinkedHashMap<String, Object>();
        entry.put("name", namespace);
        entry.put("namespace", namespace);
        entry.put("driver", "repository");
        entry.put("type", "local");
        entry.put("status", "active");
        result.add(entry);
      }
    }
    if (!"local".equals(type)) {
      for (final var peer : store.peers()) {
        for (final var namespace : peer.namespaces()) {
          if (!drivers.supported().contains(namespace)) {
            final var entry = new LinkedHashMap<String, Object>();
            entry.put("name", namespace);
            entry.put("namespace", namespace);
            entry.put("driver", "delegated");
            entry.put("type", "delegated");
            entry.put("status", "active");
            entry.put("peer", peer.identity());
            result.add(entry);
          }
        }
      }
    }
    return Response.ok(Map.of("namespaces", result)).build();
  }

  @GET
  @Path("/{namespace}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response detail(@PathParam("namespace") final String namespace) {
    final var local = drivers.supported().contains(namespace);
    String owner = null;
    if (!local) {
      for (final var peer : store.peers()) {
        if (peer.namespaces().contains(namespace)) {
          owner = peer.identity();
          break;
        }
      }
      if (owner == null) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(
                new ErrorResponse(
                    "namespace_not_found", "Namespace '" + namespace + "' is not registered"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }
    }
    final var schemas = local ? store.schemas(namespace) : List.of();
    final var result = new LinkedHashMap<String, Object>();
    result.put("namespace", namespace);
    result.put("type", local ? "local" : "delegated");
    result.put("status", "active");
    result.put("local", local);
    result.put("schemas", schemas);
    result.put("supported_attributes", List.of());
    result.put("capabilities", Map.of());
    result.put("identifier_format", null);
    result.put("example_urns", List.of());
    if (owner != null) {
      result.put("peer", owner);
    }
    return Response.ok(result).build();
  }
}
