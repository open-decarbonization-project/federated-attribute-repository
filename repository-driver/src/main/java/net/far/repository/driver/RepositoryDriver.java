package net.far.repository.driver;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import net.far.repository.core.Repository;
import net.far.repository.model.Certificate;
import net.far.repository.model.CertificateNotFoundException;
import net.far.resolver.model.*;
import net.far.resolver.model.query.Page;
import net.far.resolver.model.query.Query;
import net.far.resolver.spi.Driver;

/**
 * Bridges the repository to the resolver's {@link Driver} SPI, allowing the resolver to query
 * locally stored certificates. Converts between the repository's {@link Certificate} model and the
 * resolver's {@link Resolution} model.
 */
public class RepositoryDriver implements Driver {

  private final Repository repository;
  private final Set<String> namespaces;
  private final String identity;

  public RepositoryDriver(
      final Repository repository, final Set<String> namespaces, final String identity) {
    this.repository = repository;
    this.namespaces = namespaces;
    this.identity = identity;
  }

  private static String escape(final String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  @Override
  public String name() {
    return "repository";
  }

  @Override
  public Set<String> namespaces() {
    return namespaces;
  }

  @Override
  public Optional<Resolution> resolve(final Urn urn) {
    try {
      final var certificate = repository.find(urn);
      return Optional.of(toResolution(certificate));
    } catch (final CertificateNotFoundException exception) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<History> history(final Urn urn) {
    try {
      repository.find(urn);
      final var events = repository.events(urn);
      if (events.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(new History(urn, events));
    } catch (final CertificateNotFoundException exception) {
      return Optional.empty();
    }
  }

  @Override
  public boolean exists(final Urn urn) {
    return repository.exists(urn);
  }

  @Override
  public Optional<Rendition> rendition(final Urn urn, final String media) {
    if (!"text/html".equals(media)) {
      return Optional.empty();
    }
    try {
      final var certificate = repository.find(urn);
      final var html =
          "<html><body><h1>"
              + escape(urn.toString())
              + "</h1>"
              + "<table>"
              + certificate.attributes().entrySet().stream()
                  .map(
                      entry ->
                          "<tr><td>"
                              + escape(entry.getKey())
                              + "</td><td>"
                              + escape(String.valueOf(entry.getValue().value()))
                              + "</td></tr>")
                  .reduce("", String::concat)
              + "</table></body></html>";
      return Optional.of(
          new Rendition(html.getBytes(StandardCharsets.UTF_8), media, urn, Instant.now()));
    } catch (final CertificateNotFoundException exception) {
      return Optional.empty();
    }
  }

  @Override
  public Page query(final Query query) {
    final var result = repository.search(query);
    final var resolutions = result.value().stream().map(this::toResolution).toList();
    return new Page(resolutions, result.count(), result.skip(), result.top());
  }

  private Resolution toResolution(final Certificate certificate) {
    return new Resolution(
        certificate.urn(),
        certificate.namespace(),
        certificate.identifier(),
        certificate.attributes(),
        certificate.integrity(),
        identity,
        certificate.status().name(),
        certificate.modified());
  }
}
