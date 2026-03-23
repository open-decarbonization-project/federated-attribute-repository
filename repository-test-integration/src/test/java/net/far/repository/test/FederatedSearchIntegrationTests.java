package net.far.repository.test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.far.resolver.core.PeerRegistry;
import net.far.resolver.model.Peer;
import net.far.resolver.signature.DigestCalculator;
import net.far.resolver.signature.KeyManager;
import net.far.resolver.signature.MessageComponents;
import net.far.resolver.signature.Signer;
import net.far.resolver.signature.Verifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FederatedSearchIntegrationTests {

  @Inject PeerRegistry peers;
  @Inject KeyManager localKeys;
  @Inject Verifier verifier;

  private HttpServer mock;
  private KeyManager peerKeys;
  private Signer peerSigner;

  private void start(final String identity, final Set<String> namespaces) throws IOException {
    peerKeys = KeyManager.generate(identity + "#key-1");
    peerSigner = new Signer(peerKeys);
    mock = HttpServer.create(new InetSocketAddress(0), 0);
    mock.start();
    final var port = mock.getAddress().getPort();
    peers.register(
        new Peer(
            identity,
            "http://localhost:" + port,
            namespaces,
            peerKeys.pem(),
            peerKeys.id(),
            List.of(),
            Instant.now(),
            Integer.MAX_VALUE,
            true,
            "http://localhost:" + port + "/v1",
            5));
  }

  private void respond(final HttpExchange exchange, final String body) throws IOException {
    final var bytes = body.getBytes(StandardCharsets.UTF_8);
    final var digest = DigestCalculator.compute(bytes);
    final var type = "application/json;charset=UTF-8";
    final var headers = new LinkedHashMap<String, String>();
    final var components = new MessageComponents(200, null, null, null, type, digest, headers);
    final var signature = peerSigner.sign(components);
    exchange.getResponseHeaders().set("Content-Type", type);
    exchange.getResponseHeaders().set("Content-Digest", digest);
    exchange.getResponseHeaders().set("Signature", "sig1=:" + signature.value() + ":");
    exchange.getResponseHeaders().set("Signature-Input", signature.input());
    exchange.sendResponseHeaders(200, bytes.length);
    try (final var output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  @AfterEach
  void teardown() {
    if (mock != null) {
      mock.stop(0);
    }
    peers.remove("https://peer.example.com");
  }

  @Test
  @Order(0)
  @TestSecurity(user = "tester")
  void setup() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"namespace":"test","identifier":"FAR-LOCAL-001"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body("""
            {"namespace":"test","identifier":"FAR-LOCAL-002"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);
  }

  @Test
  @Order(1)
  @TestSecurity(user = "tester")
  void shouldSearchLocalCertificates() {
    given()
        .queryParam("$filter", "contains(identifier,'FAR-LOCAL')")
        .when()
        .get("/v1/resources")
        .then()
        .statusCode(200)
        .body("value.size()", greaterThanOrEqualTo(2));
  }

  @Test
  @Order(2)
  @TestSecurity(user = "tester")
  void shouldSearchAcrossLocalAndPeer() throws IOException {
    start("https://peer.example.com", Set.of("remote"));
    mock.createContext(
        "/v1/resources",
        exchange -> {
          respond(
              exchange,
              """
              {
                "value": [
                  {
                    "urn": "urn:far:remote:FAR-REMOTE-001",
                    "namespace": "remote",
                    "identifier": "FAR-REMOTE-001",
                    "attributes": {},
                    "resolver": "https://peer.example.com",
                    "timestamp": "%s"
                  },
                  {
                    "urn": "urn:far:remote:FAR-REMOTE-002",
                    "namespace": "remote",
                    "identifier": "FAR-REMOTE-002",
                    "attributes": {},
                    "resolver": "https://peer.example.com",
                    "timestamp": "%s"
                  }
                ],
                "count": 2,
                "skip": 0,
                "top": 25
              }
              """
                  .formatted(Instant.now().toString(), Instant.now().toString()));
        });

    final var response =
        given()
            .queryParam("$filter", "contains(identifier,'FAR')")
            .when()
            .get("/v1/resources")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var count = response.jsonPath().getInt("value.size()");
    assertThat(count).as("Must include both local and remote results").isGreaterThanOrEqualTo(4);

    final var namespaces = response.jsonPath().getList("value.namespace", String.class);
    assertThat(namespaces).as("Must include local namespace").contains("test");
    assertThat(namespaces).as("Must include remote namespace").contains("remote");
  }

  @Test
  @Order(3)
  @TestSecurity(user = "tester")
  void shouldExcludeUnsignedPeerFromSearch() throws IOException {
    mock = HttpServer.create(new InetSocketAddress(0), 0);
    mock.createContext(
        "/v1/resources",
        exchange -> {
          final var body = """
              {"value":[],"count":0,"skip":0,"top":25}
              """;
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          final var bytes = body.getBytes();
          exchange.sendResponseHeaders(200, bytes.length);
          try (final var output = exchange.getResponseBody()) {
            output.write(bytes);
          }
        });
    mock.start();
    final var port = mock.getAddress().getPort();
    peers.register(
        new Peer(
            "https://peer.example.com",
            "http://localhost:" + port,
            Set.of("unsns"),
            null,
            null,
            Instant.now()));

    given()
        .queryParam("$filter", "namespace eq 'unsns'")
        .when()
        .get("/v1/resources")
        .then()
        .statusCode(200)
        .body("value.size()", equalTo(0));
  }

  @Test
  @Order(4)
  @TestSecurity(user = "tester")
  void shouldResolveFederatedUrn() throws IOException {
    start("https://peer.example.com", Set.of("remote"));
    mock.createContext(
        "/v1/resources/urn:far:remote:REM-001",
        exchange ->
            respond(
                exchange,
                """
                {
                  "urn": "urn:far:remote:REM-001",
                  "namespace": "remote",
                  "identifier": "REM-001",
                  "attributes": {
                    "region": {"name":"region","value":"EU","source":"peer","verified":true}
                  },
                  "resolver": "https://peer.example.com",
                  "timestamp": "%s"
                }
                """
                    .formatted(Instant.now().toString())));

    given()
        .urlEncodingEnabled(false)
        .when()
        .get("/v1/resources/urn:far:remote:REM-001")
        .then()
        .statusCode(200)
        .body("namespace", equalTo("remote"))
        .body("attributes.region.value", equalTo("EU"));
  }

  @Test
  @Order(5)
  @TestSecurity(user = "tester")
  void shouldSignFederatedSearchResponse() throws IOException {
    start("https://peer.example.com", Set.of("sigtest"));
    mock.createContext(
        "/v1/resources",
        exchange ->
            respond(
                exchange,
                """
                {"value":[],"count":0,"skip":0,"top":25}
                """));

    given()
        .queryParam("$filter", "namespace eq 'sigtest'")
        .when()
        .get("/v1/resources")
        .then()
        .statusCode(200)
        .header("Content-Digest", startsWith("sha-256=:"))
        .header("Signature", startsWith("sig1=:"))
        .header("Signature-Input", notNullValue());
  }

  @Test
  @Order(6)
  @TestSecurity(user = "tester")
  void shouldReturn404ForUnknownNamespace() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .get("/v1/resources/urn:far:nonexistent:GONE-001")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(7)
  @TestSecurity(user = "tester")
  void shouldResolveLocalViaResources() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .get("/v1/resources/urn:far:test:FAR-LOCAL-001")
        .then()
        .statusCode(200)
        .body("namespace", equalTo("test"))
        .body("timestamp", notNullValue());
  }

  @Test
  @Order(8)
  @TestSecurity(user = "tester")
  void shouldResolveExistsMode() {
    given()
        .urlEncodingEnabled(false)
        .queryParam("resolve", "exists")
        .when()
        .get("/v1/resources/urn:far:test:FAR-LOCAL-001")
        .then()
        .statusCode(200)
        .body("exists", equalTo(true));
  }

  @Test
  @Order(9)
  @TestSecurity(user = "tester")
  void shouldResolveSummaryMode() {
    given()
        .urlEncodingEnabled(false)
        .queryParam("resolve", "summary")
        .when()
        .get("/v1/resources/urn:far:test:FAR-LOCAL-001")
        .then()
        .statusCode(200)
        .body("urn", notNullValue())
        .body("status", notNullValue())
        .body("$", not(hasKey("attributes")));
  }

  @Test
  @Order(10)
  @TestSecurity(user = "tester")
  void shouldPreserveUpstreamResolver() throws IOException {
    start("https://peer.example.com", Set.of("provns"));
    mock.createContext(
        "/v1/resources",
        exchange ->
            respond(
                exchange,
                """
                {
                  "value": [{
                    "urn": "urn:far:provns:P-001",
                    "namespace": "provns",
                    "identifier": "P-001",
                    "attributes": {},
                    "resolver": "https://origin.example.com",
                    "timestamp": "%s"
                  }],
                  "count": 1, "skip": 0, "top": 25
                }
                """
                    .formatted(Instant.now().toString())));

    given()
        .queryParam("$filter", "namespace eq 'provns'")
        .when()
        .get("/v1/resources")
        .then()
        .statusCode(200)
        .body("value[0].resolver", equalTo("https://origin.example.com"));
  }

  @Test
  @Order(11)
  void shouldVerifyLocalResponseSignature() {
    final var response =
        given()
            .queryParam("$filter", "namespace eq 'test' and identifier eq 'FAR-LOCAL-001'")
            .when()
            .get("/v1/resources")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var body = response.body().asByteArray();
    final var digest = response.header("Content-Digest");
    final var signature = response.header("Signature");
    final var input = response.header("Signature-Input");

    assertThat(DigestCalculator.validate(body, digest)).isTrue();

    final var type = response.header("Content-Type");
    final var headers = new LinkedHashMap<String, String>();
    if (response.header("Far-Resolver") != null)
      headers.put("far-resolver", response.header("Far-Resolver"));
    if (response.header("Far-Namespace") != null)
      headers.put("far-namespace", response.header("Far-Namespace"));
    final var components = new MessageComponents(200, null, null, null, type, digest, headers);
    final var raw = signature.substring("sig1=:".length(), signature.length() - 1);
    assertThat(verifier.verify(components, raw, input, localKeys.verifying())).isTrue();
  }

  @Test
  @Order(12)
  @TestSecurity(user = "tester")
  void shouldForwardToken() throws IOException {
    final var captured = new AtomicReference<String>();
    start("https://peer.example.com", Set.of("tokenns"));
    mock.createContext(
        "/v1/resources/urn:far:tokenns:TOK-001",
        exchange -> {
          captured.set(exchange.getRequestHeaders().getFirst("Authorization"));
          respond(
              exchange,
              """
              {
                "urn": "urn:far:tokenns:TOK-001",
                "namespace": "tokenns",
                "identifier": "TOK-001",
                "attributes": {},
                "resolver": "https://peer.example.com",
                "timestamp": "%s"
              }
              """
                  .formatted(Instant.now().toString()));
        });

    given()
        .urlEncodingEnabled(false)
        .header("Authorization", "Bearer my-token")
        .when()
        .get("/v1/resources/urn:far:tokenns:TOK-001")
        .then()
        .statusCode(200);

    assertThat(captured.get()).isEqualTo("Bearer my-token");
  }

  @Test
  @Order(13)
  @TestSecurity(user = "tester")
  void shouldRestrictToLocalWithLocalParam() throws IOException {
    start("https://peer.example.com", Set.of("remote"));
    mock.createContext(
        "/v1/resources",
        exchange ->
            respond(
                exchange,
                """
                {
                  "value": [{
                    "urn": "urn:far:remote:SHOULD-NOT-APPEAR",
                    "namespace": "remote",
                    "identifier": "SHOULD-NOT-APPEAR",
                    "attributes": {},
                    "resolver": "https://peer.example.com",
                    "timestamp": "%s"
                  }],
                  "count": 1, "skip": 0, "top": 25
                }
                """
                    .formatted(Instant.now().toString())));

    final var response =
        given()
            .queryParam("$filter", "contains(identifier,'FAR')")
            .queryParam("local", "true")
            .when()
            .get("/v1/resources")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var namespaces = response.jsonPath().getList("value.namespace", String.class);
    assertThat(namespaces).as("local=true must exclude remote peers").doesNotContain("remote");
  }

  @Test
  @Order(14)
  @TestSecurity(user = "tester")
  void shouldNotReDelegateOnDelegatedRequest() throws IOException {
    start("https://peer.example.com", Set.of("remote"));
    final var delegated = new AtomicReference<Boolean>(false);
    mock.createContext(
        "/v1/resources",
        exchange -> {
          delegated.set(true);
          respond(exchange, """
              {"value":[],"count":0,"skip":0,"top":25}
              """);
        });

    given()
        .queryParam("$filter", "contains(identifier,'FAR')")
        .header("Far-Delegation-Chain", "https://other.example.com")
        .when()
        .get("/v1/resources")
        .then()
        .statusCode(200);

    assertThat(delegated.get())
        .as("Delegated requests must not re-delegate to peers")
        .isFalse();
  }
}
