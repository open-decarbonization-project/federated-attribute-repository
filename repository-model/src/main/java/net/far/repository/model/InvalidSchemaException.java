package net.far.repository.model;

public class InvalidSchemaException extends RepositoryException {

  public InvalidSchemaException(final String message) {
    super("INVALID_SCHEMA", message);
  }
}
