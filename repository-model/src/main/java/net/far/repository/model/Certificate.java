package net.far.repository.model;

import java.time.Instant;
import java.util.Map;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Integrity;
import net.far.resolver.model.Urn;

/**
 * An immutable certificate representing a verified claim about an entity's attributes. Certificates
 * are identified by a URN ({@code urn:far:{namespace}:{identifier}}), carry a map of typed
 * attributes, and progress through a status lifecycle (DRAFT → ACTIVE → SUSPENDED → RETIRED). Each
 * mutation produces a new integrity digest for tamper detection.
 */
public record Certificate(
    Urn urn,
    String namespace,
    String identifier,
    Map<String, Attribute> attributes,
    Status status,
    Integrity integrity,
    String owner,
    String schema,
    Integer pin,
    int version,
    Instant created,
    Instant modified) {

  public Certificate(
      final Urn urn,
      final String namespace,
      final String identifier,
      final Map<String, Attribute> attributes,
      final Status status,
      final Integrity integrity,
      final String owner,
      final String schema,
      final Integer pin,
      final Instant created,
      final Instant modified) {
    this(
        urn,
        namespace,
        identifier,
        attributes,
        status,
        integrity,
        owner,
        schema,
        pin,
        1,
        created,
        modified);
  }

  public Certificate {
    if (urn == null) {
      throw new IllegalArgumentException("URN must not be null");
    }
    if (namespace == null || namespace.isBlank()) {
      throw new IllegalArgumentException("Namespace must not be blank");
    }
    if (identifier == null || identifier.isBlank()) {
      throw new IllegalArgumentException("Identifier must not be blank");
    }
    if (attributes == null) {
      attributes = Map.of();
    }
    if (status == null) {
      status = Status.DRAFT;
    }
    if (created == null) {
      created = Instant.now();
    }
    if (modified == null) {
      modified = created;
    }
  }
}
