package net.far.repository.web;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/v1/auth")
@PermitAll
public class AuthResource {

  @ConfigProperty(name = "repository.oidc.authority", defaultValue = "")
  String authority;

  @ConfigProperty(name = "repository.oidc.client", defaultValue = "repository-ui")
  String client;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response config() {
    return Response.ok(
            Map.of(
                "authority", authority,
                "client", client))
        .build();
  }
}
