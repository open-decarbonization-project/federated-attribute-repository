package net.far.repository.model;

public class SchemaNotFoundException extends RepositoryException {

  public SchemaNotFoundException(final String id) {
    super("SCHEMA_NOT_FOUND", "Schema not found: " + id);
  }
}
