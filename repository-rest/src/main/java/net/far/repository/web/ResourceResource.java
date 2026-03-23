package net.far.repository.web;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.far.repository.web.util.Filters;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.far.repository.core.Binder;
import net.far.repository.core.Filter;
import net.far.repository.model.policy.AccessPolicy;
import net.far.repository.spi.Store;
import net.far.repository.web.util.RepositorySignatureFilter;
import net.far.resolver.core.PeerRegistry;
import net.far.resolver.core.Resolver;
import net.far.resolver.core.Router;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.IdentifierNotFoundException;
import net.far.resolver.model.NamespaceNotFoundException;
import net.far.resolver.model.Resolution;
import net.far.resolver.model.Urn;
import net.far.resolver.model.query.Page;
import net.far.resolver.model.query.Query;
import net.far.resolver.spi.DriverRegistry;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Federated resolution and search endpoint. Queries are routed to local drivers for owned
 * namespaces and delegated to peers for remote namespaces. Responses are signed per RFC 9421 by
 * {@link RepositorySignatureFilter}.
 *
 * <p>Supports {@code resolve=exists} (lightweight existence check), {@code resolve=summary}
 * (reduced body without attributes), content negotiation for renditions, and attribute selection
 * via {@code ?attribute=}.
 */
@Path("/v1/resources")
public class ResourceResource {

  private static final Logger LOGGER = Logger.getLogger(ResourceResource.class.getName());
  private static final String PREFIX = "Bearer ";
  private static final Set<String> RENDITION_TYPES =
      Set.of(MediaType.TEXT_HTML, "application/pdf", "image/png");

  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  @Inject Resolver resolver;

  @Inject Router router;

  @Inject PeerRegistry peers;

  @Inject Store store;

  @Inject Binder binder;

  @Inject SecurityIdentity identity;

  @Inject DriverRegistry drivers;

  @ConfigProperty(name = "repository.identity", defaultValue = "https://repository.far.example.com")
  String node;

  static String media(final String accept) {
    if (accept == null || accept.isBlank()) {
      return null;
    }
    for (final var part : accept.split(",")) {
      final var type = part.split(";")[0].trim();
      if (RENDITION_TYPES.contains(type)) {
        return type;
      }
      if (MediaType.APPLICATION_JSON.equals(type) || "*/*".equals(type)) {
        return null;
      }
    }
    return null;
  }

  private static Page deduplicate(final Page page) {
    final var seen = new LinkedHashSet<String>();
    final var unique = new ArrayList<Resolution>();
    for (final var resolution : page.value()) {
      if (seen.add(resolution.urn().toString())) {
        unique.add(resolution);
      }
    }
    if (unique.size() == page.value().size()) {
      return page;
    }
    final var ratio = (double) unique.size() / page.value().size();
    final var estimated = Math.max(unique.size(), (long) (page.count() * ratio));
    return new Page(unique, estimated, page.skip(), page.top());
  }

  private static String extract(final String authorization) {
    if (authorization != null && authorization.startsWith(PREFIX)) {
      return authorization.substring(PREFIX.length()).trim();
    }
    return null;
  }

  private static List<String> delegation(final String chain) {
    if (chain == null || chain.isBlank()) {
      return List.of();
    }
    final var entries = List.of(chain.split(",\\s*"));
    for (final var entry : entries) {
      if (entry.isBlank() || entry.contains("\r") || entry.contains("\n") || entry.length() > 256) {
        throw new BadRequestException("Malformed delegation chain");
      }
    }
    return entries;
  }

  private static Resolution select(final Resolution resolution, final String attributes) {
    final var names = Set.of(attributes.split(","));
    final var filtered = new LinkedHashMap<String, Attribute>();
    for (final var entry : resolution.attributes().entrySet()) {
      if (names.contains(entry.getKey())) {
        filtered.put(entry.getKey(), entry.getValue());
      }
    }
    return new Resolution(
        resolution.urn(),
        resolution.namespace(),
        resolution.identifier(),
        filtered,
        resolution.integrity(),
        resolution.resolver(),
        resolution.status(),
        resolution.timestamp(),
        resolution.delegated());
  }

  private static String sanitize(final String disposition) {
    if (disposition == null) {
      return "attachment";
    }
    final var clean = disposition.replaceAll("[\\r\\n\"]", "");
    if (!clean.startsWith("attachment")) {
      return "attachment";
    }
    return clean;
  }

