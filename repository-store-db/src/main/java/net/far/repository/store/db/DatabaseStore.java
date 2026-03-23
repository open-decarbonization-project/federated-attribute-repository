package net.far.repository.store.db;

import com.fasterxml.uuid.Generators;
import io.agroal.api.AgroalDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import net.far.repository.model.Amendment;
import net.far.repository.model.Certificate;
import net.far.repository.model.CertificateNotFoundException;
import net.far.repository.model.Document;
import net.far.repository.model.Page;
import net.far.repository.model.SchemaNotFoundException;
import net.far.repository.model.policy.AccessPolicy;
import net.far.repository.model.policy.FieldPolicy;
import net.far.repository.model.schema.Revision;
import net.far.repository.model.schema.Schema;
import net.far.repository.spi.Store;
import net.far.resolver.model.Event;
import net.far.resolver.model.Integrity;
import net.far.resolver.model.Peer;
import net.far.resolver.model.Urn;
import net.far.resolver.model.query.Filter;
import net.far.resolver.model.query.Query;
import org.jdbi.v3.core.Jdbi;

/**
 * PostgreSQL implementation of the {@link Store} SPI using JDBI for SQL execution. Certificate
 * attributes are stored as JSONB and queried via {@code jsonb_extract_path_text()} for custom
 * attribute filtering. Schema versioning uses an append-only {@code schema_versions} table.
 */
@ApplicationScoped
public class DatabaseStore implements Store {

