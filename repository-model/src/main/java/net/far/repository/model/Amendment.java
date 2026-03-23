package net.far.repository.model;

import java.util.Map;
import net.far.resolver.model.Attribute;

/** Request payload for updating an existing certificate's attributes and/or status. */
public record Amendment(Map<String, Attribute> attributes, Status status) {

  public Amendment {
    if (attributes == null) {
      attributes = Map.of();
    }
  }
}