  @GET
  @PermitAll
  @Produces(MediaType.APPLICATION_JSON)
  public Response query(
      @QueryParam("$filter") final String filter,
      @QueryParam("$top") @DefaultValue("25") final int top,
      @QueryParam("$skip") @DefaultValue("0") final int skip,
      @QueryParam("$orderby") final String orderby,
      @QueryParam("local") @DefaultValue("false") final boolean local,
      @HeaderParam("Far-Delegation-Chain") final String chain,
      @HeaderParam("Authorization") final String authorization) {
    final var delegates = delegation(chain);
    final var token = extract(authorization);
    final var delegated = !delegates.isEmpty();
    final var restricted = local || delegated;
    if (filter == null || filter.isBlank()) {
      if (!delegated) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse("invalid_filter", "$filter parameter is required"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }
      final var scope = router.namespaces();
      final var query = new Query(scope, null, top, skip, orderby);
      final var page = resolver.query(query, delegates, token);
      return Response.ok(filter(stamp(deduplicate(page)), delegated)).build();
    }
    final var parsed = Filters.parse(filter);
    var namespaces = Filters.namespaces(parsed);
    final var stripped = Filters.strip(parsed);
    if (namespaces.isEmpty()) {
      namespaces = restricted ? router.namespaces() : namespaces();
    }
    final var query = new Query(namespaces, stripped, top, skip, orderby);
    final var page = resolver.query(query, delegates, token);
    return Response.ok(filter(stamp(deduplicate(page)), delegated)).build();
  }

