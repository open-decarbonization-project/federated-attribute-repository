package net.far.repository.model.schema;

import java.time.Instant;
import java.util.List;

/**
 * A versioned schema defining the expected attribute structure for certificates within a namespace.
 * Each revision increments the version number. Schemas can be active or inactive and carry
 * field-level access policies.
 */
public record Schema(
    String id,
    String namespace,
    String name,
    String description,
    int version,
    List<Field> fields,
    boolean active,
    String owner,
    Instant created,
    Instant modified) {

  public Schema {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Schema id must not be blank");
    }
    if (namespace == null || namespace.isBlank()) {
      throw new IllegalArgumentException("Schema namespace must not be blank");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Schema name must not be blank");
    }
    if (fields == null) {
      fields = List.of();
    } else {
      fields = List.copyOf(fields);
    }
    if (created == null) {
      created = Instant.now();
    }
    if (modified == null) {
      modified = created;
    }
  }
}
