package net.far.repository.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service registry for {@link Ledger} implementations, discovered via {@link
 * java.util.ServiceLoader} at construction time. The registry operates outside CDI so that ledger
 * implementations can be loaded from any module on the classpath.
 */
public class LedgerRegistry {

  private final Map<String, Ledger> ledgers = new ConcurrentHashMap<>();

  public LedgerRegistry() {
    discover();
  }

  private void discover() {
    final var loader = ServiceLoader.load(Ledger.class);
    for (final var ledger : loader) {
      register(ledger);
    }
  }

  public void register(final Ledger ledger) {
    ledgers.put(ledger.name(), ledger);
  }

  public Optional<Ledger> find(final String name) {
    return Optional.ofNullable(ledgers.get(name));
  }

  public Collection<Ledger> all() {
    return Collections.unmodifiableCollection(ledgers.values());
  }
}