  private static final int MAX_DEPTH = 10;
  private static final Pattern SAFE_FIELD = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]{0,63}");
  private static final Set<String> COLUMNS =
      Set.of("identifier", "namespace", "urn", "status", "owner");
  private static final Set<String> SORTABLE =
      Set.of("identifier", "namespace", "urn", "status", "owner", "created", "modified");
  @Inject AgroalDataSource source;
  private Jdbi jdbi;

  private static String clause(final Filter filter, final HashMap<String, Object> bindings) {
    return clause(filter, bindings, 0);
  }

  private static String clause(
      final Filter filter, final HashMap<String, Object> bindings, final int depth) {
    if (depth > MAX_DEPTH) {
      throw new IllegalArgumentException("Filter exceeds maximum nesting depth of " + MAX_DEPTH);
    }
    return switch (filter) {
      case Filter.Comparison comparison -> comparison(comparison, bindings);
      case Filter.And and ->
          and.operands().stream()
              .map(operand -> clause(operand, bindings, depth + 1))
              .filter(clause -> !clause.isBlank())
              .collect(java.util.stream.Collectors.joining(" AND ", "(", ")"));
      case Filter.Or or ->
          or.operands().stream()
              .map(operand -> clause(operand, bindings, depth + 1))
              .filter(clause -> !clause.isBlank())
              .collect(java.util.stream.Collectors.joining(" OR ", "(", ")"));
    };
  }

  private static void validate(final String field) {
    if (!COLUMNS.contains(field) && !SAFE_FIELD.matcher(field).matches()) {
      throw new IllegalArgumentException("Invalid field name: " + field);
    }
  }

  private static String comparison(
      final Filter.Comparison comparison, final HashMap<String, Object> bindings) {
    final var field = comparison.field();
    validate(field);
    final var value = comparison.operand();
    final var key = "p" + bindings.size();

    if (comparison.operator() == net.far.resolver.model.query.Operator.CONTAINS
        && value instanceof String text) {
      final var pattern = "%" + text.replace("%", "\\%").replace("_", "\\_") + "%";
      bindings.put(key, pattern);
      return switch (field) {
        case "identifier" -> "(identifier ILIKE :" + key + " OR urn ILIKE :" + key + ")";
        case "namespace" -> "namespace ILIKE :" + key;
        case "urn" -> "urn ILIKE :" + key;
        case "status" -> "status ILIKE :" + key;
        case "owner" -> "owner ILIKE :" + key;
        default -> {
          final var attr = "a" + key.substring(1);
          bindings.put(attr, field);
          yield "jsonb_extract_path_text(attributes, :" + attr + ", 'value') ILIKE :" + key;
        }
      };
    }

    final var operator = comparison.operator();
    final var symbol =
        switch (operator) {
          case net.far.resolver.model.query.Operator.EQ -> "=";
          case net.far.resolver.model.query.Operator.NE -> "!=";
          case net.far.resolver.model.query.Operator.GT -> ">";
          case net.far.resolver.model.query.Operator.GE -> ">=";
          case net.far.resolver.model.query.Operator.LT -> "<";
          case net.far.resolver.model.query.Operator.LE -> "<=";
          default -> null;
        };

    if (symbol != null) {
      if (value instanceof Number) {
        bindings.put(key, value.toString());
      } else {
        bindings.put(key, value instanceof String ? value : String.valueOf(value));
      }
      return switch (field) {
        case "identifier" -> "identifier " + symbol + " :" + key;
        case "namespace" -> "namespace " + symbol + " :" + key;
        case "urn" -> "urn " + symbol + " :" + key;
        case "status" -> "status " + symbol + " :" + key;
        case "owner" -> "owner " + symbol + " :" + key;
        default -> {
          final var attr = "a" + key.substring(1);
          bindings.put(attr, field);
          yield value instanceof Number
              ? "COALESCE("
                  + "jsonb_extract_path_text(attributes, :"
                  + attr
                  + ", 'value', 'amount'), "
                  + "CASE WHEN jsonb_extract_path_text(attributes, :"
                  + attr
                  + ", 'value') ~ '^-?[0-9]+(\\.[0-9]+)?$' "
                  + "THEN jsonb_extract_path_text(attributes, :"
                  + attr
                  + ", 'value') ELSE NULL END"
                  + ")::numeric "
                  + symbol
                  + " CAST(:"
                  + key
                  + " AS numeric)"
              : "COALESCE("
                  + "jsonb_extract_path_text(attributes, :"
                  + attr
                  + ", 'value', 'amount'), "
                  + "jsonb_extract_path_text(attributes, :"
                  + attr
                  + ", 'value')"
                  + ") "
                  + symbol
                  + " :"
                  + key;
        }
      };
    }

    if (operator == net.far.resolver.model.query.Operator.IN
        && value instanceof java.util.List<?> values) {
      final var params = new ArrayList<String>();
      for (int i = 0; i < values.size(); i++) {
        final var param = key + "_" + i;
        bindings.put(
            param, values.get(i) instanceof String ? values.get(i) : String.valueOf(values.get(i)));
        params.add(":" + param);
      }
      final var list = String.join(", ", params);
      return switch (field) {
        case "identifier" -> "identifier IN (" + list + ")";
        case "namespace" -> "namespace IN (" + list + ")";
        case "urn" -> "urn IN (" + list + ")";
        case "status" -> "status IN (" + list + ")";
        case "owner" -> "owner IN (" + list + ")";
        default -> {
          final var attr = "a" + key.substring(1);
          bindings.put(attr, field);
          yield "jsonb_extract_path_text(attributes, :" + attr + ", 'value') IN (" + list + ")";
        }
      };
    }

    throw new UnsupportedOperationException("Unsupported filter operator: " + operator);
  }

  private static String orderby(final String expression) {
    if (expression == null || expression.isBlank()) {
      return "identifier ASC";
    }
    final var parts = expression.trim().split("\\s+", 2);
    final var field = parts[0];
    final var direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]) ? "DESC" : "ASC";
    if (SORTABLE.contains(field.toLowerCase())) {
      return field.toLowerCase() + " " + direction;
    }
    if (!SAFE_FIELD.matcher(field).matches()) {
      return "identifier ASC";
    }
    final var escaped = field.replace("'", "''");
    return "COALESCE("
        + "jsonb_extract_path_text(attributes, '"
        + escaped
        + "', 'value', 'amount'), "
        + "jsonb_extract_path_text(attributes, '"
        + escaped
        + "', 'value')"
        + ") "
        + direction;
  }

  private static String kind(final AccessPolicy policy) {
    return policy.label();
  }

  private static String role(final AccessPolicy policy) {
    return policy.role();
  }

  @PostConstruct
  void setup() {
    jdbi = Jdbi.create(source);
  }

  @Override
  public Certificate save(final Certificate certificate) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status,
    integrity_digest, integrity_algorithm, owner, schema_id, schema_version, created, modified)
