package net.far.repository.model.type;

import net.far.resolver.model.Value;

public record BooleanType() implements Primitive {

  @Override
  public boolean matches(final Value value) {
    return value instanceof Value.Bool;
  }
}
