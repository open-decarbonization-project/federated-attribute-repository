package net.far.repository.model.type;

import java.util.List;
import net.far.repository.model.schema.Field;
import net.far.resolver.model.Value;

public record RecordType(List<Field> fields) implements Structured {

  public RecordType {
    if (fields == null) {
      fields = List.of();
    }
  }

  @Override
  public boolean matches(final Value value) {
    return value instanceof Value.Record;
  }
}