VALUES (:id, :namespace, :identifier, :urn, CAST(:attributes AS jsonb), :status,
    :digest, :algorithm, :owner, CAST(:schema AS uuid), :pin, :created, :modified)
""")
                .bind("id", Generators.timeBasedEpochGenerator().generate())
                .bind("namespace", certificate.namespace())
                .bind("identifier", certificate.identifier())
                .bind("urn", certificate.urn().toString())
                .bind("attributes", Mapper.serialize(certificate.attributes()))
                .bind("status", certificate.status().name())
                .bind(
                    "digest",
                    certificate.integrity() != null ? certificate.integrity().digest() : null)
                .bind(
                    "algorithm",
                    certificate.integrity() != null ? certificate.integrity().algorithm() : null)
                .bind("owner", certificate.owner())
                .bind("schema", certificate.schema())
                .bind("pin", certificate.pin())
                .bind("created", certificate.created())
                .bind("modified", certificate.modified())
                .execute());
    return certificate;
  }

  @Override
  public Optional<Certificate> find(final Urn urn) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT * FROM certificates WHERE urn = :urn")
                .bind("urn", urn.toString())
                .map(Mapper::certificate)
                .findFirst());
  }

  @Override
  public boolean exists(final Urn urn) {
    return jdbi.withHandle(
        handle ->
            handle
                    .createQuery("SELECT count(*) FROM certificates WHERE urn = :urn")
                    .bind("urn", urn.toString())
                    .mapTo(Long.class)
                    .one()
                > 0);
  }

  @Override
  public Certificate update(final Urn urn, final Amendment amendment) {
    return jdbi.inTransaction(
        handle -> {
          final var existing =
              handle
                  .createQuery("SELECT * FROM certificates WHERE urn = :urn")
                  .bind("urn", urn.toString())
                  .map(Mapper::certificate)
                  .findFirst()
                  .orElseThrow(() -> new CertificateNotFoundException(urn.toString()));

          final var attributes =
              amendment.attributes() != null && !amendment.attributes().isEmpty()
                  ? merge(existing, amendment)
                  : existing.attributes();
          final var status = amendment.status() != null ? amendment.status() : existing.status();
          final var now = Instant.now();

          final var affected =
              handle
                  .createUpdate(
                      """
                      UPDATE certificates SET attributes = CAST(:attributes AS jsonb),
                          status = :status, modified = :modified, version = version + 1
                      WHERE urn = :urn AND version = :version
                      """)
                  .bind("attributes", Mapper.serialize(attributes))
                  .bind("status", status.name())
                  .bind("modified", now)
                  .bind("urn", urn.toString())
                  .bind("version", existing.version())
                  .execute();

          if (affected == 0) {
            throw new ConcurrentModificationException(
                "Certificate was modified concurrently: " + urn);
          }

          return new Certificate(
              existing.urn(),
              existing.namespace(),
              existing.identifier(),
              attributes,
              status,
              existing.integrity(),
              existing.owner(),
              existing.schema(),
              existing.pin(),
              existing.version() + 1,
              existing.created(),
              now);
        });
  }

  @Override
  public Certificate integrity(final Urn urn, final Integrity integrity) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "UPDATE certificates SET integrity_digest = :digest, integrity_algorithm ="
                        + " :algorithm WHERE urn = :urn")
                .bind("digest", integrity.digest())
                .bind("algorithm", integrity.algorithm())
                .bind("urn", urn.toString())
                .execute());
    return find(urn).orElseThrow(() -> new CertificateNotFoundException(urn.toString()));
  }

  @Override
  public Certificate update(final Urn urn, final Amendment amendment, final Integrity integrity) {
    return jdbi.inTransaction(
        handle -> {
          final var existing =
              handle
                  .createQuery("SELECT * FROM certificates WHERE urn = :urn")
                  .bind("urn", urn.toString())
                  .map(Mapper::certificate)
                  .findFirst()
                  .orElseThrow(() -> new CertificateNotFoundException(urn.toString()));

          final var attributes =
              amendment.attributes() != null && !amendment.attributes().isEmpty()
                  ? merge(existing, amendment)
                  : existing.attributes();
          final var status = amendment.status() != null ? amendment.status() : existing.status();
          final var now = Instant.now();

          final var affected =
              handle
                  .createUpdate(
                      """
