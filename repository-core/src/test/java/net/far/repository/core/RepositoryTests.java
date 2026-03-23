package net.far.repository.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import net.far.repository.model.*;
import net.far.repository.model.policy.*;
import net.far.repository.model.schema.*;
import net.far.repository.model.type.QuantityType;
import net.far.repository.model.type.StringType;
import net.far.repository.spi.LedgerRegistry;
import net.far.resolver.model.Attribute;
import net.far.resolver.model.Urn;
import net.far.resolver.model.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepositoryTests {

  private InMemoryStore store;
  private Repository repository;

  @BeforeEach
  void setup() {
    store = new InMemoryStore();
    final var ledgers = new LedgerRegistry();
    repository = new Repository(store, ledgers);
  }

  @Test
  void shouldCreate() {
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

    final var certificate = repository.create(submission);

    assertThat(certificate.urn().toString()).isEqualTo("urn:far:test:CERT-001");
    assertThat(certificate.status()).isEqualTo(Status.DRAFT);
    assertThat(certificate.integrity()).isNotNull();
  }

  @Test
  void shouldPersistOnCreate() {
    final var submission = new Submission("test", "CERT-002", null, "owner", null, null);

    repository.create(submission);

    final var urn = new Urn("test", "CERT-002");
    assertThat(repository.exists(urn)).isTrue();
  }

  @Test
  void shouldRejectDuplicate() {
    final var submission = new Submission("test", "CERT-001", null, null, null, null);
    repository.create(submission);

    assertThatThrownBy(() -> repository.create(submission))
        .isInstanceOf(DuplicateCertificateException.class);
  }

  @Test
  void shouldFind() {
    final var submission = new Submission("test", "CERT-001", null, "owner", null, null);
    repository.create(submission);

    final var urn = new Urn("test", "CERT-001");
    final var result = repository.find(urn);

    assertThat(result.urn()).isEqualTo(urn);
    assertThat(result.owner()).isEqualTo("owner");
  }

  @Test
  void shouldThrowOnMissing() {
    final var urn = new Urn("test", "MISSING");

    assertThatThrownBy(() -> repository.find(urn)).isInstanceOf(CertificateNotFoundException.class);
  }

  @Test
  void shouldCheckExists() {
    final var submission = new Submission("test", "CERT-001", null, null, null, null);
    repository.create(submission);

    final var urn = new Urn("test", "CERT-001");
    assertThat(repository.exists(urn)).isTrue();
    assertThat(repository.exists(new Urn("test", "MISSING"))).isFalse();
  }

  @Test
  void shouldRetire() {
    final var submission = new Submission("test", "CERT-001", null, null, null, null);
    repository.create(submission);

    final var urn = new Urn("test", "CERT-001");
    final var retired = repository.retire(urn);

    assertThat(retired.status()).isEqualTo(Status.RETIRED);
  }

  @Test
  void shouldDefineSchema() {
    final var definition =
        new Definition(
            "test",
            "carbon",
            "Carbon credit schema",
            List.of(new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 0)),
            "owner");

    final var schema = repository.define(definition);

    assertThat(schema.id()).isNotNull();
    assertThat(schema.namespace()).isEqualTo("test");
    assertThat(schema.name()).isEqualTo("carbon");
    assertThat(schema.version()).isEqualTo(1);
    assertThat(schema.fields()).hasSize(1);
  }

  @Test
  void shouldFindSchema() {
    final var definition = new Definition("test", "carbon", null, List.of(), "owner");
    final var created = repository.define(definition);

    final var found = repository.schema(created.id());

    assertThat(found.name()).isEqualTo("carbon");
  }

  @Test
  void shouldThrowOnMissingSchema() {
    assertThatThrownBy(() -> repository.schema("nonexistent"))
        .isInstanceOf(SchemaNotFoundException.class);
  }

  @Test
  void shouldReviseSchema() {
    final var definition = new Definition("test", "carbon", "original", List.of(), "owner");
    final var created = repository.define(definition);

    final var revision =
        new Revision(
            "updated",
            List.of(new Field("status", "Status", null, new StringType(), true, 0)),
            true);
    final var revised = repository.revise(created.id(), revision);

    assertThat(revised.version()).isEqualTo(2);
    assertThat(revised.description()).isEqualTo("updated");
    assertThat(revised.fields()).hasSize(1);
  }

  @Test
  void shouldValidateAgainstSchema() {
    final var definition =
        new Definition(
            "test",
            "carbon",
            null,
            List.of(new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 0)),
            "owner");
    final var schema = repository.define(definition);

    final var submission =
        new Submission(
            "test",
            "CERT-001",
            Map.of(
                "volume",
                new Attribute("volume", Value.of(1000, "tCO2e"), "test", true, Instant.now())),
            "owner",
            schema.id(),
            null);

    final var certificate = repository.create(submission);

    assertThat(certificate.schema()).isEqualTo(schema.id());
  }

  @Test
  void shouldRejectInvalidSubmissionForSchema() {
    final var definition =
        new Definition(
            "test",
            "carbon",
            null,
            List.of(new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 0)),
            "owner");
    final var schema = repository.define(definition);

    final var submission = new Submission("test", "CERT-001", Map.of(), "owner", schema.id(), null);

    assertThatThrownBy(() -> repository.create(submission))
        .isInstanceOf(InvalidSubmissionException.class)
        .hasMessageContaining("Schema validation failed");
  }

  @Test
  void shouldPinToLatestVersion() {
    final var definition =
        new Definition(
            "test",
            "carbon",
            null,
            List.of(new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 0)),
            "owner");
    final var schema = repository.define(definition);

    final var submission =
        new Submission(
            "test",
            "CERT-001",
            Map.of(
                "volume",
                new Attribute("volume", Value.of(1000, "tCO2e"), "test", true, Instant.now())),
            "owner",
            schema.id(),
            null);
    final var certificate = repository.create(submission);

    assertThat(certificate.pin()).isEqualTo(schema.version());
  }

  @Test
  void shouldPinToSpecificVersion() {
    final var definition =
        new Definition(
            "test",
            "carbon",
            null,
            List.of(new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 0)),
            "owner");
    final var schema = repository.define(definition);

    repository.revise(
        schema.id(),
        new Revision(
            "v2",
            List.of(
                new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 0),
                new Field("status", "Status", null, new StringType(), false, 1)),
            true));

    final var submission =
        new Submission(
            "test",
            "CERT-001",
            Map.of(
                "volume",
                new Attribute("volume", Value.of(1000, "tCO2e"), "test", true, Instant.now())),
            "owner",
            schema.id(),
            1);
    final var certificate = repository.create(submission);

    assertThat(certificate.pin()).isEqualTo(1);
  }

  @Test
  void shouldRecomputeIntegrityOnUpdate() {
    final var submission =
        new Submission(
            "test",
            "CERT-001",
            Map.of("name", new Attribute("name", Value.of("Alice"), "test", true, Instant.now())),
            "owner",
            null,
            null);
    final var created = repository.create(submission);
    final var original = created.integrity();

    final var amendment =
        new Amendment(
            Map.of("name", new Attribute("name", Value.of("Bob"), "test", true, Instant.now())),
            null);
    final var updated = repository.update(created.urn(), amendment);

    assertThat(updated.integrity()).isNotNull();
    assertThat(updated.integrity().digest()).isNotEqualTo(original.digest());
  }

  @Test
  void shouldRecomputeIntegrityOnRetire() {
    final var submission = new Submission("test", "CERT-001", null, "owner", null, null);
    final var created = repository.create(submission);
    final var original = created.integrity();

    final var retired = repository.retire(created.urn());

    assertThat(retired.integrity()).isNotNull();
    assertThat(retired.integrity().digest()).isNotEqualTo(original.digest());
    assertThat(retired.status()).isEqualTo(Status.RETIRED);
  }

  @Test
  void shouldRejectCrossNamespaceSchema() {
    final var definition =
        new Definition(
            "other",
            "carbon",
            null,
            List.of(new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 0)),
            "owner");
    final var schema = repository.define(definition);

    final var submission =
        new Submission(
            "test",
            "CERT-001",
            Map.of(
                "volume",
                new Attribute("volume", Value.of(1000, "tCO2e"), "test", true, Instant.now())),
            "owner",
            schema.id(),
            null);

    assertThatThrownBy(() -> repository.create(submission))
        .isInstanceOf(InvalidSubmissionException.class)
        .hasMessageContaining("namespace");
  }

  @Test
  void shouldListVersions() {
    final var definition = new Definition("test", "carbon", null, List.of(), "owner");
    final var schema = repository.define(definition);

    repository.revise(schema.id(), new Revision("v2", List.of(), true));
    repository.revise(schema.id(), new Revision("v3", List.of(), true));

    final var versions = repository.versions(schema.id());

    assertThat(versions).hasSize(3);
    assertThat(versions.get(0).version()).isEqualTo(3);
    assertThat(versions.get(2).version()).isEqualTo(1);
  }

  @Test
  void shouldCreateWithNullAttributes() {
    final var submission = new Submission("test", "CERT-010", null, "owner", null, null);

    final var certificate = repository.create(submission);

    assertThat(certificate.attributes()).isEmpty();
    assertThat(certificate.owner()).isEqualTo("owner");
  }

  @Test
  void shouldCreateWithEmptyAttributes() {
    final var submission = new Submission("test", "CERT-011", Map.of(), "owner", null, null);

    final var certificate = repository.create(submission);

    assertThat(certificate.attributes()).isEmpty();
  }

  @Test
  void shouldCreateWithMultipleAttributes() {
    final var now = Instant.now();
    final var attributes =
        Map.of(
            "region", new Attribute("region", Value.of("EU"), "repository", true, now),
            "volume", new Attribute("volume", Value.of(1000, "tCO2e"), "repository", true, now),
            "project", new Attribute("project", Value.of("WindFarm"), "repository", false, now));
    final var submission = new Submission("test", "CERT-012", attributes, "owner", null, null);

    final var certificate = repository.create(submission);

    assertThat(certificate.attributes()).hasSize(3);
    assertThat(certificate.attributes()).containsKeys("region", "volume", "project");
  }

  @Test
  void shouldCreateWithNullOwner() {
    final var submission = new Submission("test", "CERT-013", null, null, null, null);

    final var certificate = repository.create(submission);

    assertThat(certificate.owner()).isNull();
  }

  @Test
  void shouldAssignIntegrityOnCreate() {
    final var submission = new Submission("test", "CERT-014", null, "owner", null, null);

    final var certificate = repository.create(submission);

    assertThat(certificate.integrity()).isNotNull();
    assertThat(certificate.integrity().digest()).isNotBlank();
    assertThat(certificate.integrity().algorithm()).isEqualTo("sha-256");
  }

  @Test
  void shouldAssignTimestampsOnCreate() {
    final var before = Instant.now();
    final var submission = new Submission("test", "CERT-015", null, "owner", null, null);

    final var certificate = repository.create(submission);

    assertThat(certificate.created()).isAfterOrEqualTo(before);
    assertThat(certificate.modified()).isEqualTo(certificate.created());
  }

  @Test
  void shouldRejectBlankNamespace() {
    assertThatThrownBy(() -> new Submission("", "CERT-001", null, null, null, null))
        .isInstanceOf(InvalidSubmissionException.class)
        .hasMessageContaining("Namespace");
  }

  @Test
  void shouldRejectNullNamespace() {
    assertThatThrownBy(() -> new Submission(null, "CERT-001", null, null, null, null))
        .isInstanceOf(InvalidSubmissionException.class);
  }

  @Test
  void shouldRejectBlankIdentifier() {
    assertThatThrownBy(() -> new Submission("test", "", null, null, null, null))
        .isInstanceOf(InvalidSubmissionException.class)
        .hasMessageContaining("Identifier");
  }

  @Test
  void shouldRejectNullIdentifier() {
    assertThatThrownBy(() -> new Submission("test", null, null, null, null, null))
        .isInstanceOf(InvalidSubmissionException.class);
  }

  @Test
  void shouldUpdateAttributes() {
    final var now = Instant.now();
    final var submission =
        new Submission(
            "test",
            "CERT-020",
            Map.of("name", new Attribute("name", Value.of("Alice"), "test", true, now)),
            "owner",
            null,
            null);
    final var created = repository.create(submission);

    final var amendment =
        new Amendment(
            Map.of("name", new Attribute("name", Value.of("Bob"), "test", true, now)), null);
    final var updated = repository.update(created.urn(), amendment);

    assertThat(updated.attributes().get("name").value()).isEqualTo(Value.of("Bob"));
    assertThat(updated.status()).isEqualTo(Status.DRAFT);
  }

  @Test
  void shouldUpdateStatus() {
    final var submission = new Submission("test", "CERT-021", null, "owner", null, null);
    final var created = repository.create(submission);

    final var amendment = new Amendment(null, Status.ACTIVE);
    final var updated = repository.update(created.urn(), amendment);

    assertThat(updated.status()).isEqualTo(Status.ACTIVE);
  }

  @Test
  void shouldMergeAttributes() {
    final var now = Instant.now();
    final var submission =
        new Submission(
            "test",
            "CERT-022",
            Map.of("region", new Attribute("region", Value.of("EU"), "test", true, now)),
            "owner",
            null,
            null);
    final var created = repository.create(submission);

    final var amendment =
        new Amendment(
            Map.of("volume", new Attribute("volume", Value.of(500), "test", true, now)), null);
    final var updated = repository.update(created.urn(), amendment);

    assertThat(updated.attributes()).containsKeys("region", "volume");
  }

  @Test
  void shouldUpdateMissing() {
    assertThatThrownBy(
            () -> repository.update(new Urn("test", "MISSING"), new Amendment(null, Status.ACTIVE)))
        .isInstanceOf(CertificateNotFoundException.class);
  }

  @Test
  void shouldRejectInvalidUpdateAgainstSchema() {
    final var definition =
        new Definition(
            "test",
            "strict",
            null,
            List.of(new Field("volume", "Volume", null, new QuantityType("tCO2e"), true, 0)),
            "owner");
    final var schema = repository.define(definition);

    final var submission =
        new Submission(
            "test",
            "CERT-023",
            Map.of(
                "volume",
                new Attribute("volume", Value.of(100, "tCO2e"), "test", true, Instant.now())),
            "owner",
            schema.id(),
            null);
    final var created = repository.create(submission);

    final var amendment =
        new Amendment(
            Map.of(
                "volume",
                new Attribute("volume", Value.of("not-a-number"), "test", true, Instant.now())),
            null);

    assertThatThrownBy(() -> repository.update(created.urn(), amendment))
        .isInstanceOf(InvalidSubmissionException.class)
        .hasMessageContaining("Schema validation failed");
  }

  @Test
  void shouldRetireWithIntegrity() {
    final var submission = new Submission("test", "CERT-030", null, "owner", null, null);
    repository.create(submission);

    final var retired = repository.retire(new Urn("test", "CERT-030"));

    assertThat(retired.status()).isEqualTo(Status.RETIRED);
    assertThat(retired.integrity()).isNotNull();
    assertThat(retired.integrity().algorithm()).isEqualTo("sha-256");
  }

  @Test
  void shouldRejectRetireMissing() {
    assertThatThrownBy(() -> repository.retire(new Urn("test", "MISSING")))
        .isInstanceOf(CertificateNotFoundException.class);
  }

  @Test
  void shouldRecordCreateEvent() {
    final var submission = new Submission("test", "CERT-040", null, "owner", null, null);
    repository.create(submission);

    final var events = repository.events(new Urn("test", "CERT-040"));

    assertThat(events).hasSize(1);
    assertThat(events.get(0).type()).isEqualTo(net.far.resolver.model.Event.EventType.ISSUANCE);
  }

  @Test
  void shouldRecordUpdateEvent() {
    final var submission = new Submission("test", "CERT-041", null, "owner", null, null);
    repository.create(submission);

    repository.update(new Urn("test", "CERT-041"), new Amendment(null, Status.ACTIVE));

    final var events = repository.events(new Urn("test", "CERT-041"));

    assertThat(events).hasSize(2);
    assertThat(events.get(1).type())
        .isEqualTo(net.far.resolver.model.Event.EventType.STATUS_CHANGE);
  }

  @Test
  void shouldRecordRetireEvent() {
    final var submission = new Submission("test", "CERT-042", null, "owner", null, null);
    repository.create(submission);

    repository.retire(new Urn("test", "CERT-042"));

    final var events = repository.events(new Urn("test", "CERT-042"));

    assertThat(events).hasSize(2);
    assertThat(events.get(1).type()).isEqualTo(net.far.resolver.model.Event.EventType.RETIREMENT);
  }

  @Test
  void shouldReturnEmptyEventsForUnknown() {
    final var events = repository.events(new Urn("test", "NONEXISTENT"));

    assertThat(events).isEmpty();
  }

  @Test
  void shouldSearchAll() {
    repository.create(new Submission("test", "CERT-050", null, "owner", null, null));
    repository.create(new Submission("test", "CERT-051", null, "owner", null, null));

    final var page =
        repository.search(
            new net.far.resolver.model.query.Query(java.util.Set.of(), null, 25, 0, null));

    assertThat(page.value()).hasSizeGreaterThanOrEqualTo(2);
    assertThat(page.count()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void shouldReturnRecent() {
    repository.create(new Submission("test", "CERT-052", null, "owner", null, null));
    repository.create(new Submission("test", "CERT-053", null, "owner", null, null));

    final var recent = repository.recent(10);

    assertThat(recent).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void shouldCompleteLifecycle() {
    final var now = Instant.now();
    final var submission =
        new Submission(
            "test",
            "CERT-060",
            Map.of("status", new Attribute("status", Value.of("pending"), "test", true, now)),
            "owner",
            null,
            null);
    final var created = repository.create(submission);
    assertThat(created.status()).isEqualTo(Status.DRAFT);

    final var activated = repository.update(created.urn(), new Amendment(null, Status.ACTIVE));
    assertThat(activated.status()).isEqualTo(Status.ACTIVE);

    final var updated =
        repository.update(
            created.urn(),
            new Amendment(
                Map.of("status", new Attribute("status", Value.of("verified"), "test", true, now)),
                null));
    assertThat(updated.attributes().get("status").value()).isEqualTo(Value.of("verified"));
    assertThat(updated.status()).isEqualTo(Status.ACTIVE);

    final var retired = repository.retire(created.urn());
    assertThat(retired.status()).isEqualTo(Status.RETIRED);

    final var events = repository.events(created.urn());
    assertThat(events).hasSize(4);
  }

  @Test
  void shouldListAllSchemas() {
    repository.define(new Definition("test", "alpha", null, List.of(), "owner"));
    repository.define(new Definition("test", "beta", null, List.of(), "owner"));

    final var schemas = repository.schemas();

    assertThat(schemas).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void shouldListSchemasByNamespace() {
    repository.define(new Definition("ns1", "schema1", null, List.of(), "owner"));
    repository.define(new Definition("ns2", "schema2", null, List.of(), "owner"));

    final var ns1 = repository.schemas("ns1");

    assertThat(ns1).hasSize(1);
    assertThat(ns1.get(0).name()).isEqualTo("schema1");
  }

  @Test
  void shouldManagePolicies() {
    final var schema =
        repository.define(new Definition("test", "policy", null, List.of(), "owner"));

    final var policies =
        List.of(
            new FieldPolicy("name", AccessPolicy.PUBLIC),
            new FieldPolicy("secret", AccessPolicy.MASKED));
    repository.policies(schema.id(), policies);

    final var retrieved = repository.policies(schema.id());

    assertThat(retrieved).hasSize(2);
  }

  @Test
  void shouldReturnEmptyPolicies() {
    final var schema =
        repository.define(new Definition("test", "nopolicy", null, List.of(), "owner"));

    final var policies = repository.policies(schema.id());

    assertThat(policies).isEmpty();
  }
}
