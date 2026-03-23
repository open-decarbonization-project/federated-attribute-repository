package net.far.repository.model.type;

import net.far.resolver.model.Value;

public record ListType(Type element) implements Structured {

  @Override
  public boolean matches(final Value value) {
    return value instanceof Value.Arr;
  }
}
