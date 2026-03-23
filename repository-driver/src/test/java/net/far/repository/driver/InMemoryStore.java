package net.far.repository.driver;

import java.time.Instant;
import java.util.*;
import net.far.repository.model.*;
import net.far.repository.model.policy.*;
import net.far.repository.model.schema.*;
import net.far.repository.spi.Store;
import net.far.resolver.model.Event;
import net.far.resolver.model.Integrity;
import net.far.resolver.model.Peer;
import net.far.resolver.model.Urn;
import net.far.resolver.model.query.Query;

final class InMemoryStore implements Store {

  private final Map<String, Certificate> certificates = new LinkedHashMap<>();
  private final Map<String, Document> documents = new LinkedHashMap<>();
  private final Map<String, byte[]> blobs = new HashMap<>();
  private final Map<String, Set<String>> attachments = new LinkedHashMap<>();
  private final Map<String, Schema> schemas = new LinkedHashMap<>();
  private final Map<String, List<Schema>> history = new LinkedHashMap<>();
  private final Map<String, List<FieldPolicy>> policyMap = new LinkedHashMap<>();
  private final Map<String, List<Event>> events = new LinkedHashMap<>();

  @Override
  public Certificate save(final Certificate certificate) {
    certificates.put(certificate.urn().toString(), certificate);
    return certificate;
  }

  @Override
  public Optional<Certificate> find(final Urn urn) {
    return Optional.ofNullable(certificates.get(urn.toString()));
  }

  @Override
  public boolean exists(final Urn urn) {
    return certificates.containsKey(urn.toString());
  }

  @Override
  public Certificate update(final Urn urn, final Amendment amendment) {
    final var existing = certificates.get(urn.toString());
    if (existing == null) {
      throw new CertificateNotFoundException(urn.toString());
    }
    final var merged = new HashMap<>(existing.attributes());
    if (amendment.attributes() != null) {
      merged.putAll(amendment.attributes());
    }
    final var status = amendment.status() != null ? amendment.status() : existing.status();
    final var updated =
        new Certificate(
            existing.urn(),
            existing.namespace(),
            existing.identifier(),
            merged,
            status,
            existing.integrity(),
            existing.owner(),
            existing.schema(),
            existing.pin(),
            existing.created(),
            Instant.now());
    certificates.put(urn.toString(), updated);
    return updated;
  }

  @Override
  public Certificate update(final Urn urn, final Amendment amendment, final Integrity integrity) {
    final var updated = update(urn, amendment);
    return integrity(urn, integrity);
  }

  @Override
  public Certificate integrity(final Urn urn, final Integrity integrity) {
    final var existing = certificates.get(urn.toString());
    if (existing == null) {
      throw new CertificateNotFoundException(urn.toString());
    }
    final var updated =
        new Certificate(
            existing.urn(),
            existing.namespace(),
            existing.identifier(),
            existing.attributes(),
            existing.status(),
            integrity,
            existing.owner(),
            existing.schema(),
            existing.pin(),
            existing.created(),
            existing.modified());
    certificates.put(urn.toString(), updated);
    return updated;
  }

  @Override
  public Optional<String> owner(final String document) {
    for (final var entry : attachments.entrySet()) {
      if (entry.getValue().contains(document)) {
        final var certificate = certificates.get(entry.getKey());
        if (certificate != null) {
          return Optional.ofNullable(certificate.owner());
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public Page search(final Query query) {
    final var all = new ArrayList<>(certificates.values());
    final var total = all.size();
    final var skip = Math.min(query.skip(), total);
    final var end = Math.min(skip + query.top(), total);
    return new Page(all.subList(skip, end), total, query.skip(), query.top());
  }

  @Override
  public List<Certificate> recent(final int limit) {
    final var all = new ArrayList<>(certificates.values());
    Collections.reverse(all);
    return all.subList(0, Math.min(limit, all.size()));
  }

  @Override
  public Document save(final Document document, final byte[] content) {
    documents.put(document.id(), document);
    blobs.put(document.id(), content);
    return document;
  }

  @Override
  public Optional<Document> document(final String id) {
    return Optional.ofNullable(documents.get(id));
  }

  @Override
  public List<Document> documents() {
    return List.copyOf(documents.values());
  }

  @Override
  public Optional<byte[]> content(final String id) {
    return Optional.ofNullable(blobs.get(id));
  }

  @Override
  public void attach(final Urn urn, final String document) {
    attachments.computeIfAbsent(urn.toString(), k -> new LinkedHashSet<>()).add(document);
  }

  @Override
  public void detach(final Urn urn, final String document) {
    final var set = attachments.get(urn.toString());
    if (set != null) {
      set.remove(document);
    }
  }

  @Override
  public List<Document> documents(final Urn urn) {
    final var set = attachments.getOrDefault(urn.toString(), Set.of());
    return set.stream().map(documents::get).filter(Objects::nonNull).toList();
  }

  @Override
  public Schema save(final Schema schema) {
    schemas.put(schema.id(), schema);
    history.computeIfAbsent(schema.id(), k -> new ArrayList<>()).add(schema);
    return schema;
  }

  @Override
  public Optional<Schema> schema(final String id) {
    return Optional.ofNullable(schemas.get(id));
  }

  @Override
  public Optional<Schema> schema(final String id, final int version) {
    final var versions = history.get(id);
    if (versions == null) return Optional.empty();
    return versions.stream().filter(s -> s.version() == version).findFirst();
  }

  @Override
  public List<Schema> versions(final String id) {
    final var versions = history.get(id);
    if (versions == null) return List.of();
    final var reversed = new ArrayList<>(versions);
    Collections.reverse(reversed);
    return reversed;
  }

  @Override
  public Schema update(final String id, final Revision revision) {
    final var existing = schemas.get(id);
    if (existing == null) {
      throw new SchemaNotFoundException(id);
    }
    final var fields = revision.fields() != null ? revision.fields() : existing.fields();
    final var description =
        revision.description() != null ? revision.description() : existing.description();
    final var active = revision.active() != null ? revision.active() : existing.active();
    final var updated =
        new Schema(
            existing.id(),
            existing.namespace(),
            existing.name(),
            description,
            existing.version() + 1,
            fields,
            active,
            existing.owner(),
            existing.created(),
            Instant.now());
    schemas.put(id, updated);
    history.computeIfAbsent(id, k -> new ArrayList<>()).add(updated);
    return updated;
  }

  @Override
  public List<Schema> schemas(final String namespace) {
    return schemas.values().stream()
        .filter(schema -> namespace.equals(schema.namespace()))
        .toList();
  }

  @Override
  public List<Schema> schemas() {
    return List.copyOf(schemas.values());
  }

  @Override
  public List<FieldPolicy> policies(final String id) {
    return policyMap.getOrDefault(id, List.of());
  }

  @Override
  public void policies(final String id, final List<FieldPolicy> policies) {
    policyMap.put(id, List.copyOf(policies));
  }

  @Override
  public Peer save(final Peer peer) {
    return peer;
  }

  @Override
  public List<Peer> peers() {
    return List.of();
  }

  @Override
  public void remove(final String identity) {}

  @Override
  public void record(final Urn urn, final Event event) {
    events.computeIfAbsent(urn.toString(), k -> new ArrayList<>()).add(event);
  }

  @Override
  public List<Event> events(final Urn urn) {
    return events.getOrDefault(urn.toString(), List.of());
  }
}
