package net.far.repository.store.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.far.repository.model.Certificate;
import net.far.repository.model.Document;
import net.far.repository.model.Status;
import net.far.repository.model.policy.AccessPolicy;
import net.far.repository.model.policy.FieldPolicy;
import net.far.repository.model.schema.Field;
import net.far.repository.model.schema.Schema;
import net.far.resolver.json.ValueDeserializer;
import net.far.resolver.json.ValueSerializer;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Integrity;
import net.far.resolver.model.Peer;
import net.far.resolver.model.Urn;
import net.far.resolver.model.Value;

/**
 * JDBI row-to-model mappers and JSON serialization utilities for the database store. Uses a
 * dedicated Jackson {@link ObjectMapper} configured with FAR's custom {@link
 * ValueSerializer}/{@link ValueDeserializer} for attribute value round-tripping.
 */
final class Mapper {

  static final ObjectMapper JSON = configure();
  private static final TypeReference<Map<String, Attribute>> ATTRIBUTES_TYPE =
      new TypeReference<>() {};
  private static final TypeReference<List<Field>> FIELDS_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
  private static final TypeReference<Map<String, Object>> DETAILS_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<Map<String, String>>> KEY_LIST_TYPE =
      new TypeReference<>() {};

  private Mapper() {}

  private static ObjectMapper configure() {
    final var module = new SimpleModule("far");
    module.addSerializer(Value.class, new ValueSerializer());
    module.addDeserializer(Value.class, new ValueDeserializer());
    return new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule())
        .registerModule(module);
  }

  static Certificate certificate(
      final ResultSet rs, final org.jdbi.v3.core.statement.StatementContext context)
      throws SQLException {
    final var urn = Urn.parse(rs.getString("urn"));
    final var attributes = deserialize(rs.getString("attributes"));
    final var digest = rs.getString("integrity_digest");
    final var algorithm = rs.getString("integrity_algorithm");
    final var integrity = digest != null ? new Integrity(digest, algorithm) : null;
    final var schema =
        rs.getObject("schema_id") != null
            ? rs.getObject("schema_id", java.util.UUID.class).toString()
            : null;
    final var pin = rs.getObject("schema_version") != null ? rs.getInt("schema_version") : null;
    final var version = rs.getInt("version");
    return new Certificate(
        urn,
        rs.getString("namespace"),
        rs.getString("identifier"),
        attributes,
        Status.valueOf(rs.getString("status")),
        integrity,
        rs.getString("owner"),
        schema,
        pin,
        version,
        rs.getTimestamp("created").toInstant(),
        rs.getTimestamp("modified").toInstant());
  }

  static Document document(
      final ResultSet rs, final org.jdbi.v3.core.statement.StatementContext context)
      throws SQLException {
    return new Document(
        rs.getObject("id", java.util.UUID.class).toString(),
        rs.getString("filename"),
        rs.getString("media"),
        rs.getLong("size"),
        rs.getString("digest"),
        rs.getString("signature"),
        rs.getString("uploader"),
        rs.getTimestamp("uploaded").toInstant());
  }

  static Schema schema(
      final ResultSet rs, final org.jdbi.v3.core.statement.StatementContext context)
      throws SQLException {
    final var fields = deserializeFields(rs.getString("fields"));
    return new Schema(
        rs.getObject("id", java.util.UUID.class).toString(),
        rs.getString("namespace"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getInt("version"),
        fields,
        rs.getBoolean("active"),
        rs.getString("owner"),
        rs.getTimestamp("created").toInstant(),
        rs.getTimestamp("modified").toInstant());
  }

  static FieldPolicy policy(
      final ResultSet rs, final org.jdbi.v3.core.statement.StatementContext context)
      throws SQLException {
    final var field = rs.getString("field");
    final var kind = rs.getString("kind");
    final var role = rs.getString("role");
    final var value = AccessPolicy.of(kind, role);
    return new FieldPolicy(field, value);
  }

  static String serialize(final Map<String, Attribute> attributes) {
    try {
      return JSON.writeValueAsString(attributes != null ? attributes : Map.of());
    } catch (final JsonProcessingException exception) {
      throw new RuntimeException("Failed to serialize attributes", exception);
    }
  }

  static String serializeFields(final List<Field> fields) {
    try {
      return JSON.writeValueAsString(fields != null ? fields : List.of());
    } catch (final JsonProcessingException exception) {
      throw new RuntimeException("Failed to serialize fields", exception);
    }
  }

  private static Map<String, Attribute> deserialize(final String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return JSON.readValue(json, ATTRIBUTES_TYPE);
    } catch (final JsonProcessingException exception) {
      throw new RuntimeException("Failed to deserialize attributes", exception);
    }
  }

  private static List<Field> deserializeFields(final String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return JSON.readValue(json, FIELDS_TYPE);
    } catch (final JsonProcessingException exception) {
      throw new RuntimeException("Failed to deserialize fields", exception);
    }
  }

  static Peer peer(final ResultSet rs, final org.jdbi.v3.core.statement.StatementContext context)
      throws SQLException {
    final var namespaces = deserializeNamespaces(rs.getString("namespaces"));
    final var seen = rs.getTimestamp("seen");
    final var key = rs.getString("key");
    final var previous = deserializePrevious(rs.getString("previous"));
    final var priority = rs.getInt("priority");
    final var enabled = rs.getBoolean("enabled");
    final var base = rs.getString("base");
    final var depth = rs.getInt("depth");
    final var keyId = rs.getString("key_id");
    return new Peer(
        rs.getString("identity"),
        rs.getString("endpoint"),
        namespaces,
        key,
        keyId,
        previous,
        seen != null ? seen.toInstant() : null,
        priority,
        enabled,
        base,
        depth);
  }

  static String serializeNamespaces(final Set<String> namespaces) {
    try {
      return JSON.writeValueAsString(namespaces != null ? namespaces : Set.of());
    } catch (final JsonProcessingException exception) {
      throw new RuntimeException("Failed to serialize namespaces", exception);
    }
  }

  private static Set<String> deserializeNamespaces(final String json) {
    if (json == null || json.isBlank()) {
      return Set.of();
    }
    try {
      return new LinkedHashSet<>(JSON.readValue(json, STRING_LIST_TYPE));
    } catch (final JsonProcessingException exception) {
      throw new RuntimeException("Failed to deserialize namespaces", exception);
    }
  }

  static String serializePrevious(final List<Peer.PeerKey> previous) {
    if (previous == null || previous.isEmpty()) {
      return "[]";
    }
    try {
      final var entries = new ArrayList<Map<String, String>>();
      for (final var key : previous) {
        final var entry = new java.util.LinkedHashMap<String, String>();
        entry.put("id", key.id());
        entry.put("key", key.key());
        if (key.expires() != null) {
          entry.put("expires", key.expires().toString());
        }
        entries.add(entry);
      }
      return JSON.writeValueAsString(entries);
    } catch (final JsonProcessingException exception) {
      throw new RuntimeException("Failed to serialize previous keys", exception);
    }
  }

  static String serializeDetails(final Map<String, Object> details) {
    if (details == null || details.isEmpty()) {
      return null;
    }
    try {
      return JSON.writeValueAsString(details);
    } catch (final JsonProcessingException exception) {
      throw new RuntimeException("Failed to serialize event details", exception);
    }
  }

  static Map<String, Object> deserializeDetails(final String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return JSON.readValue(json, DETAILS_TYPE);
    } catch (final JsonProcessingException exception) {
      throw new RuntimeException("Failed to deserialize event details", exception);
    }
  }

  private static List<Peer.PeerKey> deserializePrevious(final String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      final var entries = JSON.readValue(json, KEY_LIST_TYPE);
      final var result = new ArrayList<Peer.PeerKey>();
      for (final var entry : entries) {
        final var expires =
            entry.get("expires") != null ? Instant.parse(entry.get("expires")) : null;
        result.add(new Peer.PeerKey(entry.get("id"), entry.get("key"), expires));
      }
      return result;
    } catch (final JsonProcessingException exception) {
      throw new RuntimeException("Failed to deserialize previous keys", exception);
    }
  }
}
