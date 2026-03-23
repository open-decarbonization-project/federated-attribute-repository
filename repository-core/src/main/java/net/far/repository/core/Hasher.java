package net.far.repository.core;

import java.nio.charset.StandardCharsets;
import java.util.TreeMap;
import net.far.repository.model.Certificate;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Value;
import net.far.resolver.signature.DigestCalculator;

/**
 * Computes deterministic SHA-256 integrity digests for certificates. The digest covers the URN,
 * namespace, identifier, status, and all attributes in sorted key order, producing a stable hash
 * regardless of insertion order.
 */
public final class Hasher {

  private Hasher() {}

  public static String compute(final Certificate certificate) {
    final var canonical = canonical(certificate);
    return DigestCalculator.compute(canonical.getBytes(StandardCharsets.UTF_8));
  }

  private static String canonical(final Certificate certificate) {
    final var builder = new StringBuilder();
    builder.append(certificate.urn().toString()).append("\n");
    builder.append(certificate.status().name()).append("\n");
    final var sorted = new TreeMap<>(certificate.attributes());
    for (final var entry : sorted.entrySet()) {
      builder.append(entry.getKey()).append("=");
      builder.append(render(entry.getValue())).append("\n");
    }
    return builder.toString();
  }

  private static String render(final Attribute attribute) {
    if (attribute.value() == null) {
      return "";
    }
    return render(attribute.value());
  }

  private static String render(final Value value) {
    return switch (value) {
      case Value.Text text -> text.raw();
      case Value.Numeric numeric -> numeric.raw().toString();
      case Value.Bool bool -> String.valueOf(bool.raw());
      case Value.Temporal temporal -> temporal.raw().toString();
      case Value.Quantity quantity -> quantity.amount() + " " + quantity.unit();
      case Value.Arr arr ->
          arr.raw().stream()
              .map(Hasher::render)
              .reduce("", (final String a, final String b) -> a.isEmpty() ? b : a + "," + b);
      case Value.Record record ->
          new java.util.TreeMap<>(record.fields())
              .entrySet().stream()
                  .map(entry -> entry.getKey() + "=" + render(entry.getValue()))
                  .reduce("", (final String a, final String b) -> a.isEmpty() ? b : a + "," + b);
    };
  }
}
