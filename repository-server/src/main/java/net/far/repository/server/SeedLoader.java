package net.far.repository.server;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import net.far.repository.core.Binder;
import net.far.resolver.model.Urn;

@ApplicationScoped
public class SeedLoader {

  private static final Logger LOGGER = Logger.getLogger(SeedLoader.class.getName());

  private static final List<Seed> SEEDS =
      List.of(
          new Seed(
              "audit-report.pdf",
              "VCS-2847-2024-verification-report.pdf",
              "application/pdf",
              List.of("urn:far:carbon:VCS-2847-2024")),
          new Seed(
              "satellite-region.jpg",
              "VCS-2847-2024-rimba-raya-satellite.jpg",
              "image/jpeg",
              List.of("urn:far:carbon:VCS-2847-2024")),
          new Seed(
              "compliance-certificate.pdf",
              "EMS-2024-ACME-001-bsi-certificate.pdf",
              "application/pdf",
              List.of("urn:far:iso:EMS-2024-ACME-001")),
          new Seed(
              "certificate-of-origin.pdf",
              "FT-COF-ETH-2024-038-origin-certificate.pdf",
              "application/pdf",
              List.of("urn:far:trade:FT-COF-ETH-2024-038")),
          new Seed(
              "organic-certification.pdf",
              "ORG-CA-2024-1187-organic-certification.pdf",
              "application/pdf",
              List.of("urn:far:agriculture:ORG-CA-2024-1187", "urn:far:trade:FT-COF-ETH-2024-038")),
          new Seed(
              "energy-certificate.pdf",
              "REC-SOLAR-TX-2024-Q3-energy-attributes.pdf",
              "application/pdf",
              List.of("urn:far:energy:REC-SOLAR-TX-2024-Q3")),
          new Seed(
              "import-certificate.pdf",
              "PROV-LI-BAT-2024-0892-compliance.pdf",
              "application/pdf",
              List.of("urn:far:supply-chain:PROV-LI-BAT-2024-0892")));

  @Inject Binder binder;

  void load(@Observes @Priority(1) final StartupEvent event) {
    if (!binder.documents().isEmpty()) {
      LOGGER.info("Documents already seeded, skipping");
      return;
    }
    LOGGER.info("Seeding sample documents...");
    for (final var seed : SEEDS) {
      try (final var stream =
          Thread.currentThread()
              .getContextClassLoader()
              .getResourceAsStream("seed/" + seed.file())) {
        if (stream == null) {
          LOGGER.warning(() -> String.format("Seed file not found: %s", seed.file()));
          continue;
        }
        final var bytes = stream.readAllBytes();
        final var document = binder.upload(seed.name(), seed.media(), bytes);
        for (final var urn : seed.certificates()) {
          try {
            binder.attach(Urn.parse(urn), document.id());
          } catch (final Exception exception) {
            LOGGER.warning(
                () -> String.format("Could not attach to %s: %s", urn, exception.getMessage()));
          }
        }
        LOGGER.info(
            () ->
                String.format(
                    "Seeded document: %s (%d bytes, %d certificates)",
                    seed.name(), bytes.length, seed.certificates().size()));
      } catch (final IOException exception) {
        LOGGER.warning(
            () ->
                String.format(
                    "Failed to seed document %s: %s", seed.file(), exception.getMessage()));
      }
    }
  }

  private record Seed(String file, String name, String media, List<String> certificates) {}
}
