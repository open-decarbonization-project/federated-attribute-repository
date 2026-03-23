package net.far.repository.model;

public class LedgerException extends RepositoryException {

  public LedgerException(final String message) {
    super("LEDGER_ERROR", message);
  }

  public LedgerException(final String message, final Throwable cause) {
    super("LEDGER_ERROR", message, cause);
  }
}