UPDATE certificates SET attributes = CAST(:attributes AS jsonb),
    status = :status, integrity_digest = :digest, integrity_algorithm = :algorithm,
    modified = :modified, version = version + 1
WHERE urn = :urn AND version = :version
""")
                  .bind("attributes", Mapper.serialize(attributes))
                  .bind("status", status.name())
                  .bind("digest", integrity.digest())
                  .bind("algorithm", integrity.algorithm())
                  .bind("modified", now)
                  .bind("urn", urn.toString())
                  .bind("version", existing.version())
                  .execute();

          if (affected == 0) {
            throw new ConcurrentModificationException(
                "Certificate was modified concurrently: " + urn);
          }

          return new Certificate(
              existing.urn(),
              existing.namespace(),
              existing.identifier(),
              attributes,
              status,
              integrity,
              existing.owner(),
              existing.schema(),
              existing.pin(),
              existing.version() + 1,
              existing.created(),
              now);
        });
  }

  @Override
  public Optional<String> owner(final String document) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    """
                    SELECT c.owner FROM certificates c
                    JOIN certificate_documents cd ON c.id = cd.certificate_id
                    WHERE cd.document_id = CAST(:id AS uuid)
                    LIMIT 1
                    """)
                .bind("id", document)
                .mapTo(String.class)
                .findFirst());
  }

  @Override
  public Page search(final Query query) {
    return jdbi.withHandle(
        handle -> {
          final var where = new StringBuilder();
          final var bindings = new HashMap<String, Object>();
          final var namespaced = query.namespaces() != null && !query.namespaces().isEmpty();

          if (namespaced) {
            where.append("namespace IN (<namespaces>)");
          }

          if (query.filter() != null) {
            final var clause = clause(query.filter(), bindings);
            if (!clause.isBlank()) {
              if (!where.isEmpty()) where.append(" AND ");
              where.append(clause);
            }
          }

          final var condition = where.isEmpty() ? "" : "WHERE " + where;
          final var order = orderby(query.orderby());

          final var sql = "SELECT count(*) FROM certificates " + condition;
          var count = handle.createQuery(sql).bindMap(bindings);
          if (namespaced) {
            count = count.bindList("namespaces", List.copyOf(query.namespaces()));
          }
          final var total = count.mapTo(Long.class).one();

          final var select =
              "SELECT * FROM certificates "
                  + condition
                  + " ORDER BY "
                  + order
                  + " LIMIT :top OFFSET :skip";
          var query2 =
              handle
                  .createQuery(select)
                  .bindMap(bindings)
                  .bind("top", query.top())
                  .bind("skip", query.skip());
          if (namespaced) {
            query2 = query2.bindList("namespaces", List.copyOf(query.namespaces()));
          }
          final var results = query2.map(Mapper::certificate).list();

          return new Page(results, total, query.skip(), query.top());
        });
  }

  @Override
  public List<Certificate> recent(final int limit) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT * FROM certificates ORDER BY modified DESC LIMIT :limit")
                .bind("limit", limit)
                .map(Mapper::certificate)
                .list());
  }

  @Override
  public Document save(final Document document, final byte[] content) {
    final var id = java.util.UUID.fromString(document.id());
    jdbi.useTransaction(
        handle -> {
          handle
              .createUpdate(
                  """
