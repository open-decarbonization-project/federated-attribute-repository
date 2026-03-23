package net.far.repository.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.far.repository.model.policy.AccessPolicy;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Value;

/**
 * Applies field-level access policies to certificate attributes. Policies are evaluated per-field:
 * public fields pass through, masked fields have their value replaced with {@code ***}, and
 * credential fields are omitted entirely unless the caller holds the required role. Admin users
 * bypass all policies.
 */
public final class Filter {

  private static final Value REDACTED = Value.of("***");

  private Filter() {}

  public static Map<String, Attribute> apply(
      final Map<String, Attribute> attributes,
      final Map<String, AccessPolicy> policies,
      final Set<String> roles) {
    if (roles.contains("repository-admin")) {
      return attributes;
    }

    final var result = new LinkedHashMap<String, Attribute>();
    for (final var entry : attributes.entrySet()) {
      final var policy =
          policies.getOrDefault(
              entry.getKey(), policies.isEmpty() ? AccessPolicy.PUBLIC : AccessPolicy.MASKED);
      switch (policy.kind()) {
        case PUBLIC -> result.put(entry.getKey(), entry.getValue());
        case MASKED -> {
          final var original = entry.getValue();
          result.put(
              entry.getKey(),
              new Attribute(
                  original.name(),
                  REDACTED,
                  original.source(),
                  original.verified(),
                  original.timestamp()));
        }
        case CREDENTIAL -> {
          if (roles.contains(policy.role())) {
            result.put(entry.getKey(), entry.getValue());
          }
        }
      }
    }
    return Map.copyOf(result);
  }
}
