package net.far.repository.model.policy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Field-level access policy for schema-governed certificate attributes. Public fields are visible
 * to all; masked fields show {@code ***}; credential fields are visible only to holders of a
 * specific role.
 */
public record AccessPolicy(Kind kind, String role) {

  public static final AccessPolicy PUBLIC = new AccessPolicy(Kind.PUBLIC, null);
  public static final AccessPolicy MASKED = new AccessPolicy(Kind.MASKED, null);

  public static AccessPolicy credential(final String role) {
    return new AccessPolicy(Kind.CREDENTIAL, role);
  }

  @JsonCreator
  public static AccessPolicy of(
      @JsonProperty("kind") final String kind, @JsonProperty("role") final String role) {
    return switch (kind) {
      case "masked" -> MASKED;
      case "credential" -> credential(role);
      default -> PUBLIC;
    };
  }

  /** Returns the wire name for JSON/database serialization. */
  public String label() {
    return kind.name().toLowerCase();
  }

  public enum Kind {
    PUBLIC,
    MASKED,
    CREDENTIAL
  }
}
