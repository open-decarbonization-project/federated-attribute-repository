package net.far.repository.web.util;

import net.far.repository.web.ErrorResponse;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.far.resolver.model.DelegationException;
import net.far.resolver.model.DelegationLoopException;
import net.far.resolver.model.FarException;
import net.far.resolver.model.IdentifierNotFoundException;
import net.far.resolver.model.InvalidUrnException;
import net.far.resolver.model.NamespaceNotFoundException;
import net.far.resolver.model.SignatureException;
import net.far.resolver.model.UnauthorizedException;
import net.far.resolver.model.UnsupportedFormatException;
import net.far.resolver.model.query.InvalidFilterException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class FarExceptionMapper implements ExceptionMapper<FarException> {

  @ConfigProperty(name = "repository.identity", defaultValue = "https://repository.far.example.com")
  String identity;

  @Override
  public Response toResponse(final FarException exception) {
    final var status =
        switch (exception) {
          case InvalidFilterException ignored -> Response.Status.BAD_REQUEST;
          case InvalidUrnException ignored -> Response.Status.BAD_REQUEST;
          case IdentifierNotFoundException ignored -> Response.Status.NOT_FOUND;
          case NamespaceNotFoundException ignored -> Response.Status.NOT_FOUND;
          case UnsupportedFormatException ignored -> Response.Status.NOT_ACCEPTABLE;
          case UnauthorizedException ignored -> Response.Status.FORBIDDEN;
          case SignatureException ignored -> Response.Status.UNAUTHORIZED;
          case DelegationLoopException ignored -> Response.Status.CONFLICT;
          case DelegationException ignored -> Response.Status.BAD_GATEWAY;
          default -> Response.Status.INTERNAL_SERVER_ERROR;
        };

    final var body = new ErrorResponse(exception.code(), exception.getMessage(), null, identity);

    return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
  }
}