  @GET
  @PermitAll
  @Path("/{urn: urn:far:.+}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, "application/pdf", "image/png"})
  public Response resolve(
      @PathParam("urn") final String raw,
      @HeaderParam("Far-Delegation-Chain") final String chain,
      @HeaderParam("Authorization") final String authorization,
      @HeaderParam("Accept") final String accept,
      @QueryParam("attribute") final String attribute,
      @QueryParam("delegate") @DefaultValue("allow") final String delegate,
      @QueryParam("resolve") final String mode) {
    final var urn = Urn.parse(raw);
    if ("exists".equals(mode)) {
      final var delegates = delegation(chain);
      final var token = extract(authorization);
      try {
        resolver.resolve(urn, delegates, token);
        final var body = new LinkedHashMap<String, Object>();
        body.put("urn", urn.toString());
        body.put("namespace", urn.namespace());
        body.put("identifier", urn.identifier());
        body.put("exists", true);
        body.put("resolver", node);
        return Response.ok(body)
            .type(MediaType.APPLICATION_JSON)
            .header("Far-Resolver", node)
            .header("Far-Namespace", urn.namespace())
            .build();
      } catch (final IdentifierNotFoundException ignored) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new ErrorResponse("identifier_not_found", "Not found: " + urn))
            .type(MediaType.APPLICATION_JSON)
            .build();
      } catch (final NamespaceNotFoundException ignored) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new ErrorResponse("namespace_not_found", "Namespace not found: " + urn.namespace()))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }
    }
    if ("summary".equals(mode)) {
      final var delegates = delegation(chain);
      final var token = extract(authorization);
      final var delegated = !delegates.isEmpty();
      final var resolution = filter(resolver.resolve(urn, delegates, token), delegated);
      final var body = new LinkedHashMap<String, Object>();
      body.put("urn", resolution.urn().toString());
      body.put("namespace", resolution.namespace());
      body.put("identifier", resolution.identifier());
      body.put("status", resolution.status());
      if (resolution.integrity() != null) {
        body.put(
            "integrity",
            Map.of(
                "digest", resolution.integrity().digest(),
                "algorithm", resolution.integrity().algorithm()));
      }
      body.put("resolver", resolution.resolver() != null ? resolution.resolver() : node);
      body.put("timestamp", resolution.timestamp().toString());
      return Response.ok(body)
          .type(MediaType.APPLICATION_JSON)
          .header("Far-Resolver", node)
          .header("Far-Namespace", resolution.namespace())
          .build();
    }
    final var media = media(accept);
    if (media != null) {
      final var delegates = delegation(chain);
      final var token = extract(authorization);
      final var rendition = resolver.rendition(urn, media, delegates, token);
      return Response.ok(rendition.content(), rendition.media())
          .header("Far-Resolver", node)
          .header("Far-Namespace", urn.namespace())
          .build();
    }
    if ("deny".equals(delegate) && !drivers.supported().contains(urn.namespace())) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(
              new ErrorResponse(
                  "namespace_not_found",
                  "Namespace not local and delegation denied: " + urn.namespace()))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    final var delegates = delegation(chain);
    final var token = extract(authorization);
    final var delegated = !delegates.isEmpty();
    var resolution = filter(resolver.resolve(urn, delegates, token), delegated);
    if (attribute != null && !attribute.isBlank()) {
      resolution = select(resolution, attribute);
    }
    final var builder = Response.ok(resolution);
    builder.header("Far-Resolver", node);
    builder.header("Far-Namespace", resolution.namespace());
    return builder.build();
  }

  @HEAD
  @PermitAll
  @Path("/{urn: urn:far:.+}")
  public Response exists(
      @PathParam("urn") final String raw,
      @HeaderParam("Far-Delegation-Chain") final String chain,
      @HeaderParam("Authorization") final String authorization) {
    final var urn = Urn.parse(raw);
    final var delegates = delegation(chain);
    if (resolver.exists(urn, delegates)) {
      return Response.ok()
          .header("Far-Resolver", node)
          .header("Far-Namespace", urn.namespace())
          .build();
    }
    final var token = extract(authorization);
    resolver.resolve(urn, delegates, token);
    return Response.ok()
        .header("Far-Resolver", node)
        .header("Far-Namespace", urn.namespace())
        .build();
  }

  @GET
  @PermitAll
  @Path("/{urn: urn:far:.+}/history")
  @Produces(MediaType.APPLICATION_JSON)
  public Response history(
      @PathParam("urn") final String raw,
      @QueryParam("type") final String type,
      @QueryParam("limit") @DefaultValue("100") final int limit,
      @QueryParam("offset") @DefaultValue("0") final int offset,
      @QueryParam("from") final String from,
      @QueryParam("to") final String to,
      @HeaderParam("Far-Delegation-Chain") final String chain,
      @HeaderParam("Authorization") final String authorization) {
    final var urn = Urn.parse(raw);
    final var delegates = delegation(chain);
    final var token = extract(authorization);
    final var history = resolver.history(urn, delegates, token);
    var filtered = history.events();
    if (type != null && !type.isBlank()) {
      final var target = net.far.resolver.model.Event.EventType.parse(type);
      filtered = filtered.stream().filter(e -> e.type() == target).toList();
    }
    if (from != null && !from.isBlank()) {
      final var start = java.time.Instant.parse(from);
      filtered = filtered.stream().filter(e -> !e.timestamp().isBefore(start)).toList();
    }
    if (to != null && !to.isBlank()) {
      final var end = java.time.Instant.parse(to);
      filtered = filtered.stream().filter(e -> !e.timestamp().isAfter(end)).toList();
    }
    final var total = filtered.size();
    final var start = Math.min(offset, total);
    final var end = Math.min(start + limit, total);
    final var page = filtered.subList(start, end);
    final var result = new java.util.LinkedHashMap<String, Object>();
    result.put("urn", urn.toString());
    result.put("namespace", urn.namespace());
    result.put("identifier", urn.identifier());
    result.put("history", page);
    result.put("total", total);
    result.put("limit", limit);
    result.put("offset", offset);
    return Response.ok(result)
        .header("Far-Resolver", node)
        .header("Far-Namespace", urn.namespace())
        .build();
  }

  @GET
  @Authenticated
  @Path("/{urn: urn:far:.+}/documents")
  @Produces(MediaType.APPLICATION_JSON)
  public Response documents(
      @PathParam("urn") final String raw,
      @HeaderParam("Authorization") final String authorization) {
    final var urn = Urn.parse(raw);
    if (router.route(urn.namespace()).isPresent()) {
      enforce(urn);
      return Response.ok(binder.documents(urn)).build();
    }
    final var endpoint = endpoint(urn.namespace());
    if (endpoint.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    try {
      final var uri = URI.create(endpoint.get() + "/resources/" + urn + "/documents");
      final var builder =
          HttpRequest.newBuilder(uri)
              .header("Accept", "application/json")
              .timeout(Duration.ofSeconds(30));
      final var token = extract(authorization);
      if (token != null) {
        builder.header("Authorization", "Bearer " + token);
      }
      final var response = HTTP.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        return Response.ok(response.body()).type(MediaType.APPLICATION_JSON).build();
      }
      return Response.status(response.statusCode()).build();
    } catch (final Exception exception) {
      return Response.status(Response.Status.BAD_GATEWAY)
          .entity(new ErrorResponse("delegation_failed", "Remote repository unreachable"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }

  @GET
  @Authenticated
  @Path("/{urn: urn:far:.+}/documents/{id}")
  public Response document(
      @PathParam("urn") final String raw,
      @PathParam("id") final String id,
      @HeaderParam("Authorization") final String authorization) {
    final var urn = Urn.parse(raw);
    if (router.route(urn.namespace()).isPresent()) {
      enforce(urn);
      final var attached =
          binder.documents(urn).stream().anyMatch(document -> document.id().equals(id));
      if (!attached) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      final var document = binder.document(id);
      final var content = binder.content(id);
      if (document.isEmpty() || content.isEmpty()) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      return Response.ok(content.get())
          .type(document.get().media())
          .header(
              "Content-Disposition",
              "attachment; filename=\"" + sanitize(document.get().filename()) + "\"")
          .header("X-Content-Type-Options", "nosniff")
          .build();
    }
    final var endpoint = endpoint(urn.namespace());
    if (endpoint.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    try {
      final var uri = URI.create(endpoint.get() + "/resources/" + urn + "/documents/" + id);
      final var builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30));
      final var token = extract(authorization);
      if (token != null) {
        builder.header("Authorization", "Bearer " + token);
      }
      final var response =
          HTTP.send(builder.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() == 200) {
        final var type =
            response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        if (!Binder.allowed(type)) {
          return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
        }
        final var disposition =
            sanitize(response.headers().firstValue("Content-Disposition").orElse("attachment"));
        final var result =
            Response.ok(response.body())
                .type(type)
                .header("Content-Disposition", disposition)
                .header("X-Content-Type-Options", "nosniff");
        return result.build();
      }
      return Response.status(response.statusCode()).build();
    } catch (final Exception exception) {
      return Response.status(Response.Status.BAD_GATEWAY).build();
    }
  }

  private void enforce(final Urn urn) {
    final var certificate =
        store.find(urn).orElseThrow(() -> new NotFoundException("Certificate not found: " + urn));
    if (identity.hasRole("repository-admin")) {
      return;
    }
    if (identity.hasRole("peer")) {
      final var requester = (String) identity.getAttribute("requester.subject");
      if (requester == null) {
        throw new ForbiddenException("Peer request missing requester identity");
      }
      if (!requester.equals(certificate.owner())) {
        throw new ForbiddenException("Requester is not the owner of this certificate");
      }
      return;
    }
    final var principal = identity.getPrincipal().getName();
    if (!principal.equals(certificate.owner())) {
      throw new ForbiddenException("Not the owner of this certificate");
    }
  }

  private Resolution filter(final Resolution resolution, final boolean delegated) {
    try {
      final var certificate = store.find(resolution.urn()).orElse(null);
      if (certificate == null) {
        final var local = router.route(resolution.namespace()).isPresent();
        if (local) {
          return new Resolution(
              resolution.urn(),
              resolution.namespace(),
              resolution.identifier(),
              Map.of(),
              resolution.integrity(),
              resolution.resolver(),
              resolution.status(),
              resolution.timestamp());
        }
        return resolution;
      }
      if (certificate.schema() == null) {
        return resolution;
      }
      final var policies = store.policies(certificate.schema());
      final var map = new HashMap<String, AccessPolicy>();
      for (final var fp : policies) {
        map.put(fp.field(), fp.policy());
      }
      final var roles =
          identity.isAnonymous() ? new HashSet<String>() : new HashSet<>(identity.getRoles());
      if (delegated) {
        roles.remove("repository-admin");
      }
      final var filtered = Filter.apply(resolution.attributes(), map, roles);
      return new Resolution(
          resolution.urn(),
          resolution.namespace(),
          resolution.identifier(),
          filtered,
          resolution.integrity(),
          resolution.resolver(),
          resolution.status(),
          resolution.timestamp());
    } catch (final Exception exception) {
      LOGGER.log(
          Level.SEVERE,
          "Failed to apply policies for " + resolution.urn() + ", redacting attributes",
          exception);
      return new Resolution(
          resolution.urn(),
          resolution.namespace(),
          resolution.identifier(),
          Map.of(),
          resolution.integrity(),
          resolution.resolver(),
          resolution.status(),
          resolution.timestamp());
    }
  }

  private Page filter(final Page page, final boolean delegated) {
    final var results = new ArrayList<Resolution>();
    for (final var resolution : page.value()) {
      results.add(filter(resolution, delegated));
    }
    return new Page(results, page.count(), page.skip(), page.top());
  }

  private Optional<String> endpoint(final String namespace) {
    final var candidates = peers.find(namespace);
    if (!candidates.isEmpty()) {
      return Optional.of(candidates.getFirst().base());
    }
    return Optional.empty();
  }

  private Set<String> namespaces() {
    final var all = new HashSet<>(router.namespaces());
    all.addAll(peers.namespaces());
    return all;
  }

  private Page stamp(final Page page) {
    final var local = router.namespaces();
    final var index = new HashMap<String, String>();
    for (final var peer : peers.all()) {
      for (final var namespace : peer.namespaces()) {
        index.putIfAbsent(namespace, peer.identity());
      }
    }
    final var stamped = new ArrayList<Resolution>();
    for (final var resolution : page.value()) {
      final var namespace = resolution.namespace();
      if (local.contains(namespace)) {
        stamped.add(resolution);
      } else if (resolution.resolver() != null
          && !resolution.resolver().isBlank()
          && !"repository".equals(resolution.resolver())) {
        stamped.add(resolution);
      } else if (index.containsKey(namespace)) {
        stamped.add(
            new Resolution(
                resolution.urn(),
                namespace,
                resolution.identifier(),
                resolution.attributes(),
                resolution.integrity(),
                index.get(namespace),
                resolution.status(),
                resolution.timestamp()));
      } else {
        stamped.add(resolution);
      }
    }
    return new Page(stamped, page.count(), page.skip(), page.top());
  }
}
