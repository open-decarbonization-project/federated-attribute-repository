package net.far.repository.model;

public class RepositoryException extends RuntimeException {

  private final String code;

  public RepositoryException(final String code, final String message) {
    super(message);
    this.code = code;
  }

  public RepositoryException(final String code, final String message, final Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
