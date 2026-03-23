package net.far.repository.model;

public class DuplicateCertificateException extends RepositoryException {

  public DuplicateCertificateException(final String urn) {
    super("DUPLICATE_CERTIFICATE", "Certificate already exists: " + urn);
  }
}
