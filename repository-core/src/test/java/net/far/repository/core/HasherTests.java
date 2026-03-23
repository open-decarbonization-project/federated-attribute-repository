package net.far.repository.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import net.far.repository.model.Certificate;
import net.far.repository.model.Status;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Urn;
import net.far.resolver.model.Value;
import org.junit.jupiter.api.Test;

class HasherTests {

  private static Certificate certificate(
      final String identifier,
      final Map<String, Attribute> attributes,
      final Status status) {
    final var urn = new Urn("test", identifier);
    final var now = Instant.parse("2024-01-01T00:00:00Z");
    return new Certificate(
        urn, "test", identifier, attributes, status, null, "owner", null, null, now, now);
  }

  @Test
  void shouldComputeDigest() {
    final var cert =
        certificate(
            "CERT-001",
            Map.of("volume", new Attribute("volume", Value.of(1000, "tCO2e"), "repo", true, null)),
            Status.ACTIVE);

    final var digest = Hasher.compute(cert);

    assertThat(digest).startsWith("sha-256=:");
    assertThat(digest).endsWith(":");
  }

  @Test
  void shouldProduceDeterministicDigest() {
    final var cert =
        certificate(
            "CERT-001",
            Map.of("volume", new Attribute("volume", Value.of(1000), "repo", true, null)),
            Status.DRAFT);

    assertThat(Hasher.compute(cert)).isEqualTo(Hasher.compute(cert));
  }

  @Test
  void shouldDifferByStatus() {
    final var attributes =
        Map.of("name", new Attribute("name", Value.of("Alice"), "repo", true, null));
    final var draft = certificate("CERT-002", attributes, Status.DRAFT);
    final var active = certificate("CERT-002", attributes, Status.ACTIVE);

    assertThat(Hasher.compute(draft)).isNotEqualTo(Hasher.compute(active));
  }

  @Test
  void shouldDifferByAttributes() {
    final var first =
        certificate(
            "CERT-003",
            Map.of("name", new Attribute("name", Value.of("Alice"), "repo", true, null)),
            Status.DRAFT);
    final var second =
        certificate(
            "CERT-003",
            Map.of("name", new Attribute("name", Value.of("Bob"), "repo", true, null)),
            Status.DRAFT);

    assertThat(Hasher.compute(first)).isNotEqualTo(Hasher.compute(second));
  }

  @Test
  void shouldDifferByIdentifier() {
    final var first = certificate("CERT-A", Map.of(), Status.DRAFT);
    final var second = certificate("CERT-B", Map.of(), Status.DRAFT);

    assertThat(Hasher.compute(first)).isNotEqualTo(Hasher.compute(second));
  }

  @Test
  void shouldHandleEmptyAttributes() {
    final var cert = certificate("CERT-004", Map.of(), Status.DRAFT);

    final var digest = Hasher.compute(cert);

    assertThat(digest).startsWith("sha-256=:");
  }

  @Test
  void shouldHandleMultipleAttributesSorted() {
    final var forward =
        certificate(
            "CERT-005",
            Map.of(
                "alpha", new Attribute("alpha", Value.of("a"), "repo", true, null),
                "beta", new Attribute("beta", Value.of("b"), "repo", true, null)),
            Status.DRAFT);
    final var reverse =
        certificate(
            "CERT-005",
            Map.of(
                "beta", new Attribute("beta", Value.of("b"), "repo", true, null),
                "alpha", new Attribute("alpha", Value.of("a"), "repo", true, null)),
            Status.DRAFT);

    assertThat(Hasher.compute(forward)).isEqualTo(Hasher.compute(reverse));
  }
}
