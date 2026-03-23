package net.far.repository.web.peers;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/** Authentication request carrying RFC 9421 HTTP message signature credentials from a peer. */
public class PeerSignatureRequest extends BaseAuthenticationRequest {

  private final String input;
  private final String signature;
  private final String method;
  private final String target;
  private final String authority;
  private final String type;
  private final String digest;
  private final String token;
  private final String chain;

  public PeerSignatureRequest(
      final String input,
      final String signature,
      final String method,
      final String target,
      final String authority,
      final String type,
      final String digest,
      final String token,
      final String chain) {
    this.input = input;
    this.signature = signature;
    this.method = method;
    this.target = target;
    this.authority = authority;
    this.type = type;
    this.digest = digest;
    this.token = token;
    this.chain = chain;
  }

  public String input() {
    return input;
  }

  public String signature() {
    return signature;
  }

  public String method() {
    return method;
  }

  public String target() {
    return target;
  }

  public String authority() {
    return authority;
  }

  public String type() {
    return type;
  }

  public String digest() {
    return digest;
  }

  public String token() {
    return token;
  }

  public String chain() {
    return chain;
  }
}
