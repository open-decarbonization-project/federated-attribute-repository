package net.far.repository.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Urn;
import net.far.resolver.model.Value;
import org.junit.jupiter.api.Test;

class CertificateTests {

  @Test
  void shouldCreateWithDefaults() {
    final var urn = new Urn("test", "CERT-001");
    final var certificate =
        new Certificate(urn, "test", "CERT-001", null, null, null, "owner", null, null, null, null);

    assertThat(certificate.urn()).isEqualTo(urn);
    assertThat(certificate.namespace()).isEqualTo("test");
    assertThat(certificate.identifier()).isEqualTo("CERT-001");
    assertThat(certificate.attributes()).isEmpty();
    assertThat(certificate.status()).isEqualTo(Status.DRAFT);
    assertThat(certificate.created()).isNotNull();
    assertThat(certificate.modified()).isEqualTo(certificate.created());
  }

  @Test
  void shouldCreateWithAttributes() {
    final var urn = new Urn("test", "CERT-001");
    final var attributes =
        Map.of(
            "volume",
            new Attribute("volume", Value.of(1000, "tCO2e"), "repository", true, Instant.now()));
    final var certificate =
        new Certificate(
            urn,
            "test",
            "CERT-001",
            attributes,
            Status.ACTIVE,
            null,
            "owner",
            null,
            null,
            null,
            null);

    assertThat(certificate.attributes()).hasSize(1);
    assertThat(certificate.status()).isEqualTo(Status.ACTIVE);
  }

  @Test
  void shouldRejectNullUrn() {
    assertThatThrownBy(
            () ->
                new Certificate(
                    null, "test", "CERT-001", null, null, null, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankNamespace() {
    final var urn = new Urn("test", "CERT-001");
    assertThatThrownBy(
            () ->
                new Certificate(
                    urn, "", "CERT-001", null, null, null, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankIdentifier() {
    final var urn = new Urn("test", "CERT-001");
    assertThatThrownBy(
            () -> new Certificate(urn, "test", "", null, null, null, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldDefaultNullAttributesToEmpty() {
    final var urn = new Urn("test", "CERT-001");
    final var certificate =
        new Certificate(urn, "test", "CERT-001", null, null, null, null, null, null, null, null);

    assertThat(certificate.attributes()).isNotNull();
    assertThat(certificate.attributes()).isEmpty();
  }

  @Test
  void shouldDefaultNullStatusToDraft() {
    final var urn = new Urn("test", "CERT-001");
    final var certificate =
        new Certificate(urn, "test", "CERT-001", null, null, null, null, null, null, null, null);

    assertThat(certificate.status()).isEqualTo(Status.DRAFT);
  }

  @Test
  void shouldDefaultTimestamps() {
    final var urn = new Urn("test", "CERT-001");
    final var certificate =
        new Certificate(urn, "test", "CERT-001", null, null, null, null, null, null, null, null);

    assertThat(certificate.created()).isNotNull();
    assertThat(certificate.modified()).isNotNull();
    assertThat(certificate.modified()).isEqualTo(certificate.created());
  }

  @Test
  void shouldPreserveExplicitTimestamps() {
    final var urn = new Urn("test", "CERT-001");
    final var created = Instant.parse("2025-01-01T00:00:00Z");
    final var modified = Instant.parse("2025-06-01T00:00:00Z");
    final var certificate =
        new Certificate(
            urn, "test", "CERT-001", null, null, null, null, null, null, created, modified);

    assertThat(certificate.created()).isEqualTo(created);
    assertThat(certificate.modified()).isEqualTo(modified);
  }

  @Test
  void shouldAcceptAllStatuses() {
    final var urn = new Urn("test", "CERT-001");

    for (final var status : Status.values()) {
      final var certificate =
          new Certificate(
              urn, "test", "CERT-001", null, status, null, null, null, null, null, null);
      assertThat(certificate.status()).isEqualTo(status);
    }
  }

  @Test
  void shouldAcceptIntegrity() {
    final var urn = new Urn("test", "CERT-001");
    final var integrity = new net.far.resolver.model.Integrity("abc123", "sha-256");
    final var certificate =
        new Certificate(
            urn, "test", "CERT-001", null, null, integrity, null, null, null, null, null);

    assertThat(certificate.integrity()).isNotNull();
    assertThat(certificate.integrity().digest()).isEqualTo("abc123");
  }

  @Test
  void shouldAcceptSchemaAndPin() {
    final var urn = new Urn("test", "CERT-001");
    final var certificate =
        new Certificate(
            urn, "test", "CERT-001", null, null, null, "owner", "schema-id", 3, null, null);

    assertThat(certificate.schema()).isEqualTo("schema-id");
    assertThat(certificate.pin()).isEqualTo(3);
  }

  @Test
  void shouldDefaultVersionToOne() {
    final var urn = new Urn("test", "CERT-001");
    final var certificate =
        new Certificate(urn, "test", "CERT-001", null, null, null, null, null, null, null, null);

    assertThat(certificate.version()).isEqualTo(1);
  }

  @Test
  void shouldAcceptExplicitVersion() {
    final var urn = new Urn("test", "CERT-001");
    final var certificate =
        new Certificate(urn, "test", "CERT-001", null, null, null, null, null, null, 5, null, null);

    assertThat(certificate.version()).isEqualTo(5);
  }
}
