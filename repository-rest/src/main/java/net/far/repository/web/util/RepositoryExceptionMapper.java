package net.far.repository.web.util;

import net.far.repository.web.ErrorResponse;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.far.repository.model.*;
import net.far.repository.model.schema.*;

@Provider
public class RepositoryExceptionMapper implements ExceptionMapper<RepositoryException> {

  @Override
  public Response toResponse(final RepositoryException exception) {
    final var status =
        switch (exception) {
          case InvalidSubmissionException e -> Response.Status.BAD_REQUEST;
          case CertificateNotFoundException e -> Response.Status.NOT_FOUND;
          case DocumentNotFoundException e -> Response.Status.NOT_FOUND;
          case DuplicateCertificateException e -> Response.Status.CONFLICT;
          case SchemaNotFoundException e -> Response.Status.NOT_FOUND;
          case InvalidSchemaException e -> Response.Status.BAD_REQUEST;
          case LedgerException e -> Response.Status.INTERNAL_SERVER_ERROR;
          default -> Response.Status.INTERNAL_SERVER_ERROR;
        };

    final var body = new ErrorResponse(exception.code(), exception.getMessage());

    return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
  }
}
