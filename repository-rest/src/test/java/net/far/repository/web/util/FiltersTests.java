package net.far.repository.web.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.far.resolver.model.query.Filter;
import net.far.resolver.model.query.InvalidFilterException;
import net.far.resolver.model.query.Operator;
import org.junit.jupiter.api.Test;

class FiltersTests {

  @Test
  void shouldParseSimpleComparison() {
    final var filter = Filters.parse("name eq 'Alice'");

    assertThat(filter).isInstanceOf(Filter.Comparison.class);
    final var comparison = (Filter.Comparison) filter;
    assertThat(comparison.field()).isEqualTo("name");
    assertThat(comparison.operator()).isEqualTo(Operator.EQ);
    assertThat(comparison.operand()).isEqualTo("Alice");
  }

  @Test
  void shouldParseAnd() {
    final var filter = Filters.parse("a eq 'x' and b eq 'y'");
    assertThat(filter).isInstanceOf(Filter.And.class);
  }

  @Test
  void shouldParseOr() {
    final var filter = Filters.parse("a eq 'x' or b eq 'y'");
    assertThat(filter).isInstanceOf(Filter.Or.class);
  }

  @Test
  void shouldParseContains() {
    final var filter = Filters.parse("contains(name, 'test')");
    assertThat(filter).isInstanceOf(Filter.Comparison.class);
    final var comparison = (Filter.Comparison) filter;
    assertThat(comparison.operator()).isEqualTo(Operator.CONTAINS);
  }

  @Test
  void shouldParseIn() {
    final var filter = Filters.parse("status in ('ACTIVE', 'DRAFT')");
    assertThat(filter).isInstanceOf(Filter.Comparison.class);
    final var comparison = (Filter.Comparison) filter;
    assertThat(comparison.operator()).isEqualTo(Operator.IN);
  }

  @Test
  void shouldParseNumeric() {
    final var filter = Filters.parse("amount gt 100");
    final var comparison = (Filter.Comparison) filter;
    assertThat(comparison.operator()).isEqualTo(Operator.GT);
    assertThat(comparison.operand()).isEqualTo(100L);
  }

  @Test
  void shouldExtractNamespaces() {
    final var filter = Filters.parse("namespace eq 'carbon' and status eq 'ACTIVE'");
    final var namespaces = Filters.namespaces(filter);
    assertThat(namespaces).containsExactly("carbon");
  }

  @Test
  void shouldNotExtractNamespacesFromOr() {
    final var filter = Filters.parse("namespace eq 'carbon' or namespace eq 'energy'");
    final var namespaces = Filters.namespaces(filter);
    assertThat(namespaces).isEmpty();
  }

  @Test
  void shouldStripNamespaces() {
    final var filter = Filters.parse("namespace eq 'carbon' and status eq 'ACTIVE'");
    final var stripped = Filters.strip(filter);
    assertThat(stripped).isInstanceOf(Filter.Comparison.class);
    final var comparison = (Filter.Comparison) stripped;
    assertThat(comparison.field()).isEqualTo("status");
  }

  @Test
  void shouldRejectBlank() {
    assertThatThrownBy(() -> Filters.parse("")).isInstanceOf(InvalidFilterException.class);
  }

  @Test
  void shouldRejectMalformed() {
    assertThatThrownBy(() -> Filters.parse("not a filter"))
        .isInstanceOf(InvalidFilterException.class);
  }

  @Test
  void shouldParseAllOperators() {
    for (final var op : new String[] {"eq", "ne", "gt", "ge", "lt", "le"}) {
      final var filter = Filters.parse("field " + op + " 'value'");
      assertThat(filter).isInstanceOf(Filter.Comparison.class);
    }
  }

  @Test
  void shouldParseParenthesized() {
    final var filter = Filters.parse("(a eq 'x') and (b eq 'y')");
    assertThat(filter).isInstanceOf(Filter.And.class);
  }

  @Test
  void shouldParseNamespaceIn() {
    final var filter = Filters.parse("namespace in ('carbon', 'energy')");
    final var namespaces = Filters.namespaces(filter);
    assertThat(namespaces).containsExactlyInAnyOrder("carbon", "energy");
  }
}
