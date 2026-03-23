package net.far.repository.model;

public class CertificateNotFoundException extends RepositoryException {

  public CertificateNotFoundException(final String urn) {
    super("CERTIFICATE_NOT_FOUND", "Certificate not found: " + urn);
  }
}
