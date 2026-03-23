package net.far.repository.model;

public class DocumentNotFoundException extends RepositoryException {

  public DocumentNotFoundException(final String id) {
    super("DOCUMENT_NOT_FOUND", "Document not found: " + id);
  }
}
