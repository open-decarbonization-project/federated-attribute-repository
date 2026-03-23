package net.far.repository.driver;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import net.far.repository.core.Repository;
import net.far.repository.model.Submission;
import net.far.repository.spi.LedgerRegistry;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Urn;
import net.far.resolver.model.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepositoryDriverTests {

  private Repository repository;
  private RepositoryDriver driver;

  @BeforeEach
  void setup() {
    final var store = new InMemoryStore();
    final var ledgers = new LedgerRegistry();
    repository = new Repository(store, ledgers);
    driver = new RepositoryDriver(repository, Set.of("test"), "https://test.far.example.com");
  }

  @Test
  void shouldReturnName() {
    assertThat(driver.name()).isEqualTo("repository");
  }

  @Test
  void shouldReturnNamespaces() {
    assertThat(driver.namespaces()).containsExactly("test");
  }

  @Test
  void shouldResolve() {
    final var submission =
        new Submission(
            "test",
            "CERT-001",
            Map.of(
                "volume",
                new Attribute("volume", Value.of(1000), "repository", true, Instant.now())),
            "owner",
            null,
            null);
    repository.create(submission);

    final var urn = new Urn("test", "CERT-001");
    final var resolution = driver.resolve(urn);

    assertThat(resolution).isPresent();
    assertThat(resolution.get().urn()).isEqualTo(urn);
    assertThat(resolution.get().attributes()).containsKey("volume");
  }

  @Test
  void shouldReturnEmptyForMissing() {
    final var urn = new Urn("test", "MISSING");
    final var resolution = driver.resolve(urn);

    assertThat(resolution).isEmpty();
  }

  @Test
  void shouldCheckExists() {
    final var submission = new Submission("test", "CERT-001", null, null, null, null);
    repository.create(submission);

    final var urn = new Urn("test", "CERT-001");
    assertThat(driver.exists(urn)).isTrue();
  }

  @Test
  void shouldReturnHistory() {
    final var submission = new Submission("test", "CERT-001", null, "owner", null, null);
    repository.create(submission);

    final var urn = new Urn("test", "CERT-001");
    final var history = driver.history(urn);

    assertThat(history).isPresent();
    assertThat(history.get().events()).hasSize(1);
  }
}
