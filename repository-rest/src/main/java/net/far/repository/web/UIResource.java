package net.far.repository.web;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;

/**
 * Catch-all resource that forwards non-API browser requests to the Vue SPA's
 * {@code index.html}. Without this, client-side routes (e.g. {@code /certificates/urn:far:...})
 * would return 404 on browser refresh. Requests to {@code /v1/} (REST API) and
 * {@code /q/} (Quarkus dev UI) are excluded via the path regex and handled by
 * their respective resources.
 */
@Path("/")
@PermitAll
public class UIResource {

  @GET
  @Path("/{path: (?!v1|q/).*}")
  @Produces(MediaType.TEXT_HTML)
  public InputStream forward() {
    return getClass().getClassLoader().getResourceAsStream("META-INF/resources/index.html");
  }
}
