package net.far.repository.model.schema;

import java.util.List;

public record Revision(String description, List<Field> fields, Boolean active) {}