INSERT INTO documents (id, filename, media, size, digest, signature, uploader, uploaded)
VALUES (:id, :filename, :media, :size, :digest, :signature, :uploader, :uploaded)
""")
              .bind("id", id)
              .bind("filename", document.filename())
              .bind("media", document.media())
              .bind("size", document.size())
              .bind("digest", document.digest())
              .bind("signature", document.signature())
              .bind("uploader", document.uploader())
              .bind("uploaded", document.uploaded())
              .execute();

          handle
              .createUpdate(
                  """
                  INSERT INTO document_content (id, content)
                  VALUES (:id, :content)
                  """)
              .bind("id", id)
              .bind("content", content)
              .execute();
        });
    return document;
  }

  @Override
  public Optional<Document> document(final String id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT * FROM documents WHERE id = CAST(:id AS uuid)")
                .bind("id", id)
                .map(Mapper::document)
                .findFirst());
  }

  @Override
  public List<Document> documents() {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT * FROM documents ORDER BY uploaded DESC")
                .map(Mapper::document)
                .list());
  }

  @Override
  public Optional<byte[]> content(final String id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT content FROM document_content WHERE id = :id")
                .bind("id", java.util.UUID.fromString(id))
                .mapTo(byte[].class)
                .findFirst());
  }

  @Override
  public void attach(final Urn urn, final String document) {
    jdbi.useHandle(
        handle -> {
          final var id =
              handle
                  .createQuery("SELECT id FROM certificates WHERE urn = :urn")
                  .bind("urn", urn.toString())
                  .mapTo(java.util.UUID.class)
                  .findFirst()
                  .orElseThrow(() -> new CertificateNotFoundException(urn.toString()));
          handle
              .createUpdate(
                  """
                  INSERT INTO certificate_documents (certificate_id, document_id, attached)
                  VALUES (:certificate, CAST(:document AS uuid), now())
                  ON CONFLICT DO NOTHING
                  """)
              .bind("certificate", id)
              .bind("document", document)
              .execute();
        });
  }

  @Override
  public void detach(final Urn urn, final String document) {
    jdbi.useHandle(
        handle -> {
          final var id =
              handle
                  .createQuery("SELECT id FROM certificates WHERE urn = :urn")
                  .bind("urn", urn.toString())
                  .mapTo(java.util.UUID.class)
                  .findFirst()
                  .orElseThrow(() -> new CertificateNotFoundException(urn.toString()));
          handle
              .createUpdate(
                  """
                  DELETE FROM certificate_documents
                  WHERE certificate_id = :certificate AND document_id = CAST(:document AS uuid)
                  """)
              .bind("certificate", id)
              .bind("document", document)
              .execute();
        });
  }

  @Override
  public List<Document> documents(final Urn urn) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    """
                    SELECT d.* FROM documents d
                    JOIN certificate_documents cd ON d.id = cd.document_id
                    JOIN certificates c ON c.id = cd.certificate_id
                    WHERE c.urn = :urn
                    ORDER BY cd.attached DESC
                    """)
                .bind("urn", urn.toString())
                .map(Mapper::document)
                .list());
  }

  @Override
  public Schema save(final Schema schema) {
    jdbi.useTransaction(
        handle -> {
          handle
              .createUpdate(
                  """
INSERT INTO schemas (id, namespace, name, description, version, fields, active, owner, created, modified)
VALUES (CAST(:id AS uuid), :namespace, :name, :description, :version,
    CAST(:fields AS jsonb), :active, :owner, :created, :modified)
