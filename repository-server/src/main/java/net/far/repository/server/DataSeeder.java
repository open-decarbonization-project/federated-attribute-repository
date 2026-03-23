package net.far.repository.server;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jdbi.v3.core.Jdbi;

@ApplicationScoped
public class DataSeeder {

  private static final Logger LOGGER = Logger.getLogger(DataSeeder.class.getName());

  @ConfigProperty(name = "repository.seed")
  Optional<String> seed;

  @Inject AgroalDataSource source;

  void startup(@Observes @Priority(1) final StartupEvent event) {
    if (seed.isEmpty() || seed.get().isBlank()) {
      LOGGER.info("No seed profile configured, skipping data seeding");
      return;
    }
    final var profile = seed.get();
    final var jdbi = Jdbi.create(source);

    final var count =
        jdbi.withHandle(
            handle ->
                handle.createQuery("SELECT count(*) FROM certificates").mapTo(Long.class).one());

    if (count > 0) {
      LOGGER.info(() -> String.format("Database already has %d certificates, skipping seed", count));
      return;
    }

    LOGGER.info(() -> String.format("Seeding data for profile: %s", profile));
    execute(jdbi, "db/seed/" + profile + "/V100__seed_" + profile + ".sql");
    execute(jdbi, "db/seed/" + profile + "/V101__peers_" + profile + ".sql");
  }

  private void execute(final Jdbi jdbi, final String path) {
    try (final var stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
      if (stream == null) {
        LOGGER.warning(() -> String.format("Seed file not found: %s", path));
        return;
      }
      final var sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      jdbi.useHandle(handle -> handle.createScript(sql).execute());
      LOGGER.info(() -> String.format("Executed seed file: %s", path));
    } catch (final IOException exception) {
      LOGGER.severe(
          () -> String.format("Failed to read seed file %s: %s", path, exception.getMessage()));
    }
  }
}
