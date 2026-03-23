package net.far.repository.model;

import java.time.Instant;

/** Metadata for an uploaded document, including its content digest and Ed25519 signature. */
public record Document(
    String id,
    String filename,
    String media,
    long size,
    String digest,
    String signature,
    String uploader,
    Instant uploaded) {

  public Document(
      final String id,
      final String filename,
      final String media,
      final long size,
      final String digest,
      final String signature,
      final Instant uploaded) {
    this(id, filename, media, size, digest, signature, null, uploaded);
  }

  public Document {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Document id must not be blank");
    }
    if (filename == null || filename.isBlank()) {
      throw new IllegalArgumentException("Filename must not be blank");
    }
    if (media == null || media.isBlank()) {
      throw new IllegalArgumentException("Media type must not be blank");
    }
    if (digest == null || digest.isBlank()) {
      throw new IllegalArgumentException("Digest must not be blank");
    }
    if (uploaded == null) {
      uploaded = Instant.now();
    }
  }
}
