package net.far.repository.model;

import java.util.Map;
import net.far.resolver.model.Attribute;

/** Request payload for creating a new certificate. Namespace and identifier are required. */
public record Submission(
    String namespace,
    String identifier,
    Map<String, Attribute> attributes,
    String owner,
    String schema,
    Integer pin) {

  public Submission {
    if (namespace == null || namespace.isBlank()) {
      throw new InvalidSubmissionException("Namespace must not be blank");
    }
    if (identifier == null || identifier.isBlank()) {
      throw new InvalidSubmissionException("Identifier must not be blank");
    }
    if (attributes == null) {
      attributes = Map.of();
    }
  }
}
