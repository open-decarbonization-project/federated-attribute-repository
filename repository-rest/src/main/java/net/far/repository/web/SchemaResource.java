package net.far.repository.web;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import net.far.repository.core.Repository;
import net.far.repository.model.policy.FieldPolicy;
import net.far.repository.model.schema.Definition;
import net.far.repository.model.schema.Revision;

/**
 * REST resource for schema lifecycle management. Schemas define the expected attribute structure
 * for certificates within a namespace. Supports versioned revisions and field-level access policies
 * (public, masked, credential).
 */
@Path("/v1/schemas")
@Authenticated
public class SchemaResource {

  @Inject Repository repository;

  @Inject SecurityIdentity identity;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(final Definition definition) {
    final var principal = identity.getPrincipal().getName();
    final var owned =
        new Definition(
            definition.namespace(),
            definition.name(),
            definition.description(),
            definition.fields(),
            principal);
    final var schema = repository.define(owned);
    return Response.status(Response.Status.CREATED).entity(schema).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response list(@QueryParam("namespace") final String namespace) {
    final var schemas =
        namespace != null && !namespace.isBlank()
            ? repository.schemas(namespace)
            : repository.schemas();
    return Response.ok(schemas).build();
  }

  @GET
  @Path("/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response find(@PathParam("id") final String id) {
    final var schema = repository.schema(id);
    return Response.ok(schema).build();
  }

  @PUT
  @Path("/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response update(@PathParam("id") final String id, final Revision revision) {
    enforce(id);
    final var schema = repository.revise(id, revision);
    return Response.ok(schema).build();
  }

  @GET
  @Path("/{id}/versions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response versions(@PathParam("id") final String id) {
    final var versions = repository.versions(id);
    return Response.ok(versions).build();
  }

  @GET
  @Path("/{id}/versions/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response version(
      @PathParam("id") final String id, @PathParam("version") final int version) {
    final var schema = repository.schema(id, version);
    return Response.ok(schema).build();
  }

  @GET
  @Path("/{id}/policies")
  @Produces(MediaType.APPLICATION_JSON)
  public Response policies(@PathParam("id") final String id) {
    final var policies = repository.policies(id);
    return Response.ok(policies).build();
  }

  @PUT
  @Path("/{id}/policies")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response policies(@PathParam("id") final String id, final List<FieldPolicy> policies) {
    enforce(id);
    repository.policies(id, policies);
    return Response.ok(repository.policies(id)).build();
  }

  private void enforce(final String id) {
    if (identity.hasRole("repository-admin")) {
      return;
    }
    final var schema = repository.schema(id);
    final var principal = identity.getPrincipal().getName();
    if (!principal.equals(schema.owner())) {
      throw new ForbiddenException("Not the owner of this schema");
    }
  }
}
