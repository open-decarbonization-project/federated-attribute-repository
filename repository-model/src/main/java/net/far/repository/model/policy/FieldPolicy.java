package net.far.repository.model.policy;

/** Binds a field name to an access policy. */
public record FieldPolicy(String field, AccessPolicy policy) {

  public FieldPolicy {
    if (field == null || field.isBlank()) {
      throw new IllegalArgumentException("Field name must not be blank");
    }
    if (policy == null) {
      policy = AccessPolicy.PUBLIC;
    }
  }
}
