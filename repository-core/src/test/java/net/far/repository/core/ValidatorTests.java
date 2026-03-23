package net.far.repository.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import net.far.repository.model.schema.Field;
import net.far.repository.model.schema.Schema;
import net.far.repository.model.type.BooleanType;
import net.far.repository.model.type.DateTimeType;
import net.far.repository.model.type.NumericType;
import net.far.repository.model.type.QuantityType;
import net.far.repository.model.type.StringType;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Value;
import org.junit.jupiter.api.Test;

class ValidatorTests {

  private static Schema schema(final Field... fields) {
    return new Schema("id", "test", "schema", null, 1, List.of(fields), true, "owner", null, null);
  }

  private static Attribute attribute(final String name, final Value value) {
    return new Attribute(name, value, "test", true, Instant.now());
  }

  @Test
  void shouldPassValid() {
    final var target =
        schema(
            new Field("name", "Name", null, new StringType(), true, 0),
            new Field("count", "Count", null, new NumericType(), false, 1));
    final var attributes =
        Map.of(
            "name", attribute("name", Value.of("test")),
            "count", attribute("count", Value.of(42)));

    final var violations = Validator.validate(attributes, target);

    assertThat(violations).isEmpty();
  }

  @Test
  void shouldDetectMissing() {
    final var target = schema(new Field("name", "Name", null, new StringType(), true, 0));
    final Map<String, Attribute> attributes = Map.of();

    final var violations = Validator.validate(attributes, target);

    assertThat(violations).hasSize(1);
    assertThat(violations.get(0)).contains("Required field missing: name");
  }

  @Test
  void shouldAllowMissingOptional() {
    final var target = schema(new Field("name", "Name", null, new StringType(), false, 0));
    final Map<String, Attribute> attributes = Map.of();

    final var violations = Validator.validate(attributes, target);

    assertThat(violations).isEmpty();
  }

  @Test
  void shouldDetectTypeMismatch() {
    final var target = schema(new Field("name", "Name", null, new StringType(), true, 0));
    final var attributes = Map.of("name", attribute("name", Value.of(42)));

    final var violations = Validator.validate(attributes, target);

    assertThat(violations).hasSize(1);
    assertThat(violations.get(0)).contains("expects String");
  }

  @Test
  void shouldDetectQuantityUnitMismatch() {
    final var target =
        schema(new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 0));
    final var attributes = Map.of("volume", attribute("volume", Value.of(1000, "kg")));

    final var violations = Validator.validate(attributes, target);

    assertThat(violations).hasSize(1);
    assertThat(violations.get(0)).contains("expects unit 'tCO2e'");
  }

  @Test
  void shouldPassQuantityWithMatchingUnit() {
    final var target =
        schema(new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 0));
    final var attributes = Map.of("volume", attribute("volume", Value.of(1000, "tCO2e")));

    final var violations = Validator.validate(attributes, target);

    assertThat(violations).isEmpty();
  }

  @Test
  void shouldPassQuantityWithNoUnitConstraint() {
    final var target = schema(new Field("volume", "Volume", null, new QuantityType(null), true, 0));
    final var attributes = Map.of("volume", attribute("volume", Value.of(1000, "kg")));

    final var violations = Validator.validate(attributes, target);

    assertThat(violations).isEmpty();
  }

  @Test
  void shouldValidateBool() {
    final var target = schema(new Field("verified", "Verified", null, new BooleanType(), true, 0));
    final var attributes = Map.of("verified", attribute("verified", Value.of(true)));

    final var violations = Validator.validate(attributes, target);

    assertThat(violations).isEmpty();
  }

  @Test
  void shouldValidateTemporal() {
    final var target = schema(new Field("issued", "Issued", null, new DateTimeType(), true, 0));
    final var attributes = Map.of("issued", attribute("issued", Value.of(Instant.now())));

    final var violations = Validator.validate(attributes, target);

    assertThat(violations).isEmpty();
  }

  @Test
  void shouldCollectMultipleViolations() {
    final var target =
        schema(
            new Field("name", "Name", null, new StringType(), true, 0),
            new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 1));
    final Map<String, Attribute> attributes = Map.of();

    final var violations = Validator.validate(attributes, target);

    assertThat(violations).hasSize(2);
  }
}
