package net.far.repository.web;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import net.far.resolver.core.PeerRegistry;
import net.far.resolver.spi.DriverRegistry;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/v1/health")
@PermitAll
public class HealthResource {

  @Inject DriverRegistry drivers;

  @Inject PeerRegistry peers;

  @ConfigProperty(name = "repository.protocol.version", defaultValue = "0.1.0")
  String version;

  private Instant started;

  @PostConstruct
  void init() {
    started = Instant.now();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response health() {
    final var local = (int) drivers.supported().size();
    final var delegated =
        (int)
            peers.namespaces().stream()
                .filter(namespace -> !drivers.supported().contains(namespace))
                .count();
    final var connected = peers.all().size();

    final var result = new LinkedHashMap<String, Object>();
    result.put("status", "healthy");
    result.put("version", version);
    result.put("uptime", Duration.between(started, Instant.now()).toString());
    result.put(
        "namespaces",
        Map.of(
            "local", local,
            "delegated", delegated,
            "total", local + delegated));
    result.put(
        "peers",
        Map.of(
            "connected", connected,
            "total", connected));
    result.put("timestamp", Instant.now().toString());
    return Response.ok(result).build();
  }
}
