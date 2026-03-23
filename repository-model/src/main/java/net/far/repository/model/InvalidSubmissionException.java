package net.far.repository.model;

public class InvalidSubmissionException extends RepositoryException {

  public InvalidSubmissionException(final String message) {
    super("INVALID_SUBMISSION", message);
  }
}
