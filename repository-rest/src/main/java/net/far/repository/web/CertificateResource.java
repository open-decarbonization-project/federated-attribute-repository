package net.far.repository.web;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import net.far.repository.web.util.Filters;
import net.far.repository.core.Binder;
import net.far.repository.core.Filter;
import net.far.repository.core.Repository;
import net.far.repository.model.Amendment;
import net.far.repository.model.Certificate;
import net.far.repository.model.Page;
import net.far.repository.model.Submission;
import net.far.repository.model.policy.AccessPolicy;
import net.far.resolver.model.Urn;
import net.far.resolver.model.query.Query;

/**
 * REST resource for certificate CRUD operations. Supports creation with optional schema validation,
 * OData-style search with {@code $filter/$top/$skip/$orderby}, attribute amendment, status
 * transitions, and retirement. Field-level policies are applied to responses based on the caller's
 * roles.
 *
 * <p>Certificate ownership is set from the authenticated principal on creation and enforced on
 * mutation (PUT/DELETE require owner or repository-admin).
 */
@Path("/v1/certificates")
@Authenticated
public class CertificateResource {

  @Inject Repository repository;

  @Inject Binder binder;

  @Inject SecurityIdentity identity;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(final Submission submission) {
    final var principal = identity.getPrincipal().getName();
    final var owned =
        new Submission(
            submission.namespace(),
            submission.identifier(),
            submission.attributes(),
            principal,
            submission.schema(),
            submission.pin());
    final var certificate = repository.create(owned);
    return Response.status(Response.Status.CREATED).entity(certificate).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response search(
      @QueryParam("$filter") final String filter,
      @QueryParam("$top") @DefaultValue("25") final int top,
      @QueryParam("$skip") @DefaultValue("0") final int skip,
      @QueryParam("$orderby") final String orderby) {
    net.far.resolver.model.query.Filter parsed = null;
    if (filter != null && !filter.isBlank()) {
      parsed = Filters.parse(filter);
    }
    final var query = new Query(Set.of(), parsed, top, skip, orderby);
    final var page = repository.search(query);
    return Response.ok(filter(page)).build();
  }

  @GET
  @Path("/{urn: urn:far:.+}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response find(@PathParam("urn") final String raw) {
    final var urn = Urn.parse(raw);
    final var certificate = repository.find(urn);
    return Response.ok(filter(certificate)).build();
  }

  @PUT
  @Path("/{urn: urn:far:.+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response update(@PathParam("urn") final String raw, final Amendment amendment) {
    final var urn = Urn.parse(raw);
    enforce(urn);
    final var certificate = repository.update(urn, amendment);
    return Response.ok(certificate).build();
  }

  @HEAD
  @Path("/{urn: urn:far:.+}")
  public Response exists(@PathParam("urn") final String raw) {
    final var urn = Urn.parse(raw);
    final var found = repository.exists(urn);
    return found ? Response.ok().build() : Response.status(Response.Status.NOT_FOUND).build();
  }

  @DELETE
  @Path("/{urn: urn:far:.+}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response retire(@PathParam("urn") final String raw) {
    final var urn = Urn.parse(raw);
    enforce(urn);
    final var certificate = repository.retire(urn);
    return Response.ok(certificate).build();
  }

  @GET
  @Path("/{urn: urn:far:.+}/documents")
  @Produces(MediaType.APPLICATION_JSON)
  public Response documents(@PathParam("urn") final String raw) {
    final var urn = Urn.parse(raw);
    enforce(urn);
    final var documents = binder.documents(urn);
    return Response.ok(documents).build();
  }

  @POST
  @Path("/{urn: urn:far:.+}/documents/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response attach(@PathParam("urn") final String raw, @PathParam("id") final String id) {
    final var urn = Urn.parse(raw);
    enforce(urn);
    binder.attach(urn, id);
    return Response.ok().build();
  }

  @DELETE
  @Path("/{urn: urn:far:.+}/documents/{id}")
  public Response detach(@PathParam("urn") final String raw, @PathParam("id") final String id) {
    final var urn = Urn.parse(raw);
    enforce(urn);
    binder.detach(urn, id);
    return Response.noContent().build();
  }

  private Certificate filter(final Certificate certificate) {
    if (certificate.schema() == null) {
      return certificate;
    }
    final var policies = repository.policies(certificate.schema());
    final var map = new HashMap<String, AccessPolicy>();
    for (final var fp : policies) {
      map.put(fp.field(), fp.policy());
    }
    final var roles = identity.getRoles();
    final var filtered = Filter.apply(certificate.attributes(), map, roles);
    return new Certificate(
        certificate.urn(),
        certificate.namespace(),
        certificate.identifier(),
        filtered,
        certificate.status(),
        certificate.integrity(),
        certificate.owner(),
        certificate.schema(),
        certificate.pin(),
        certificate.version(),
        certificate.created(),
        certificate.modified());
  }

  private Page filter(final Page page) {
    final var filtered = new ArrayList<Certificate>();
    for (final var certificate : page.value()) {
      filtered.add(filter(certificate));
    }
    return new Page(filtered, page.count(), page.skip(), page.top());
  }

  @POST
  @Path("/{urn: urn:far:.+}/entries")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("repository-admin")
  public Response publish(@PathParam("urn") final String raw) {
    final var urn = Urn.parse(raw);
    final var entries = repository.publish(urn);
    return Response.status(Response.Status.CREATED).entity(entries).build();
  }

  @GET
  @Path("/{urn: urn:far:.+}/entries")
  @Produces(MediaType.APPLICATION_JSON)
  public Response entries(@PathParam("urn") final String raw) {
    final var urn = Urn.parse(raw);
    final var entries = repository.entries(urn);
    return Response.ok(entries).build();
  }

  private void enforce(final Urn urn) {
    if (identity.hasRole("repository-admin")) {
      return;
    }
    final var certificate = repository.find(urn);
    final var principal = identity.getPrincipal().getName();
    if (!principal.equals(certificate.owner())) {
      throw new ForbiddenException("Not the owner of this certificate");
    }
  }
}
