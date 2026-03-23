package net.far.repository.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.far.resolver.model.Urn;
import org.junit.jupiter.api.Test;

class LedgerEntryTests {

  @Test
  void shouldCreateEntry() {
    final var urn = new Urn("test", "CERT-001");
    final var entry = new LedgerEntry("entry-1", urn, "database", "sha-256=:abc:", null, null);

    assertThat(entry.id()).isEqualTo("entry-1");
    assertThat(entry.certificate()).isEqualTo(urn);
    assertThat(entry.ledger()).isEqualTo("database");
    assertThat(entry.published()).isNotNull();
  }

  @Test
  void shouldRejectBlankLedger() {
    final var urn = new Urn("test", "CERT-001");
    assertThatThrownBy(() -> new LedgerEntry("entry-1", urn, "", null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
