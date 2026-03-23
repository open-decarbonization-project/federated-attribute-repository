package net.far.repository.web.util;

import net.far.repository.web.ErrorResponse;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UnsupportedOperationMapper implements ExceptionMapper<UnsupportedOperationException> {

  @Override
  public Response toResponse(final UnsupportedOperationException exception) {
    final var body = new ErrorResponse("unsupported_operation", exception.getMessage());
    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(body)
        .build();
  }
}
