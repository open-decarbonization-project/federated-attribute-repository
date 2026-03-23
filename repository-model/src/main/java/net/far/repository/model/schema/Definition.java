package net.far.repository.model.schema;

import java.util.List;
import net.far.repository.model.InvalidSchemaException;

/** Request payload for defining a new schema. Creates version 1. */
public record Definition(
    String namespace, String name, String description, List<Field> fields, String owner) {

  public Definition {
    if (namespace == null || namespace.isBlank()) {
      throw new InvalidSchemaException("Definition namespace must not be blank");
    }
    if (name == null || name.isBlank()) {
      throw new InvalidSchemaException("Definition name must not be blank");
    }
    if (fields == null) {
      fields = List.of();
    }
  }
}
