package net.far.repository.test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import net.far.resolver.signature.DigestCalculator;
import net.far.resolver.signature.KeyManager;
import net.far.resolver.signature.MessageComponents;
import net.far.resolver.signature.Verifier;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SignatureIntegrationTests {

  @Inject KeyManager keys;

  @Inject Verifier verifier;

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
              "identifier": "SIG-001",
              "attributes": {
                "volume": {
                  "name": "volume",
                  "value": "1000",
                  "source": "repository",
                  "verified": true
                }
              }
            }
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201)
        .body("urn", equalTo("urn:far:test:SIG-001"));

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "namespace": "test",
              "identifier": "SIG-002",
              "attributes": {
                "volume": {
                  "name": "volume",
                  "value": "2000",
                  "source": "repository",
                  "verified": true
                }
              }
            }
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201)
        .body("urn", equalTo("urn:far:test:SIG-002"));
  }

  @Test
  @Order(1)
  void shouldSignResolve() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .get("/v1/resources/urn:far:test:SIG-001")
        .then()
        .statusCode(200)
        .header("Signature", startsWith("sig1=:"))
        .header("Signature-Input", startsWith("sig1="))
        .header("Signature-Input", containsString("keyid="))
        .header("Signature-Input", containsString("alg=\"ed25519\""))
        .header("Content-Digest", startsWith("sha-256=:"));
  }

  @Test
  @Order(2)
  void shouldSignQuery() {
    given()
        .queryParam("$filter", "namespace eq 'test'")
        .when()
        .get("/v1/resources")
        .then()
        .statusCode(200)
        .header("Signature", startsWith("sig1=:"))
        .header("Signature-Input", startsWith("sig1="))
        .header("Content-Digest", startsWith("sha-256=:"));
  }

  @Test
  @Order(3)
  void shouldNotSignHead() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .head("/v1/resources/urn:far:test:SIG-001")
        .then()
        .statusCode(200)
        .header("Signature", nullValue())
        .header("Signature-Input", nullValue())
        .header("Content-Digest", nullValue());
  }

  @Test
  @Order(4)
  void shouldVerifyRoundtrip() {
    final var response =
        given()
            .urlEncodingEnabled(false)
            .when()
            .get("/v1/resources/urn:far:test:SIG-001")
            .then()
            .statusCode(200)
            .extract()
            .response();

    final var body = response.asByteArray();
    final var signature = response.header("Signature");
    final var input = response.header("Signature-Input");
    final var digest = response.header("Content-Digest");
    final var resolver = response.header("Far-Resolver");
    final var namespace = response.header("Far-Namespace");

    assertThat(body).isNotEmpty();
    assertThat(signature).isNotNull();
    assertThat(input).isNotNull();
    assertThat(digest).isNotNull();

    // Validate content digest matches response body
    final var valid = DigestCalculator.validate(body, digest);
    assertThat(valid).isTrue();

    // Build message components matching what the server filter produces
    final var headers = new LinkedHashMap<String, String>();
    if (resolver != null) {
      headers.put("far-resolver", resolver);
    }
    if (namespace != null) {
      headers.put("far-namespace", namespace);
    }

    // Recompute digest from body to match what the filter signed
    final var computed = DigestCalculator.compute(body);

    // Use the actual Content-Type header value (including charset)
    final var type = input.contains("\"content-type\"") ? response.header("Content-Type") : null;
    final var components = new MessageComponents(200, null, null, null, type, computed, headers);

    // Extract signature value: strip "sig1=:" prefix and ":" suffix
    final var raw = signature.substring("sig1=:".length(), signature.length() - 1);

    // Verify cryptographically
    final var verified = verifier.verify(components, raw, input, keys.verifying());
    assertThat(verified).isTrue();
  }

  @Test
  @Order(5)
  void shouldGenerateKey() {
    assertThat(keys).isNotNull();
    assertThat(keys.signing()).isNotNull();
    assertThat(keys.verifying()).isNotNull();
    assertThat(keys.id()).isNotNull();
    assertThat(keys.id()).contains("#");
    assertThat(keys.id()).startsWith("https://repository-test.far.example.com#");
  }

  @Test
  @Order(6)
  void shouldExposeConfiguration() {
    given()
        .when()
        .get("/v1/peers/configuration")
        .then()
        .statusCode(200)
        .body("public_key", notNullValue())
        .body("public_key.algorithm", equalTo("Ed25519"))
        .body("public_key.key_id", startsWith("https://repository-test.far.example.com#"))
        .body("public_key.public_key_pem", startsWith("-----BEGIN PUBLIC KEY-----"));
  }
}
