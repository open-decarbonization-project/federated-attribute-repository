package net.far.repository.spi;

import java.util.List;
import java.util.Optional;
import net.far.repository.model.Certificate;
import net.far.repository.model.LedgerEntry;
import net.far.resolver.model.Urn;

/**
 * Publication SPI for certificate ledgers. Implementations anchor certificate metadata to an
 * external tamper-evident store (database, blockchain, etc.).
 *
 * <p>Ledger implementations are discovered via {@link java.util.ServiceLoader} and registered in
 * {@link LedgerRegistry}.
 */
public interface Ledger {

  /** Unique name of this ledger (e.g. "database"). */
  String name();

  /** Publishes a certificate's metadata to this ledger and returns the entry. */
  LedgerEntry publish(Certificate certificate);

  /** Returns all ledger entries for a certificate. */
  List<LedgerEntry> entries(Urn certificate);

  /** Verifies a certificate against this ledger, returning the entry if valid. */
  Optional<LedgerEntry> verify(Urn certificate);
}
