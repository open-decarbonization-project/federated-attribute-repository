package net.far.repository.core;

import com.fasterxml.uuid.Generators;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.far.repository.model.Amendment;
import net.far.repository.model.Certificate;
import net.far.repository.model.CertificateNotFoundException;
import net.far.repository.model.DuplicateCertificateException;
import net.far.repository.model.InvalidSubmissionException;
import net.far.repository.model.LedgerEntry;
import net.far.repository.model.SchemaNotFoundException;
import net.far.repository.model.Status;
import net.far.repository.model.Submission;
import net.far.repository.model.policy.FieldPolicy;
import net.far.repository.model.schema.Definition;
import net.far.repository.model.schema.Revision;
import net.far.repository.model.schema.Schema;
import net.far.repository.spi.LedgerRegistry;
import net.far.repository.spi.Store;
import net.far.resolver.model.Event;
import net.far.resolver.model.Integrity;
import net.far.resolver.model.Urn;
import net.far.resolver.model.query.Query;

/**
 * Central orchestrator for certificate lifecycle operations. Coordinates between the {@link Store}
 * for persistence, {@link Hasher} for integrity computation, {@link Validator} for schema
 * enforcement, and {@link LedgerRegistry} for publication.
 *
 * <p>Every mutation (create, update, retire) recomputes the certificate's integrity digest and
 * records an audit event.
 */
public class Repository {

  private final Store store;
  private final LedgerRegistry ledgers;

  public Repository(final Store store, final LedgerRegistry ledgers) {
    this.store = store;
    this.ledgers = ledgers;
  }

  public Certificate create(final Submission submission) {
    final var urn = new Urn(submission.namespace(), submission.identifier());
    if (store.exists(urn)) {
      throw new DuplicateCertificateException(urn.toString());
    }
    var attributes = submission.attributes();
    Integer pin = submission.pin();
    if (submission.schema() != null) {
      final var target =
          pin != null ? schema(submission.schema(), pin) : schema(submission.schema());
      if (!submission.namespace().equals(target.namespace())) {
        throw new InvalidSubmissionException(
            "Schema namespace '"
                + target.namespace()
                + "' does not match certificate namespace '"
                + submission.namespace()
                + "'");
      }
      attributes = Coercer.coerce(attributes, target);
      final var violations = Validator.validate(attributes, target);
      if (!violations.isEmpty()) {
        throw new InvalidSubmissionException(
            "Schema validation failed: " + String.join("; ", violations));
      }
      pin = target.version();
    }
    final var now = Instant.now();
    final var certificate =
        new Certificate(
            urn,
            submission.namespace(),
            submission.identifier(),
            attributes,
            Status.DRAFT,
            null,
            submission.owner(),
            submission.schema(),
            pin,
            now,
            now);
    final var digest = Hasher.compute(certificate);
    final var integrity = new Integrity(digest, Integrity.SHA256);
    final var signed =
        new Certificate(
            urn,
            certificate.namespace(),
            certificate.identifier(),
            certificate.attributes(),
            certificate.status(),
            integrity,
            certificate.owner(),
            certificate.schema(),
            certificate.pin(),
            certificate.created(),
            certificate.modified());
    final var saved = store.save(signed);
    store.record(
        urn,
        new Event(
            Event.EventType.ISSUANCE,
            Instant.now(),
            submission.owner(),
            Map.of("status", saved.status().name())));
    return saved;
  }

  public Certificate find(final Urn urn) {
    return store.find(urn).orElseThrow(() -> new CertificateNotFoundException(urn.toString()));
  }

  public boolean exists(final Urn urn) {
    return store.exists(urn);
  }

  public Certificate update(final Urn urn, final Amendment amendment) {
    final var existing = find(urn);
    final var merged = new java.util.HashMap<>(existing.attributes());
    if (amendment.attributes() != null && !amendment.attributes().isEmpty()) {
      merged.putAll(amendment.attributes());
    }
    if (existing.schema() != null && amendment.attributes() != null) {
      final var target =
          existing.pin() != null
              ? schema(existing.schema(), existing.pin())
              : schema(existing.schema());
      final var violations = Validator.validate(merged, target);
      if (!violations.isEmpty()) {
        throw new InvalidSubmissionException(
            "Schema validation failed: " + String.join("; ", violations));
      }
    }
    final var status = amendment.status() != null ? amendment.status() : existing.status();
    final var preview =
        new Certificate(
            urn,
            existing.namespace(),
            existing.identifier(),
            merged,
            status,
            null,
            existing.owner(),
            existing.schema(),
            existing.pin(),
            existing.created(),
            java.time.Instant.now());
    final var digest = Hasher.compute(preview);
    final var integrity = new Integrity(digest, Integrity.SHA256);
    final var updated = store.update(urn, amendment, integrity);
    store.record(
        urn,
        new Event(
            Event.EventType.STATUS_CHANGE,
            Instant.now(),
            existing.owner(),
            Map.of("status", updated.status().name())));
    return updated;
  }

  public net.far.repository.model.Page search(final Query query) {
    return store.search(query);
  }

  public List<Certificate> recent(final int limit) {
    return store.recent(limit);
  }

  public Certificate retire(final Urn urn) {
    final var existing = find(urn);
    final var preview =
        new Certificate(
            urn,
            existing.namespace(),
            existing.identifier(),
            existing.attributes(),
            Status.RETIRED,
            null,
            existing.owner(),
            existing.schema(),
            existing.pin(),
            existing.created(),
            java.time.Instant.now());
    final var digest = Hasher.compute(preview);
    final var integrity = new Integrity(digest, Integrity.SHA256);
    final var amendment = new Amendment(null, Status.RETIRED);
    final var retired = store.update(urn, amendment, integrity);
    store.record(
        urn,
        new Event(
            Event.EventType.RETIREMENT,
            Instant.now(),
            existing.owner(),
            Map.of("status", retired.status().name())));
    return retired;
  }

  public List<LedgerEntry> publish(final Urn urn) {
    final var certificate = find(urn);
    final var entries = new ArrayList<LedgerEntry>();
    for (final var ledger : ledgers.all()) {
      final var entry = ledger.publish(certificate);
      entries.add(entry);
    }
    return entries;
  }

  public Schema define(final Definition definition) {
    final var now = Instant.now();
    final var schema =
        new Schema(
            Generators.timeBasedEpochGenerator().generate().toString(),
            definition.namespace(),
            definition.name(),
            definition.description(),
            1,
            definition.fields(),
            true,
            definition.owner(),
            now,
            now);
    return store.save(schema);
  }

  public Schema schema(final String id) {
    return store.schema(id).orElseThrow(() -> new SchemaNotFoundException(id));
  }

  public Schema schema(final String id, final int version) {
    return store.schema(id, version).orElseThrow(() -> new SchemaNotFoundException(id));
  }

  public List<Schema> versions(final String id) {
    return store.versions(id);
  }

  public Schema revise(final String id, final Revision revision) {
    return store.update(id, revision);
  }

  public List<Schema> schemas() {
    return store.schemas();
  }

  public List<Schema> schemas(final String namespace) {
    return store.schemas(namespace);
  }

  public List<FieldPolicy> policies(final String id) {
    return store.policies(id);
  }

  public void policies(final String id, final List<FieldPolicy> policies) {
    store.policies(id, policies);
  }

  public List<Event> events(final Urn urn) {
    return store.events(urn);
  }

  public List<LedgerEntry> entries(final Urn urn) {
    final var entries = new ArrayList<LedgerEntry>();
    for (final var ledger : ledgers.all()) {
      entries.addAll(ledger.entries(urn));
    }
    return entries;
  }
}
