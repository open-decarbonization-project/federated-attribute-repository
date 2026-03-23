package net.far.repository.model;

import java.util.List;

/** Paginated certificate search result with total count and offset. */
public record Page(List<Certificate> value, long count, int skip, int top) {

  public Page {
    if (value == null) {
      value = List.of();
    } else {
      value = List.copyOf(value);
    }
    if (skip < 0) {
      skip = 0;
    }
    if (top <= 0) {
      top = 25;
    }
  }
}
