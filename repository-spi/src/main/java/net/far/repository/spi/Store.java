package net.far.repository.spi;

import java.util.List;
import java.util.Optional;
import net.far.repository.model.Amendment;
import net.far.repository.model.Certificate;
import net.far.repository.model.Document;
import net.far.repository.model.Page;
import net.far.repository.model.policy.FieldPolicy;
import net.far.repository.model.schema.Revision;
import net.far.repository.model.schema.Schema;
import net.far.resolver.model.Event;
import net.far.resolver.model.Integrity;
import net.far.resolver.model.Peer;
import net.far.resolver.model.Urn;
import net.far.resolver.model.query.Query;

/**
 * Persistence SPI for the certificate repository. Implementations provide storage for certificates,
 * documents, schemas, field policies, peers, and audit events.
 *
 * <p>The store is the single source of truth for all repository state. It supports optimistic
 * concurrency via certificate versioning and JSONB-based attribute queries.
 */
public interface Store {

  // ── Certificates ──────────────────────────────────────────────────

  /** Persists a new certificate. The URN must be unique. */
  Certificate save(Certificate certificate);

  /** Finds a certificate by its URN, or empty if not registered. */
  Optional<Certificate> find(Urn urn);

  /** Returns true if a certificate with the given URN exists. */
  boolean exists(Urn urn);

  /** Applies an amendment (attribute merge and/or status change) without recomputing integrity. */
  Certificate update(Urn urn, Amendment amendment);

  /** Applies an amendment and sets a new integrity digest atomically. */
  Certificate update(Urn urn, Amendment amendment, Integrity integrity);

  /** Updates only the integrity digest of an existing certificate. */
  Certificate integrity(Urn urn, Integrity integrity);

  /** Returns the owner of the certificate to which a document is attached, if any. */
  Optional<String> owner(String document);

  /** Searches certificates using an OData-style filter with pagination and sorting. */
  Page search(Query query);

  /** Returns the most recently modified certificates, up to the given limit. */
  List<Certificate> recent(int limit);

  // ── Documents ─────────────────────────────────────────────────────

  /** Persists a document's metadata and binary content. */
  Document save(Document document, byte[] content);

  /** Finds document metadata by ID. */
  Optional<Document> document(String id);

  /** Lists all document metadata. */
  List<Document> documents();

  /** Retrieves the binary content of a document, or empty if not found. */
  Optional<byte[]> content(String id);

  /** Attaches a document to a certificate. */
  void attach(Urn urn, String document);

  /** Detaches a document from a certificate. */
  void detach(Urn urn, String document);

  /** Lists all documents attached to a certificate. */
  List<Document> documents(Urn urn);

  // ── Schemas ───────────────────────────────────────────────────────

  /** Persists a new schema definition. */
  Schema save(Schema schema);

  /** Finds the latest version of a schema by ID. */
  Optional<Schema> schema(String id);

  /** Finds a specific version of a schema. */
  Optional<Schema> schema(String id, int version);

  /** Lists all versions of a schema in descending order. */
  List<Schema> versions(String id);

  /** Creates a new schema revision with incremented version number. */
  Schema update(String id, Revision revision);

  /** Lists all schemas in a namespace. */
  List<Schema> schemas(String namespace);

  /** Lists all schemas across all namespaces. */
  List<Schema> schemas();

  // ── Field Policies ────────────────────────────────────────────────

  /** Returns the field-level access policies for a schema. */
  List<FieldPolicy> policies(String id);

  /** Replaces all field policies for a schema. */
  void policies(String id, List<FieldPolicy> policies);

  // ── Peers ─────────────────────────────────────────────────────────

  /** Persists or updates a federated peer registration (upsert by identity). */
  Peer save(Peer peer);

  /** Lists all registered peers. */
  List<Peer> peers();

  /** Removes a peer by identity. */
  void remove(String identity);

  // ── Audit Events ──────────────────────────────────────────────────

  /** Records a lifecycle event against a certificate. */
  void record(Urn urn, Event event);

  /** Returns all lifecycle events for a certificate in chronological order. */
  List<Event> events(Urn urn);
}
