package net.far.repository.model.type;

import net.far.resolver.model.Value;

public record QuantityType(String unit) implements Structured {

  @Override
  public boolean matches(final Value value) {
    return value instanceof Value.Quantity;
  }
}
