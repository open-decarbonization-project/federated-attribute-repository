package net.far.repository.web;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import net.far.repository.core.Binder;
import net.far.repository.model.DocumentNotFoundException;
import net.far.repository.spi.Store;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/**
 * REST resource for document upload, download, and metadata retrieval. Uploads are multipart form
 * data; each document is signed and digest-verified. Download and metadata access require ownership
 * of the attached certificate or the repository-admin role.
 */
@Path("/v1/documents")
@Authenticated
public class DocumentResource {

  @Inject Binder binder;

  @Inject Store store;

  @Inject SecurityIdentity identity;

  private static byte[] read(final FileUpload upload) {
    try {
      return Files.readAllBytes(upload.uploadedFile());
    } catch (final IOException exception) {
      throw new UncheckedIOException("Failed to read upload", exception);
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response upload(
      @RestForm final String filename,
      @RestForm final String media,
      @RestForm final FileUpload content) {
    final var bytes = read(content);
    final var principal = identity.getPrincipal().getName();
    final var document = binder.upload(filename, media, bytes, principal);
    return Response.status(Response.Status.CREATED).entity(document).build();
  }

  @GET
  @RolesAllowed("repository-admin")
  @Produces(MediaType.APPLICATION_JSON)
  public Response list() {
    final var documents = binder.documents();
    return Response.ok(documents).build();
  }

  @GET
  @Path("/{id}")
  public Response download(@PathParam("id") final String id) {
    authorize(id);
    final var document = binder.document(id).orElseThrow(() -> new DocumentNotFoundException(id));
    final var bytes = binder.content(id).orElseThrow(() -> new DocumentNotFoundException(id));
    return Response.ok(bytes)
        .type(document.media())
        .header("Content-Disposition", "attachment; filename=\"" + document.filename() + "\"")
        .header("X-Content-Type-Options", "nosniff")
        .build();
  }

  @GET
  @Path("/{id}/metadata")
  @Produces(MediaType.APPLICATION_JSON)
  public Response metadata(@PathParam("id") final String id) {
    authorize(id);
    final var document = binder.document(id).orElseThrow(() -> new DocumentNotFoundException(id));
    return Response.ok(document).build();
  }

  private void authorize(final String id) {
    if (identity.hasRole("repository-admin")) {
      return;
    }
    final var principal = identity.getPrincipal().getName();
    final var owner = store.owner(id);
    if (owner.isEmpty()) {
      final var document = binder.document(id);
      if (document.isPresent() && principal.equals(document.get().uploader())) {
        return;
      }
      throw new ForbiddenException("Document is not attached to any certificate");
    }
    if (!principal.equals(owner.get())) {
      throw new ForbiddenException("Not the owner of the attached certificate");
    }
  }
}
