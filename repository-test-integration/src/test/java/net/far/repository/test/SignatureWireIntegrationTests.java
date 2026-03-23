package net.far.repository.test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import net.far.resolver.json.Converter;
import net.far.resolver.model.Urn;
import net.far.resolver.signature.DigestCalculator;
import net.far.resolver.signature.KeyManager;
import net.far.resolver.signature.MessageComponents;
import net.far.resolver.signature.Verifier;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests the exact wire format produced by the RepositorySignatureFilter. These tests verify that
 * what a real HttpFarClient would receive from this repository can be parsed and signature-verified
 * correctly. This catches serialization mismatches, Content-Type drift, and digest/signature base
 * inconsistencies that mock-based tests miss.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SignatureWireIntegrationTests {

  @Inject KeyManager keys;

  @Inject Verifier verifier;

  @Inject ObjectMapper mapper;

  @Test
  @Order(0)
  @TestSecurity(user = "tester")
  void setup() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "namespace": "test",
              "identifier": "WIRE-001",
              "attributes": {
                "region": {"name": "region", "value": "EU", "source": "test", "verified": true},
                "volume": {"name": "volume", "value": "500", "source": "test", "verified": true}
              }
            }
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "namespace": "test",
              "identifier": "WIRE-002",
              "attributes": {
                "region": {"name": "region", "value": "US", "source": "test", "verified": true}
              }
            }
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);
  }

  @Test
  @Order(1)
  void shouldProduceJsonContentTypeOnResolve() {
    final var response =
        given()
            .urlEncodingEnabled(false)
            .when()
            .get("/v1/resources/urn:far:test:WIRE-001")
            .then()
            .statusCode(200)
            .extract()
            .response();

    assertThat(response.contentType()).startsWith("application/json");
  }

  @Test
  @Order(2)
  void shouldProduceJsonContentTypeOnQuery() {
    final var response =
        given()
            .queryParam("$filter", "namespace eq 'test' and identifier eq 'WIRE-001'")
            .when()
            .get("/v1/resources")
            .then()
            .statusCode(200)
            .extract()
            .response();

    assertThat(response.contentType()).startsWith("application/json");
  }

  @Test
  @Order(3)
  void shouldProduceValidJsonOnResolve() throws Exception {
    final var response =
        given()
            .urlEncodingEnabled(false)
            .when()
            .get("/v1/resources/urn:far:test:WIRE-001")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var body = response.body().asString();
    final var node = mapper.readTree(body);

    assertThat(node.has("urn")).isTrue();
    assertThat(node.get("urn").asText()).isEqualTo("urn:far:test:WIRE-001");
    assertThat(node.has("namespace")).isTrue();
    assertThat(node.has("attributes")).isTrue();
    assertThat(node.has("timestamp")).isTrue();
  }

  @Test
  @Order(4)
  void shouldProduceValidJsonOnQuery() throws Exception {
    final var response =
        given()
            .queryParam("$filter", "namespace eq 'test' and identifier eq 'WIRE-001'")
            .when()
            .get("/v1/resources")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var body = response.body().asString();
    final var node = mapper.readTree(body);

    assertThat(node.has("value")).isTrue();
    assertThat(node.get("value").isArray()).isTrue();
  }

  @Test
  @Order(5)
  void shouldHaveDigestMatchingBodyOnResolve() {
    final var response =
        given()
            .urlEncodingEnabled(false)
            .when()
            .get("/v1/resources/urn:far:test:WIRE-001")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var body = response.body().asByteArray();
    final var digest = response.header("Content-Digest");

    assertThat(digest).isNotNull();
    assertThat(DigestCalculator.validate(body, digest))
        .as("Content-Digest must match actual response body bytes")
        .isTrue();
  }

  @Test
  @Order(6)
  void shouldHaveDigestMatchingBodyOnQuery() {
    final var response =
        given()
            .queryParam("$filter", "namespace eq 'test' and identifier eq 'WIRE-001'")
            .when()
            .get("/v1/resources")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var body = response.body().asByteArray();
    final var digest = response.header("Content-Digest");

    assertThat(digest).isNotNull();
    assertThat(DigestCalculator.validate(body, digest))
        .as("Content-Digest must match actual response body bytes on query")
        .isTrue();
  }

  @Test
  @Order(7)
  void shouldVerifyResolveSignatureEndToEnd() {
    final var response =
        given()
            .urlEncodingEnabled(false)
            .when()
            .get("/v1/resources/urn:far:test:WIRE-001")
            .then()
            .statusCode(200)
            .extract()
            .response();

    verify(response);
  }

  @Test
  @Order(8)
  void shouldVerifyQuerySignatureEndToEnd() {
    final var response =
        given()
            .queryParam("$filter", "namespace eq 'test' and identifier eq 'WIRE-001'")
            .when()
            .get("/v1/resources")
            .then()
            .statusCode(200)
            .extract()
            .response();

    verify(response);
  }

  @Test
  @Order(9)
  void shouldRoundTripResolutionThroughNodesParser() throws Exception {
    final var response =
        given()
            .urlEncodingEnabled(false)
            .when()
            .get("/v1/resources/urn:far:test:WIRE-001")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var body = response.body().asString();
    final var node = mapper.readTree(body);
    final var resolution = Converter.resolution(node, new Urn("test", "WIRE-001"));

    assertThat(resolution.urn().toString()).isEqualTo("urn:far:test:WIRE-001");
    assertThat(resolution.namespace()).isEqualTo("test");
    assertThat(resolution.identifier()).isEqualTo("WIRE-001");
    assertThat(resolution.attributes()).containsKey("region");
    assertThat(resolution.timestamp()).isNotNull();
  }

  @Test
  @Order(10)
  void shouldRoundTripPageThroughNodesParser() throws Exception {
    final var response =
        given()
            .queryParam("$filter", "namespace eq 'test' and identifier eq 'WIRE-001'")
            .when()
            .get("/v1/resources")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var body = response.body().asString();
    final var node = mapper.readTree(body);
    final var page = Converter.page(node);

    assertThat(page.value()).isNotEmpty();
    assertThat(page.value().get(0).urn().toString()).isEqualTo("urn:far:test:WIRE-001");
    assertThat(page.count()).isGreaterThanOrEqualTo(1);
  }

  @Test
  @Order(11)
  void shouldHaveConsistentContentTypeBetweenSignatureAndHeader() {
    final var response =
        given()
            .urlEncodingEnabled(false)
            .when()
            .get("/v1/resources/urn:far:test:WIRE-001")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var input = response.header("Signature-Input");
    final var delivered = response.header("Content-Type");

    assertThat(input).contains("\"content-type\"");
    assertThat(delivered)
        .as("Content-Type must include charset")
        .isEqualTo("application/json;charset=UTF-8");
  }

  @Test
  @Order(12)
  void shouldMatchHttpFarClientVerificationLogic() {
    final var response =
        given()
            .queryParam("$filter", "namespace eq 'test' and identifier eq 'WIRE-001'")
            .when()
            .get("/v1/resources")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var body = response.body().asString();
    final var input = response.header("Signature-Input");
    final var signature = response.header("Signature");
    final var digest = response.header("Content-Digest");

    assertThat(signature).isNotNull();
    assertThat(input).isNotNull();

    // Step 1: Content-Digest validation (same as HttpFarClient)
    final var bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(DigestCalculator.validate(bytes, digest))
        .as("HttpFarClient would accept Content-Digest")
        .isTrue();

    // Step 2: Reconstruct components exactly as HttpFarClient.verify() does
    final var type = response.header("Content-Type");
    final var headers = new LinkedHashMap<String, String>();
    final var resolver = response.header("Far-Resolver");
    final var namespace = response.header("Far-Namespace");
    if (resolver != null) {
      headers.put("far-resolver", resolver);
    }
    if (namespace != null) {
      headers.put("far-namespace", namespace);
    }
    final var components =
        new MessageComponents(response.statusCode(), null, null, null, type, digest, headers);

    // Step 3: Verify signature (same as HttpFarClient)
    final var raw = signature.substring("sig1=:".length(), signature.length() - 1);
    final var verified = verifier.verify(components, raw, input, keys.verifying());
    assertThat(verified).as("HttpFarClient.verify() would accept this response").isTrue();
  }

  private void verify(final io.restassured.response.Response response) {
    final var body = response.body().asByteArray();
    final var signature = response.header("Signature");
    final var input = response.header("Signature-Input");
    final var digest = response.header("Content-Digest");

    assertThat(signature).as("Signature header").isNotNull();
    assertThat(input).as("Signature-Input header").isNotNull();
    assertThat(digest).as("Content-Digest header").isNotNull();

    assertThat(DigestCalculator.validate(body, digest))
        .as("Content-Digest must match body")
        .isTrue();

    final var type = response.header("Content-Type");
    final var headers = new LinkedHashMap<String, String>();
    final var resolver = response.header("Far-Resolver");
    final var namespace = response.header("Far-Namespace");
    if (resolver != null && input.contains("\"far-resolver\"")) {
      headers.put("far-resolver", resolver);
    }
    if (namespace != null && input.contains("\"far-namespace\"")) {
      headers.put("far-namespace", namespace);
    }

    final var signed = input.contains("\"content-type\"") ? response.header("Content-Type") : null;
    final var components = new MessageComponents(200, null, null, null, signed, digest, headers);

    final var raw = signature.substring("sig1=:".length(), signature.length() - 1);
    final var verified = verifier.verify(components, raw, input, keys.verifying());
    assertThat(verified).as("Signature must verify cryptographically").isTrue();
  }
}
