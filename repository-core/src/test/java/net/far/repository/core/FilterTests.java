package net.far.repository.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.far.repository.model.policy.AccessPolicy;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Value;
import org.junit.jupiter.api.Test;

class FilterTests {

  private static Attribute attribute(final String name, final Value value) {
    return new Attribute(name, value, "test", true, Instant.now());
  }

  @Test
  void shouldPassPublicFields() {
    final var policies = Map.<String, AccessPolicy>of("name", AccessPolicy.PUBLIC);
    final var attributes = Map.of("name", attribute("name", Value.of("Alice")));

    final var filtered = Filter.apply(attributes, policies, Set.of());

    assertThat(filtered).containsKey("name");
    assertThat(filtered.get("name").value()).isEqualTo(Value.of("Alice"));
  }

  @Test
  void shouldMaskMaskedFields() {
    final var policies = Map.<String, AccessPolicy>of("secret", AccessPolicy.MASKED);
    final var attributes = Map.of("secret", attribute("secret", Value.of("sensitive")));

    final var filtered = Filter.apply(attributes, policies, Set.of());

    assertThat(filtered).containsKey("secret");
    assertThat(filtered.get("secret").value()).isEqualTo(Value.of("***"));
  }

  @Test
  void shouldOmitCredentialFieldsWithoutRole() {
    final var policies = Map.<String, AccessPolicy>of("ssn", AccessPolicy.credential("auditor"));
    final var attributes = Map.of("ssn", attribute("ssn", Value.of("123-45-6789")));

    final var filtered = Filter.apply(attributes, policies, Set.of("user"));

    assertThat(filtered).doesNotContainKey("ssn");
  }

  @Test
  void shouldShowCredentialFieldsWithRole() {
    final var policies = Map.<String, AccessPolicy>of("ssn", AccessPolicy.credential("auditor"));
    final var attributes = Map.of("ssn", attribute("ssn", Value.of("123-45-6789")));

    final var filtered = Filter.apply(attributes, policies, Set.of("auditor"));

    assertThat(filtered).containsKey("ssn");
    assertThat(filtered.get("ssn").value()).isEqualTo(Value.of("123-45-6789"));
  }

  @Test
  void shouldBypassAllPoliciesForAdmin() {
    final var policies = new HashMap<String, AccessPolicy>();
    policies.put("name", AccessPolicy.PUBLIC);
    policies.put("secret", AccessPolicy.MASKED);
    policies.put("ssn", AccessPolicy.credential("auditor"));
    final var attributes = new LinkedHashMap<String, Attribute>();
    attributes.put("name", attribute("name", Value.of("Alice")));
    attributes.put("secret", attribute("secret", Value.of("sensitive")));
    attributes.put("ssn", attribute("ssn", Value.of("123-45-6789")));

    final var filtered = Filter.apply(attributes, policies, Set.of("repository-admin"));

    assertThat(filtered).hasSize(3);
    assertThat(filtered.get("secret").value()).isEqualTo(Value.of("sensitive"));
    assertThat(filtered.get("ssn").value()).isEqualTo(Value.of("123-45-6789"));
  }

  @Test
  void shouldDefaultUnknownFieldsToMaskedWhenPoliciesExist() {
    final var policies = Map.<String, AccessPolicy>of("name", AccessPolicy.PUBLIC);
    final var attributes =
        Map.of(
            "name", attribute("name", Value.of("Alice")),
            "extra", attribute("extra", Value.of("bonus")));

    final var filtered = Filter.apply(attributes, policies, Set.of());

    assertThat(filtered).containsKey("name");
    assertThat(filtered).containsKey("extra");
    assertThat(filtered.get("extra").value()).isEqualTo(Value.of("***"));
  }

  @Test
  void shouldDefaultAllFieldsToPublicWhenNoPolicies() {
    final var attributes =
        Map.of(
            "name", attribute("name", Value.of("Alice")),
            "extra", attribute("extra", Value.of("bonus")));

    final var filtered = Filter.apply(attributes, Map.of(), Set.of());

    assertThat(filtered).containsKey("name");
    assertThat(filtered.get("name").value()).isEqualTo(Value.of("Alice"));
    assertThat(filtered).containsKey("extra");
    assertThat(filtered.get("extra").value()).isEqualTo(Value.of("bonus"));
  }

  @Test
  void shouldFilterMixedPolicies() {
    final var policies = new HashMap<String, AccessPolicy>();
    policies.put("name", AccessPolicy.PUBLIC);
    policies.put("age", AccessPolicy.MASKED);
    policies.put("ssn", AccessPolicy.credential("auditor"));
    final var attributes = new LinkedHashMap<String, Attribute>();
    attributes.put("name", attribute("name", Value.of("Alice")));
    attributes.put("age", attribute("age", Value.of(30)));
    attributes.put("ssn", attribute("ssn", Value.of("123-45-6789")));

    final var filtered = Filter.apply(attributes, policies, Set.of("user"));

    assertThat(filtered).hasSize(2);
    assertThat(filtered).containsKey("name");
    assertThat(filtered).containsKey("age");
    assertThat(filtered).doesNotContainKey("ssn");
    assertThat(filtered.get("age").value()).isEqualTo(Value.of("***"));
  }
}
