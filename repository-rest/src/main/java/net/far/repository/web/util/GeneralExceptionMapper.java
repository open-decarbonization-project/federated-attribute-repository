package net.far.repository.web.util;

import net.far.repository.web.ErrorResponse;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.ConcurrentModificationException;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {

  @Override
  public Response toResponse(final Exception exception) {
    if (exception instanceof WebApplicationException web) {
      return web.getResponse();
    }
    if (exception instanceof IllegalArgumentException) {
      final var body = new ErrorResponse("bad_request", exception.getMessage());
      return Response.status(400).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
    if (exception instanceof ConcurrentModificationException) {
      final var body = new ErrorResponse("conflict", exception.getMessage());
      return Response.status(409).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
    if (exception instanceof UnsupportedOperationException) {
      final var body = new ErrorResponse("bad_request", exception.getMessage());
      return Response.status(400).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
    java.util.logging.Logger.getLogger(GeneralExceptionMapper.class.getName())
        .log(java.util.logging.Level.SEVERE, "Unhandled exception", exception);
    final var body = new ErrorResponse("internal_error", "Internal server error");
    return Response.status(500).type(MediaType.APPLICATION_JSON).entity(body).build();
  }
}
