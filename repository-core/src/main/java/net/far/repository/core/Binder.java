package net.far.repository.core;

import com.fasterxml.uuid.Generators;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.far.repository.model.Document;
import net.far.repository.spi.Store;
import net.far.resolver.model.Urn;
import net.far.resolver.signature.DigestCalculator;
import net.far.resolver.signature.MessageComponents;
import net.far.resolver.signature.Signer;

/**
 * Manages document uploads, binary storage, and certificate-document attachments. Each upload
 * computes a SHA-256 content digest and signs the document metadata with the repository's Ed25519
 * key for tamper evidence.
 */
public class Binder {

  public static final Set<String> ALLOWED =
      Set.of(
          "application/pdf",
          "application/json",
          "application/xml",
          "text/plain",
          "text/csv",
          "image/png",
          "image/jpeg",
          "image/gif",
          "image/webp",
          "image/svg+xml");

  public static final int MAX_SIZE = 10 * 1024 * 1024; // 10 MB

  private final Store store;
  private final Signer signer;

  public Binder(final Store store, final Signer signer) {
    this.store = store;
    this.signer = signer;
  }

  public static boolean allowed(final String media) {
    return ALLOWED.contains(media);
  }

  public Document upload(
      final String filename, final String media, final byte[] bytes, final String uploader) {
    if (!allowed(media)) {
      throw new IllegalArgumentException("Unsupported media type: " + media);
    }
    if (bytes.length > MAX_SIZE) {
      throw new IllegalArgumentException(
          "File exceeds maximum size of " + (MAX_SIZE / 1024 / 1024) + " MB");
    }
    final var digest = DigestCalculator.compute(bytes);
    final var components =
        new MessageComponents("POST", "/documents", "repository", media, digest, null);
    final var signed = signer.sign(components);
    final var document =
        new Document(
            Generators.timeBasedEpochGenerator().generate().toString(),
            filename,
            media,
            bytes.length,
            digest,
            signed.value(),
            uploader,
            Instant.now());
    return store.save(document, bytes);
  }

  public Document upload(final String filename, final String media, final byte[] bytes) {
    return upload(filename, media, bytes, null);
  }

  public void attach(final Urn urn, final String document) {
    store.attach(urn, document);
  }

  public void detach(final Urn urn, final String document) {
    store.detach(urn, document);
  }

  public Optional<Document> document(final String id) {
    return store.document(id);
  }

  public List<Document> documents() {
    return store.documents();
  }

  public List<Document> documents(final Urn urn) {
    return store.documents(urn);
  }

  public Optional<byte[]> content(final String id) {
    return store.content(id);
  }
}
