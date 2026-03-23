package net.far.repository.server;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.far.repository.spi.Store;
import net.far.resolver.core.PeerRegistry;
import net.far.resolver.model.Peer;
import net.far.resolver.spi.FarClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Startup lifecycle bean that loads peers from the database, probes their configuration endpoints
 * to fetch public keys, and schedules periodic re-probes to keep key material and namespace
 * mappings fresh. Validates identity consistency on each probe to detect endpoint repointing.
 */
@ApplicationScoped
public class PeerLoader {

  private static final Logger LOGGER = Logger.getLogger(PeerLoader.class.getName());
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  @Inject Store store;
  @Inject PeerRegistry peers;
  @Inject FarClient client;

  @ConfigProperty(name = "repository.peers.interval", defaultValue = "300")
  int interval;

  private static boolean trusted(final String base, final String endpoint) {
    try {
      final var origin = URI.create(endpoint);
      final var target = URI.create(base);
      final var scheme = origin.getScheme() != null ? origin.getScheme() : "";
      return scheme.equalsIgnoreCase(target.getScheme())
          && origin.getHost() != null
          && origin.getHost().equalsIgnoreCase(target.getHost())
          && (origin.getPort() == target.getPort() || target.getPort() < 0 || origin.getPort() < 0);
    } catch (final Exception ignored) {
      return false;
    }
  }

  void startup(@Observes @Priority(2) final StartupEvent event) {
    load();
    scheduler.schedule(this::probe, 10, TimeUnit.SECONDS);
    scheduler.scheduleAtFixedRate(this::probe, interval, interval, TimeUnit.SECONDS);
  }

  void shutdown(@Observes final ShutdownEvent event) {
    scheduler.shutdownNow();
  }

  private void load() {
    final var loaded = store.peers();
    for (final var peer : loaded) {
      peers.register(peer);
      LOGGER.info(
          () ->
              String.format(
                  "Registered peer: %s → %s (namespaces: %s)",
                  peer.identity(), peer.endpoint(), peer.namespaces()));
      refresh(peer);
    }
    LOGGER.info(() -> String.format("Loaded %d peers from database", loaded.size()));
  }

  private void probe() {
    try {
      for (final var peer : store.peers()) {
        refresh(peer);
      }
    } catch (final Exception exception) {
      LOGGER.severe(() -> String.format("Peer probe cycle failed: %s", exception.getMessage()));
    }
  }

  private void refresh(final Peer peer) {
    try {
      final var config = client.configuration(peer.endpoint());
      if (config.isPresent()) {
        final var remote = config.get();
        final var remoteIdentity = remote.get("identity") instanceof String id ? id : null;
        if (remoteIdentity != null && !remoteIdentity.equals(peer.identity())) {
          LOGGER.warning(
              () ->
                  String.format(
                      "Peer identity mismatch: expected %s but endpoint %s declared %s — skipping"
                          + " refresh",
                      peer.identity(), peer.endpoint(), remoteIdentity));
          return;
        }
        final var namespaces =
            remote.get("namespaces") instanceof List<?> list
                ? list.stream().map(Object::toString).collect(Collectors.toSet())
                : peer.namespaces();
        final var key =
            remote.get("public_key") instanceof Map<?, ?> pk
                ? (String) pk.get("public_key_pem")
                : peer.key();
        final var keyId =
            remote.get("public_key") instanceof Map<?, ?> pk2
                ? (String) pk2.get("key_id")
                : peer.keyId();
        final var previous = new ArrayList<Peer.PeerKey>();
        if (remote.get("previous_keys") instanceof List<?> keys) {
          for (final var entry : keys) {
            if (entry instanceof Map<?, ?> pk) {
              final var id = (String) pk.get("key_id");
              final var pem = (String) pk.get("public_key_pem");
              final var expires =
                  pk.get("expires") != null ? Instant.parse((String) pk.get("expires")) : null;
              previous.add(new Peer.PeerKey(id, pem, expires));
            }
          }
        }
        final var declared = remote.get("api_base") instanceof String ab ? ab : peer.base();
        final var base = trusted(declared, peer.endpoint()) ? declared : peer.endpoint() + "/v1";
        final var depth =
            remote.get("delegation") instanceof Map<?, ?> delegation
                    && delegation.get("max_depth") instanceof Number max
                ? max.intValue()
                : peer.depth();
        final var updated =
            new Peer(
                peer.identity(),
                peer.endpoint(),
                namespaces,
                key,
                keyId,
                previous,
                Instant.now(),
                peer.priority(),
                peer.enabled(),
                base,
                depth);
        peers.register(updated);
        store.save(updated);
        LOGGER.fine(
            () -> String.format("Probed peer: %s (namespaces: %s)", peer.identity(), namespaces));
      } else {
        LOGGER.warning(() -> String.format("Peer unreachable during probe: %s", peer.endpoint()));
      }
    } catch (final Exception exception) {
      LOGGER.warning(
          () ->
              String.format(
                  "Failed to probe peer %s: %s", peer.endpoint(), exception.getMessage()));
    }
  }
}
