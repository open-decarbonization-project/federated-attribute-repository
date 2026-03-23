package net.far.repository.model.schema;

import net.far.repository.model.type.Type;

public record Field(
    String name, String label, String description, Type type, boolean required, int position) {

  public Field {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Field name must not be blank");
    }
    if (type == null) {
      throw new IllegalArgumentException("Field type must not be null");
    }
  }
}