""")
              .bind("id", schema.id())
              .bind("namespace", schema.namespace())
              .bind("name", schema.name())
              .bind("description", schema.description())
              .bind("version", schema.version())
              .bind("fields", Mapper.serializeFields(schema.fields()))
              .bind("active", schema.active())
              .bind("owner", schema.owner())
              .bind("created", schema.created())
              .bind("modified", schema.modified())
              .execute();

          handle
              .createUpdate(
                  """
INSERT INTO schema_versions (schema_id, version, description, fields, active, created)
VALUES (CAST(:id AS uuid), :version, :description, CAST(:fields AS jsonb), :active, :created)
""")
              .bind("id", schema.id())
              .bind("version", schema.version())
              .bind("description", schema.description())
              .bind("fields", Mapper.serializeFields(schema.fields()))
              .bind("active", schema.active())
              .bind("created", schema.created())
              .execute();
        });
    return schema;
  }

  @Override
  public Optional<Schema> schema(final String id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT * FROM schemas WHERE id = CAST(:id AS uuid)")
                .bind("id", id)
                .map(Mapper::schema)
                .findFirst());
  }

  @Override
  public Optional<Schema> schema(final String id, final int version) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    """
                    SELECT s.id, s.namespace, s.name, v.description, v.version, v.fields,
                        v.active, s.owner, s.created, v.created AS modified
                    FROM schemas s
                    JOIN schema_versions v ON s.id = v.schema_id
                    WHERE s.id = CAST(:id AS uuid) AND v.version = :version
                    """)
                .bind("id", id)
                .bind("version", version)
                .map(Mapper::schema)
                .findFirst());
  }

  @Override
  public List<Schema> versions(final String id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    """
                    SELECT s.id, s.namespace, s.name, v.description, v.version, v.fields,
                        v.active, s.owner, s.created, v.created AS modified
                    FROM schemas s
                    JOIN schema_versions v ON s.id = v.schema_id
                    WHERE s.id = CAST(:id AS uuid)
                    ORDER BY v.version DESC
                    """)
                .bind("id", id)
                .map(Mapper::schema)
                .list());
  }

  @Override
  public Schema update(final String id, final Revision revision) {
    return jdbi.inTransaction(
        handle -> {
          final var existing =
              handle
                  .createQuery("SELECT * FROM schemas WHERE id = CAST(:id AS uuid)")
                  .bind("id", id)
                  .map(Mapper::schema)
                  .findFirst()
                  .orElseThrow(() -> new SchemaNotFoundException(id));

          final var description =
              revision.description() != null ? revision.description() : existing.description();
          final var fields = revision.fields() != null ? revision.fields() : existing.fields();
          final var active = revision.active() != null ? revision.active() : existing.active();
          final var version = existing.version() + 1;
          final var now = Instant.now();

          handle
              .createUpdate(
                  """
                  UPDATE schemas SET description = :description, fields = CAST(:fields AS jsonb),
                      active = :active, version = :version, modified = :modified
                  WHERE id = CAST(:id AS uuid)
                  """)
              .bind("description", description)
              .bind("fields", Mapper.serializeFields(fields))
              .bind("active", active)
              .bind("version", version)
              .bind("modified", now)
              .bind("id", id)
              .execute();

          handle
              .createUpdate(
                  """
