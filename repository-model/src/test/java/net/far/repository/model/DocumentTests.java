package net.far.repository.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DocumentTests {

  @Test
  void shouldCreateDocument() {
    final var document =
        new Document("doc-1", "report.pdf", "application/pdf", 1024, "sha-256=:abc:", "sig", null);

    assertThat(document.id()).isEqualTo("doc-1");
    assertThat(document.filename()).isEqualTo("report.pdf");
    assertThat(document.uploaded()).isNotNull();
  }

  @Test
  void shouldRejectBlankId() {
    assertThatThrownBy(
            () -> new Document("", "report.pdf", "application/pdf", 1024, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankFilename() {
    assertThatThrownBy(() -> new Document("doc-1", "", "application/pdf", 1024, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
