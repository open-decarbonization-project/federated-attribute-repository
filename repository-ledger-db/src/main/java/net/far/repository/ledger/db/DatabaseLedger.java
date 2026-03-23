package net.far.repository.ledger.db;

import com.fasterxml.uuid.Generators;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.inject.spi.CDI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import net.far.repository.model.Certificate;
import net.far.repository.model.LedgerEntry;
import net.far.repository.spi.Ledger;
import net.far.resolver.model.Urn;
import net.far.resolver.signature.DigestCalculator;
import org.jdbi.v3.core.Jdbi;

public class DatabaseLedger implements Ledger {

  private volatile Jdbi jdbi;

  private static String compute(final Certificate certificate) {
    if (certificate.integrity() != null) {
      return certificate.integrity().digest();
    }
    final var canonical = certificate.urn().toString() + "|" + certificate.status().name();
    return DigestCalculator.compute(canonical.getBytes(StandardCharsets.UTF_8));
  }

  private Jdbi jdbi() {
    if (jdbi == null) {
      synchronized (this) {
        if (jdbi == null) {
          final var source = CDI.current().select(AgroalDataSource.class).get();
          jdbi = Jdbi.create(source);
        }
      }
    }
    return jdbi;
  }

  @Override
  public String name() {
    return "database";
  }

  @Override
  public LedgerEntry publish(final Certificate certificate) {
    final var hash = compute(certificate);
    final var id = Generators.timeBasedEpochGenerator().generate();
    final var now = Instant.now();

    final var previous =
        jdbi()
            .withHandle(
                handle ->
                    handle
                        .createQuery(
                            "SELECT hash FROM ledger_entries WHERE urn = :urn ORDER BY published"
                                + " DESC LIMIT 1")
                        .bind("urn", certificate.urn().toString())
                        .mapTo(String.class)
                        .findFirst()
                        .orElse(null));

    final var proof =
        previous != null
            ? DigestCalculator.compute((previous + "|" + hash).getBytes(StandardCharsets.UTF_8))
            : hash;

    jdbi()
        .useHandle(
            handle ->
                handle
                    .createUpdate(
                        """
                        INSERT INTO ledger_entries (id, urn, ledger, hash, proof, published)
                        VALUES (:id, :urn, :ledger, :hash, :proof, :published)
                        """)
                    .bind("id", id)
                    .bind("urn", certificate.urn().toString())
                    .bind("ledger", name())
                    .bind("hash", hash)
                    .bind("proof", proof)
                    .bind("published", now)
                    .execute());

    return new LedgerEntry(id.toString(), certificate.urn(), name(), hash, proof, now);
  }

  @Override
  public List<LedgerEntry> entries(final Urn certificate) {
    return jdbi()
        .withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT * FROM ledger_entries WHERE urn = :urn ORDER BY published DESC")
                    .bind("urn", certificate.toString())
                    .map(
                        (rs, context) ->
                            new LedgerEntry(
                                rs.getObject("id", java.util.UUID.class).toString(),
                                Urn.parse(rs.getString("urn")),
                                rs.getString("ledger"),
                                rs.getString("hash"),
                                rs.getString("proof"),
                                rs.getTimestamp("published").toInstant()))
                    .list());
  }

  @Override
  public Optional<LedgerEntry> verify(final Urn certificate) {
    return jdbi()
        .withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT * FROM ledger_entries WHERE urn = :urn ORDER BY published DESC"
                            + " LIMIT 1")
                    .bind("urn", certificate.toString())
                    .map(
                        (rs, context) ->
                            new LedgerEntry(
                                rs.getObject("id", java.util.UUID.class).toString(),
                                Urn.parse(rs.getString("urn")),
                                rs.getString("ledger"),
                                rs.getString("hash"),
                                rs.getString("proof"),
                                rs.getTimestamp("published").toInstant()))
                    .findFirst());
  }
}
