package net.far.repository.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import net.far.repository.model.Certificate;
import net.far.repository.model.LedgerEntry;
import net.far.resolver.model.Urn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LedgerRegistryTests {

  private LedgerRegistry ledgers;

  @BeforeEach
  void setup() {
    ledgers = new LedgerRegistry();
  }

  @Test
  void shouldRegisterAndFind() {
    final var ledger = new StubLedger("test");
    ledgers.register(ledger);

    assertThat(ledgers.find("test")).isPresent();
    assertThat(ledgers.find("test").get().name()).isEqualTo("test");
  }

  @Test
  void shouldReturnEmptyForUnknown() {
    assertThat(ledgers.find("unknown")).isEmpty();
  }

  @Test
  void shouldReturnAllLedgers() {
    ledgers.register(new StubLedger("alpha"));
    ledgers.register(new StubLedger("beta"));

    assertThat(ledgers.all()).hasSize(2);
  }

  private static class StubLedger implements Ledger {
    private final String name;

    StubLedger(final String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public LedgerEntry publish(final Certificate certificate) {
      return null;
    }

    @Override
    public List<LedgerEntry> entries(final Urn certificate) {
      return List.of();
    }

    @Override
    public Optional<LedgerEntry> verify(final Urn certificate) {
      return Optional.empty();
    }
  }
}
