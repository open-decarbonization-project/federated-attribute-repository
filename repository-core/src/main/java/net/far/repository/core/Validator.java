package net.far.repository.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.far.repository.model.schema.Field;
import net.far.repository.model.schema.Schema;
import net.far.repository.model.type.ListType;
import net.far.repository.model.type.QuantityType;
import net.far.repository.model.type.RecordType;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Value;

/**
 * Validates certificate attributes against a schema definition. Returns a list of human-readable
 * violation messages. Checks required-field presence and type compatibility (string, numeric,
 * boolean, datetime, quantity with unit matching, list, record).
 */
public final class Validator {

  private Validator() {}

  public static List<String> validate(
      final Map<String, Attribute> attributes, final Schema schema) {
    final var violations = new ArrayList<String>();
    final var defined = schema.fields().stream().map(Field::name).collect(Collectors.toSet());
    for (final var key : attributes.keySet()) {
      if (!defined.contains(key)) {
        violations.add("Unknown field: " + key);
      }
    }
    for (final var field : schema.fields()) {
      final var attribute = attributes.get(field.name());
      if (attribute == null) {
        if (field.required()) {
          violations.add("Required field missing: " + field.name());
        }
        continue;
      }
      if (attribute.value() == null) {
        if (field.required()) {
          violations.add("Required field has null value: " + field.name());
        }
        continue;
      }
      final var type = field.type();
      final var value = attribute.value();
      if (!type.matches(value)) {
        violations.add(
            "Field '"
                + field.name()
                + "' expects "
                + label(type)
                + " but got "
                + value.getClass().getSimpleName());
        continue;
      }
      if (type instanceof QuantityType(String unit) && value instanceof Value.Quantity actual) {
        if (unit != null && !unit.isBlank() && !unit.equals(actual.unit())) {
          violations.add(
              "Field '"
                  + field.name()
                  + "' expects unit '"
                  + unit
                  + "' but got '"
                  + actual.unit()
                  + "'");
        }
      }
      if (type instanceof RecordType(List<Field> fields1)
          && value instanceof Value.Record(Map<String, Value> fields)) {
        validate(fields, fields1, field.name(), violations);
      }
      if (type instanceof ListType(net.far.repository.model.type.Type element)
          && value instanceof Value.Arr(List<Value> raw)) {
        for (int i = 0; i < raw.size(); i++) {
          final var item = raw.get(i);
          if (!element.matches(item)) {
            violations.add(
                "Field '"
                    + field.name()
                    + "["
                    + i
                    + "]' expects "
                    + label(element)
                    + " but got "
                    + item.getClass().getSimpleName());
          }
        }
      }
    }
    return Collections.unmodifiableList(violations);
  }

  private static void validate(
      final Map<String, Value> fields,
      final List<Field> schema,
      final String prefix,
      final List<String> violations) {
    final var defined = schema.stream().map(Field::name).collect(Collectors.toSet());
    for (final var key : fields.keySet()) {
      if (!defined.contains(key)) {
        violations.add("Unknown field: " + prefix + "." + key);
      }
    }
    for (final var field : schema) {
      final var value = fields.get(field.name());
      if (value == null) {
        if (field.required()) {
          violations.add("Required field missing: " + prefix + "." + field.name());
        }
        continue;
      }
      if (!field.type().matches(value)) {
        violations.add(
            "Field '"
                + prefix
                + "."
                + field.name()
                + "' expects "
                + label(field.type())
                + " but got "
                + value.getClass().getSimpleName());
        continue;
      }
      if (field.type() instanceof RecordType(List<Field> fields2)
          && value instanceof Value.Record(Map<String, Value> fields1)) {
        validate(fields1, fields2, prefix + "." + field.name(), violations);
      }
      if (field.type() instanceof ListType(net.far.repository.model.type.Type element)
          && value instanceof Value.Arr(List<Value> raw)) {
        for (int i = 0; i < raw.size(); i++) {
          final var item = raw.get(i);
          if (!element.matches(item)) {
            violations.add(
                "Field '"
                    + prefix
                    + "."
                    + field.name()
                    + "["
                    + i
                    + "]' expects "
                    + label(element)
                    + " but got "
                    + item.getClass().getSimpleName());
          }
        }
      }
    }
  }

  private static String label(final net.far.repository.model.type.Type type) {
    return type.getClass().getSimpleName().replace("Type", "");
  }
}
