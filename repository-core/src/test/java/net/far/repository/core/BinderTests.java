package net.far.repository.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.far.resolver.signature.KeyManager;
import net.far.resolver.signature.Signer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BinderTests {

  private InMemoryStore store;
  private Binder binder;

  @BeforeEach
  void setup() {
    store = new InMemoryStore();
    final var keys = KeyManager.generate("test-key");
    final var signer = new Signer(keys);
    binder = new Binder(store, signer);
  }

  @Test
  void shouldUploadDocument() {
    final var content = "test content".getBytes();

    final var document = binder.upload("report.pdf", "application/pdf", content);

    assertThat(document.filename()).isEqualTo("report.pdf");
    assertThat(document.media()).isEqualTo("application/pdf");
    assertThat(document.size()).isEqualTo(content.length);
    assertThat(document.digest()).startsWith("sha-256=:");
    assertThat(document.signature()).isNotNull();
  }

  @Test
  void shouldPersistUploadedDocument() {
    final var content = "test content".getBytes();

    binder.upload("report.pdf", "application/pdf", content);

    assertThat(binder.documents()).hasSize(1);
    assertThat(binder.documents().get(0).filename()).isEqualTo("report.pdf");
  }

  @Test
  void shouldRetrieveContent() {
    final var content = "test content".getBytes();
    final var document = binder.upload("report.pdf", "application/pdf", content);

    final var retrieved = binder.content(document.id());

    assertThat(retrieved).isPresent();
    assertThat(retrieved.get()).isEqualTo(content);
  }

  @Test
  void shouldReturnEmptyForMissingContent() {
    final var retrieved = binder.content("nonexistent");

    assertThat(retrieved).isEmpty();
  }

  @Test
  void shouldListEmpty() {
    assertThat(binder.documents()).isEmpty();
  }

  @Test
  void shouldRejectUnsupportedMedia() {
    assertThatThrownBy(() -> binder.upload("page.html", "text/html", "test".getBytes()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported media type");
  }

  @Test
  void shouldRejectOversizedUpload() {
    final var oversized = new byte[Binder.MAX_SIZE + 1];
    assertThatThrownBy(() -> binder.upload("big.pdf", "application/pdf", oversized))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maximum size");
  }

  @Test
  void shouldAcceptAllAllowedMediaTypes() {
    for (final var media : Binder.ALLOWED) {
      final var document = binder.upload("file", media, "data".getBytes());
      assertThat(document.media()).isEqualTo(media);
    }
  }

  @Test
  void shouldRejectExecutable() {
    assertThatThrownBy(
            () -> binder.upload("run.exe", "application/octet-stream", "data".getBytes()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectJavascript() {
    assertThatThrownBy(
            () -> binder.upload("script.js", "application/javascript", "data".getBytes()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldClassifyAllowedTypes() {
    assertThat(Binder.allowed("application/pdf")).isTrue();
    assertThat(Binder.allowed("application/json")).isTrue();
    assertThat(Binder.allowed("image/png")).isTrue();
    assertThat(Binder.allowed("image/jpeg")).isTrue();
    assertThat(Binder.allowed("text/csv")).isTrue();
    assertThat(Binder.allowed("text/plain")).isTrue();
    assertThat(Binder.allowed("text/html")).isFalse();
    assertThat(Binder.allowed("application/octet-stream")).isFalse();
    assertThat(Binder.allowed("")).isFalse();
  }

  @Test
  void shouldRecordUploader() {
    final var document = binder.upload("report.pdf", "application/pdf", "data".getBytes(), "alice");

    assertThat(document.uploader()).isEqualTo("alice");
  }

  @Test
  void shouldUploadWithNullUploader() {
    final var document = binder.upload("report.pdf", "application/pdf", "data".getBytes());

    assertThat(document.uploader()).isNull();
  }

  @Test
  void shouldFindDocumentById() {
    final var uploaded = binder.upload("report.pdf", "application/pdf", "data".getBytes());

    final var found = binder.document(uploaded.id());

    assertThat(found).isPresent();
    assertThat(found.get().filename()).isEqualTo("report.pdf");
  }

  @Test
  void shouldReturnEmptyForMissingDocument() {
    assertThat(binder.document("nonexistent")).isEmpty();
  }

  @Test
  void shouldComputeDigest() {
    final var document = binder.upload("file.json", "application/json", "content".getBytes());

    assertThat(document.digest()).startsWith("sha-256=:");
    assertThat(document.digest()).endsWith(":");
  }

  @Test
  void shouldSignDocument() {
    final var document = binder.upload("file.json", "application/json", "content".getBytes());

    assertThat(document.signature()).isNotNull();
    assertThat(document.signature()).isNotBlank();
  }

  @Test
  void shouldProduceDifferentDigestsForDifferentContent() {
    final var first = binder.upload("a.pdf", "application/pdf", "aaa".getBytes());
    final var second = binder.upload("b.pdf", "application/pdf", "bbb".getBytes());

    assertThat(first.digest()).isNotEqualTo(second.digest());
  }

  @Test
  void shouldAttachAndListDocuments() {
    final var document = binder.upload("report.pdf", "application/pdf", "data".getBytes());
    final var urn = new net.far.resolver.model.Urn("test", "CERT-001");

    binder.attach(urn, document.id());

    final var documents = binder.documents(urn);
    assertThat(documents).hasSize(1);
    assertThat(documents.get(0).id()).isEqualTo(document.id());
  }

  @Test
  void shouldDetachDocument() {
    final var document = binder.upload("report.pdf", "application/pdf", "data".getBytes());
    final var urn = new net.far.resolver.model.Urn("test", "CERT-002");

    binder.attach(urn, document.id());
    binder.detach(urn, document.id());

    assertThat(binder.documents(urn)).isEmpty();
  }

  @Test
  void shouldAttachMultipleDocuments() {
    final var first = binder.upload("a.pdf", "application/pdf", "aaa".getBytes());
    final var second = binder.upload("b.pdf", "application/pdf", "bbb".getBytes());
    final var urn = new net.far.resolver.model.Urn("test", "CERT-003");

    binder.attach(urn, first.id());
    binder.attach(urn, second.id());

    assertThat(binder.documents(urn)).hasSize(2);
  }

  @Test
  void shouldReturnEmptyDocumentsForUnknownUrn() {
    final var urn = new net.far.resolver.model.Urn("test", "NONEXISTENT");

    assertThat(binder.documents(urn)).isEmpty();
  }

  @Test
  void shouldPreserveBinaryContent() {
    final var binary = new byte[] {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};
    final var document = binder.upload("binary.png", "image/png", binary);

    final var content = binder.content(document.id());

    assertThat(content).isPresent();
    assertThat(content.get()).isEqualTo(binary);
  }

  @Test
  void shouldAcceptMaxSizeUpload() {
    final var exact = new byte[Binder.MAX_SIZE];
    final var document = binder.upload("max.pdf", "application/pdf", exact);

    assertThat(document.size()).isEqualTo(Binder.MAX_SIZE);
  }

  @Test
  void shouldAcceptEmptyContent() {
    final var document = binder.upload("empty.json", "application/json", new byte[0]);

    assertThat(document.size()).isEqualTo(0);
    assertThat(document.digest()).isNotNull();
  }

  @Test
  void shouldListMultipleUploads() {
    binder.upload("a.pdf", "application/pdf", "aaa".getBytes());
    binder.upload("b.json", "application/json", "bbb".getBytes());
    binder.upload("c.csv", "text/csv", "ccc".getBytes());

    assertThat(binder.documents()).hasSize(3);
  }
}
