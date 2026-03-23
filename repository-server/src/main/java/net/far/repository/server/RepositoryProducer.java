package net.far.repository.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import net.far.repository.core.Binder;
import net.far.repository.core.Repository;
import net.far.repository.driver.RepositoryDriver;
import net.far.repository.spi.LedgerRegistry;
import net.far.repository.spi.Store;
import net.far.resolver.client.HttpFarClient;
import net.far.resolver.core.Delegator;
import net.far.resolver.core.PeerRegistry;
import net.far.resolver.core.Resolver;
import net.far.resolver.core.Router;
import net.far.resolver.signature.KeyManager;
import net.far.resolver.signature.KeyRing;
import net.far.resolver.signature.Signer;
import net.far.resolver.signature.Verifier;
import net.far.resolver.spi.DriverRegistry;
import net.far.resolver.spi.FarClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * CDI producer that wires all repository components: key management, signing, verification, the
 * repository orchestrator, document binder, federation resolver/delegator, and the FAR client.
 * Mirrors the resolver's {@code FarProducer} but adds repository-specific beans.
 */
@ApplicationScoped
public class RepositoryProducer {

  @ConfigProperty(name = "repository.identity", defaultValue = "https://repository.far.example.com")
  String identity;

  @ConfigProperty(name = "repository.namespaces", defaultValue = "default")
  String namespaces;

  @ConfigProperty(name = "repository.keys.id", defaultValue = "key-1")
  String keyId;

  @ConfigProperty(name = "repository.keys.directory", defaultValue = "")
  Optional<String> directory;

  @ConfigProperty(name = "repository.delegation.depth", defaultValue = "5")
  int depth;

  @ConfigProperty(name = "repository.keys.previous.directory", defaultValue = "")
  Optional<String> previous;

  @ConfigProperty(name = "repository.keys.previous.expires", defaultValue = "")
  Optional<String> expires;

  @Produces
  @Singleton
  public LedgerRegistry ledgers() {
    return new LedgerRegistry();
  }

  @Produces
  @Singleton
  public Repository repository(final Store store, final LedgerRegistry ledgers) {
    return new Repository(store, ledgers);
  }

  @Produces
  @Singleton
  public KeyManager keys() {
    final var id = identity + "#" + keyId;
    if (directory.isPresent() && !directory.get().isBlank()) {
      final var path = Path.of(directory.get());
      if (Files.exists(path.resolve("private.pem"))) {
        return KeyManager.load(path, id);
      }
      final var generated = KeyManager.generate(id);
      generated.write(path);
      return generated;
    }
    return KeyManager.generate(id);
  }

  @Produces
  @Singleton
  public KeyRing ring(final KeyManager keys) {
    return new KeyRing(keys, retired());
  }

  private java.util.List<KeyRing.Retired> retired() {
    if (previous.isEmpty() || previous.get().isBlank()) {
      return java.util.List.of();
    }
    final var path = Path.of(previous.get());
    if (!Files.exists(path)) {
      return java.util.List.of();
    }
    final var id = identity + "#" + keyId + "-previous";
    final var loaded = KeyManager.load(path, id);
    final var expiry =
        expires
            .filter(s -> !s.isBlank())
            .map(java.time.Instant::parse)
            .orElse(java.time.Instant.now().plusSeconds(86400));
    return java.util.List.of(new KeyRing.Retired(loaded, expiry));
  }

  @Produces
  @Singleton
  public Signer signer(final KeyRing ring) {
    return new Signer(ring.current());
  }

  @Produces
  @Singleton
  public Verifier verifier() {
    return new Verifier();
  }

  @Produces
  @Singleton
  public FarClient client(final Signer signer, final PeerRegistry peers, final Verifier verifier) {
    return new HttpFarClient(signer, peers, verifier, true);
  }

  @Produces
  @Singleton
  public Binder binder(final Store store, final Signer signer) {
    return new Binder(store, signer);
  }

  @Produces
  @Singleton
  public RepositoryDriver driver(final Repository repository) {
    final var supported = Set.of(namespaces.split(","));
    return new RepositoryDriver(repository, supported, identity);
  }

  @Produces
  @Singleton
  public DriverRegistry drivers(final RepositoryDriver driver) {
    final var drivers = new DriverRegistry();
    drivers.register(driver);
    return drivers;
  }

  @Produces
  @Singleton
  public PeerRegistry peers() {
    return new PeerRegistry();
  }

  @Produces
  @Singleton
  public Router router(final DriverRegistry repository) {
    return new Router(repository);
  }

  @Produces
  @Singleton
  public Delegator delegator(final PeerRegistry peers, final FarClient client) {
    return new Delegator(peers, client, identity, depth);
  }

  @Produces
  @Singleton
  public Resolver resolver(
      final Router router, final Delegator delegator, final PeerRegistry peers) {
    return new Resolver(router, delegator, peers);
  }
}
