package net.far.repository.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.far.repository.model.schema.Field;
import net.far.repository.model.schema.Schema;
import net.far.repository.model.type.BooleanType;
import net.far.repository.model.type.DateTimeType;
import net.far.repository.model.type.NumericType;
import net.far.repository.model.type.QuantityType;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Value;

/**
 * Coerces attribute values from their JSON-deserialized form to the types expected by the schema.
 * JSON has no date or quantity type, so values arrive as Value.Text and must be converted before
 * schema validation.
 */
public final class Coercer {

  private Coercer() {}

  public static Map<String, Attribute> coerce(
      final Map<String, Attribute> attributes, final Schema schema) {
    final var types = schema.fields().stream().collect(Collectors.toMap(Field::name, Field::type));
    final var result = new LinkedHashMap<String, Attribute>();
    for (final var entry : attributes.entrySet()) {
      final var name = entry.getKey();
      final var attribute = entry.getValue();
      final var type = types.get(name);
      if (type == null || attribute.value() == null) {
        result.put(name, attribute);
        continue;
      }
      final var coerced = coerce(attribute.value(), type);
      result.put(
          name,
          new Attribute(
              attribute.name(),
              coerced,
              attribute.source(),
              attribute.verified(),
              attribute.timestamp()));
    }
    return result;
  }

  private static Value coerce(final Value value, final net.far.repository.model.type.Type type) {
    if (!(value instanceof Value.Text text)) {
      return value;
    }
    final var raw = text.raw().trim();
    if (type instanceof DateTimeType) {
      return temporal(raw);
    }
    if (type instanceof NumericType) {
      return numeric(raw);
    }
    if (type instanceof BooleanType) {
      return Value.of(Boolean.parseBoolean(raw));
    }
    if (type instanceof QuantityType quantity) {
      final var number = numeric(raw);
      if (number instanceof Value.Numeric n && quantity.unit() != null) {
        return Value.of(n.raw(), quantity.unit());
      }
      return number;
    }
    return value;
  }

  private static Value temporal(final String raw) {
    try {
      return Value.of(Instant.parse(raw));
    } catch (final DateTimeParseException ignored) {
    }
    try {
      return Value.of(LocalDate.parse(raw).atStartOfDay(ZoneOffset.UTC).toInstant());
    } catch (final DateTimeParseException ignored) {
    }
    return Value.of(raw);
  }

  private static Value numeric(final String raw) {
    try {
      return Value.of(new BigDecimal(raw));
    } catch (final NumberFormatException ignored) {
      return Value.of(raw);
    }
  }
}
