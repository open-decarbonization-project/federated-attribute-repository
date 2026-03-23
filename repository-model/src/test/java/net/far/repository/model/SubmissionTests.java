package net.far.repository.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SubmissionTests {

  @Test
  void shouldCreateWithDefaults() {
    final var submission = new Submission("test", "CERT-001", null, "owner", null, null);

    assertThat(submission.namespace()).isEqualTo("test");
    assertThat(submission.identifier()).isEqualTo("CERT-001");
    assertThat(submission.attributes()).isEmpty();
  }

  @Test
  void shouldRejectBlankNamespace() {
    assertThatThrownBy(() -> new Submission("", "CERT-001", null, null, null, null))
        .isInstanceOf(InvalidSubmissionException.class);
  }

  @Test
  void shouldRejectBlankIdentifier() {
    assertThatThrownBy(() -> new Submission("test", "", null, null, null, null))
        .isInstanceOf(InvalidSubmissionException.class);
  }
}