INSERT INTO schema_versions (schema_id, version, description, fields, active, created)
VALUES (CAST(:id AS uuid), :version, :description, CAST(:fields AS jsonb), :active, :created)
""")
              .bind("id", id)
              .bind("version", version)
              .bind("description", description)
              .bind("fields", Mapper.serializeFields(fields))
              .bind("active", active)
              .bind("created", now)
              .execute();

          return new Schema(
              existing.id(),
              existing.namespace(),
              existing.name(),
              description,
              version,
              fields,
              active,
              existing.owner(),
              existing.created(),
              now);
        });
  }

  @Override
  public List<Schema> schemas(final String namespace) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT * FROM schemas WHERE namespace = :namespace ORDER BY name")
                .bind("namespace", namespace)
                .map(Mapper::schema)
                .list());
  }

  @Override
  public List<Schema> schemas() {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT * FROM schemas ORDER BY namespace, name")
                .map(Mapper::schema)
                .list());
  }

  @Override
  public List<FieldPolicy> policies(final String id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT * FROM field_policies WHERE schema_id = CAST(:id AS uuid)")
                .bind("id", id)
                .map(Mapper::policy)
                .list());
  }

  @Override
  public void policies(final String id, final List<FieldPolicy> policies) {
    jdbi.useTransaction(
        handle -> {
          handle
              .createUpdate("DELETE FROM field_policies WHERE schema_id = CAST(:id AS uuid)")
              .bind("id", id)
              .execute();
          for (final var policy : policies) {
            handle
                .createUpdate(
                    """
                    INSERT INTO field_policies (schema_id, field, kind, role)
                    VALUES (CAST(:id AS uuid), :field, :kind, :role)
                    """)
                .bind("id", id)
                .bind("field", policy.field())
                .bind("kind", kind(policy.policy()))
                .bind("role", role(policy.policy()))
                .execute();
          }
        });
  }

  @Override
  public Peer save(final Peer peer) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
INSERT INTO peers (identity, endpoint, namespaces, key, key_id, previous, seen, priority, enabled, base, depth, created)
VALUES (:identity, :endpoint, CAST(:namespaces AS jsonb), :key, :keyId, CAST(:previous AS jsonb), :seen, :priority, :enabled, :base, :depth, now())
ON CONFLICT (identity) DO UPDATE SET
    endpoint = :endpoint,
    namespaces = CAST(:namespaces AS jsonb),
    key = :key,
    key_id = :keyId,
    previous = CAST(:previous AS jsonb),
    seen = :seen,
    priority = :priority,
    enabled = :enabled,
    base = :base,
    depth = :depth
""")
                .bind("identity", peer.identity())
                .bind("endpoint", peer.endpoint())
                .bind("namespaces", Mapper.serializeNamespaces(peer.namespaces()))
                .bind("key", peer.key())
                .bind("keyId", peer.keyId())
                .bind("previous", Mapper.serializePrevious(peer.previous()))
                .bind("seen", peer.seen())
                .bind("priority", peer.priority())
                .bind("enabled", peer.enabled())
                .bind("base", peer.base())
                .bind("depth", peer.depth())
                .execute());
    return peer;
  }

  @Override
  public List<Peer> peers() {
    return jdbi.withHandle(
        handle ->
            handle.createQuery("SELECT * FROM peers ORDER BY created").map(Mapper::peer).list());
  }

  @Override
  public void remove(final String identity) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("DELETE FROM peers WHERE identity = :identity")
                .bind("identity", identity)
                .execute());
  }

  @Override
  public void record(final Urn urn, final Event event) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO certificate_history (urn, type, actor, details, created)
                    VALUES (:urn, :type, :actor, CAST(:details AS jsonb), :created)
                    """)
                .bind("urn", urn.toString())
                .bind("type", event.type().label())
                .bind("actor", event.actor())
                .bind("details", Mapper.serializeDetails(event.details()))
                .bind("created", event.timestamp())
                .execute());
  }

  @Override
  public List<Event> events(final Urn urn) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    """
                    SELECT type, created, actor, details FROM certificate_history
                    WHERE urn = :urn ORDER BY created ASC
                    """)
                .bind("urn", urn.toString())
                .map(
                    (rs, context) -> {
                      final var type = Event.EventType.parse(rs.getString("type"));
                      final var timestamp = rs.getTimestamp("created").toInstant();
                      final var actor = rs.getString("actor");
                      final var details = Mapper.deserializeDetails(rs.getString("details"));
                      return new Event(type, timestamp, actor, details);
                    })
                .list());
  }

  private java.util.Map<String, net.far.resolver.model.Attribute> merge(
      final Certificate existing, final Amendment amendment) {
    final var merged = new HashMap<>(existing.attributes());
    merged.putAll(amendment.attributes());
    return merged;
  }
}
