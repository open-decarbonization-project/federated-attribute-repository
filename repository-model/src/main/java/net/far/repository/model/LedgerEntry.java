package net.far.repository.model;

import java.time.Instant;
import net.far.resolver.model.Urn;

/**
 * A ledger entry anchoring a certificate's integrity digest to an external tamper-evident store.
 */
public record LedgerEntry(
    String id, Urn certificate, String ledger, String hash, String proof, Instant published) {

  public LedgerEntry {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Entry id must not be blank");
    }
    if (certificate == null) {
      throw new IllegalArgumentException("Certificate URN must not be null");
    }
    if (ledger == null || ledger.isBlank()) {
      throw new IllegalArgumentException("Ledger name must not be blank");
    }
    if (hash == null || hash.isBlank()) {
      throw new IllegalArgumentException("Hash must not be blank");
    }
    if (published == null) {
      published = Instant.now();
    }
  }
}
