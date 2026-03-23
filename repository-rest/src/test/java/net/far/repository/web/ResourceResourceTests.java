package net.far.repository.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResourceResourceTests {

  @Test
  void shouldReturnNullForJsonAccept() {
    assertThat(ResourceResource.media("application/json")).isNull();
  }

  @Test
  void shouldReturnNullForWildcard() {
    assertThat(ResourceResource.media("*/*")).isNull();
  }

  @Test
  void shouldReturnHtmlForHtmlAccept() {
    assertThat(ResourceResource.media("text/html")).isEqualTo("text/html");
  }

  @Test
  void shouldReturnPdfForPdfAccept() {
    assertThat(ResourceResource.media("application/pdf")).isEqualTo("application/pdf");
  }

  @Test
  void shouldReturnNullForBlank() {
    assertThat(ResourceResource.media("")).isNull();
    assertThat(ResourceResource.media(null)).isNull();
  }

  @Test
  void shouldPreferJsonOverHtml() {
    assertThat(ResourceResource.media("application/json, text/html")).isNull();
  }

  @Test
  void shouldPickHtmlWhenFirst() {
    assertThat(ResourceResource.media("text/html, application/json")).isEqualTo("text/html");
  }
}
