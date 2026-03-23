package net.far.repository.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import net.far.repository.model.schema.Definition;
import net.far.repository.model.schema.Field;
import net.far.repository.model.schema.Revision;
import net.far.repository.model.schema.Schema;
import net.far.repository.model.type.BooleanType;
import net.far.repository.model.type.DateTimeType;
import net.far.repository.model.type.ListType;
import net.far.repository.model.type.NumericType;
import net.far.repository.model.type.Primitive;
import net.far.repository.model.type.QuantityType;
import net.far.repository.model.type.RecordType;
import net.far.repository.model.type.StringType;
import net.far.repository.model.type.Structured;
import net.far.repository.model.type.Type;
import net.far.resolver.model.Value;
import org.junit.jupiter.api.Test;

class SchemaTests {

  @Test
  void shouldCreateWithDefaults() {
    final var schema = new Schema("id", "test", "name", null, 1, null, true, "owner", null, null);

    assertThat(schema.namespace()).isEqualTo("test");
    assertThat(schema.name()).isEqualTo("name");
    assertThat(schema.fields()).isEmpty();
    assertThat(schema.created()).isNotNull();
    assertThat(schema.modified()).isEqualTo(schema.created());
  }

  @Test
  void shouldCreateWithFields() {
    final var field = new Field("volume", "Volume", "Amount", new QuantityType("tCO2e"), true, 0);
    final var schema =
        new Schema("id", "test", "name", "desc", 1, List.of(field), true, "owner", null, null);

    assertThat(schema.fields()).hasSize(1);
    assertThat(schema.fields().get(0).name()).isEqualTo("volume");
  }

  @Test
  void shouldRejectBlankNamespace() {
    assertThatThrownBy(() -> new Schema("id", "", "name", null, 1, null, true, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(() -> new Schema("id", "test", "", null, 1, null, true, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCreateField() {
    final var field =
        new Field("volume", "Volume", "Amount of CO2", new QuantityType("tCO2e"), true, 0);

    assertThat(field.name()).isEqualTo("volume");
    assertThat(field.type()).isInstanceOf(QuantityType.class);
    assertThat(field.required()).isTrue();
    assertThat(((QuantityType) field.type()).unit()).isEqualTo("tCO2e");
  }

  @Test
  void shouldRejectBlankFieldName() {
    assertThatThrownBy(() -> new Field("", "Label", null, new StringType(), false, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullFieldType() {
    assertThatThrownBy(() -> new Field("name", "Label", null, null, false, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCreateDefinition() {
    final var definition = new Definition("test", "name", "desc", null, "owner");

    assertThat(definition.namespace()).isEqualTo("test");
    assertThat(definition.name()).isEqualTo("name");
    assertThat(definition.fields()).isEmpty();
  }

  @Test
  void shouldRejectBlankDefinitionNamespace() {
    assertThatThrownBy(() -> new Definition("", "name", null, null, null))
        .isInstanceOf(InvalidSchemaException.class);
  }

  @Test
  void shouldRejectBlankDefinitionName() {
    assertThatThrownBy(() -> new Definition("test", "", null, null, null))
        .isInstanceOf(InvalidSchemaException.class);
  }

  @Test
  void shouldCreateRevision() {
    final var revision = new Revision("updated", List.of(), true);

    assertThat(revision.description()).isEqualTo("updated");
    assertThat(revision.active()).isTrue();
  }

  @Test
  void shouldMatchPrimitiveTypes() {
    assertThat(new StringType().matches(Value.of("hello"))).isTrue();
    assertThat(new StringType().matches(Value.of(42))).isFalse();
    assertThat(new NumericType().matches(Value.of(42))).isTrue();
    assertThat(new NumericType().matches(Value.of("hello"))).isFalse();
    assertThat(new BooleanType().matches(Value.of(true))).isTrue();
    assertThat(new BooleanType().matches(Value.of("hello"))).isFalse();
    assertThat(new DateTimeType().matches(Value.of(java.time.Instant.now()))).isTrue();
    assertThat(new DateTimeType().matches(Value.of(42))).isFalse();
  }

  @Test
  void shouldMatchStructuredTypes() {
    assertThat(new QuantityType("kg").matches(Value.of(100, "kg"))).isTrue();
    assertThat(new QuantityType("kg").matches(Value.of("hello"))).isFalse();
    assertThat(new ListType(new StringType()).matches(Value.of(List.of(Value.of("a"))))).isTrue();
    assertThat(new ListType(new StringType()).matches(Value.of(42))).isFalse();
  }

  @Test
  void shouldMatchRecordType() {
    final var record =
        new RecordType(
            List.of(
                new Field("street", "Street", null, new StringType(), true, 0),
                new Field("zip", "Zip", null, new StringType(), true, 1)));
    assertThat(
            record.matches(
                Value.of(Map.of("street", Value.of("123 Main"), "zip", Value.of("90210")))))
        .isTrue();
    assertThat(record.matches(Value.of("hello"))).isFalse();
    assertThat(record.matches(Value.of(List.of(Value.of("a"))))).isFalse();
    assertThat(record).isInstanceOf(Structured.class);
    assertThat(record.fields()).hasSize(2);
  }

  @Test
  void shouldDefaultRecordFieldsToEmpty() {
    final var record = new RecordType(null);
    assertThat(record.fields()).isEmpty();
  }

  @Test
  void shouldClassifyTypeHierarchy() {
    assertThat(new StringType()).isInstanceOf(Primitive.class);
    assertThat(new NumericType()).isInstanceOf(Primitive.class);
    assertThat(new BooleanType()).isInstanceOf(Primitive.class);
    assertThat(new DateTimeType()).isInstanceOf(Primitive.class);
    assertThat(new QuantityType("kg")).isInstanceOf(Structured.class);
    assertThat(new ListType(new StringType())).isInstanceOf(Structured.class);
    assertThat(new RecordType(List.of())).isInstanceOf(Structured.class);

    assertThat(new StringType()).isInstanceOf(Type.class);
    assertThat(new QuantityType("kg")).isInstanceOf(Type.class);
    assertThat(new RecordType(List.of())).isInstanceOf(Type.class);
  }
}
