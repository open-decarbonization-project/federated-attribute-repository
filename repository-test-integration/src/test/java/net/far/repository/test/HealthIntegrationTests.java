package net.far.repository.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HealthIntegrationTests {

  @Test
  void shouldReturnHealthy() {
    given()
        .when()
        .get("/v1/health")
        .then()
        .statusCode(200)
        .body("status", equalTo("healthy"))
        .body("timestamp", notNullValue())
        .body("namespaces.local", greaterThanOrEqualTo(1))
        .body("namespaces.total", greaterThanOrEqualTo(1));
  }

  @Test
  void shouldReturnAuthConfig() {
    given()
        .when()
        .get("/v1/auth")
        .then()
        .statusCode(200)
        .body("client", notNullValue());
  }

  @Test
  void shouldReturnPeerConfiguration() {
    given()
        .when()
        .get("/v1/peers/configuration")
        .then()
        .statusCode(200)
        .body("identity", notNullValue())
        .body("namespaces", notNullValue())
        .body("public_key.algorithm", equalTo("Ed25519"))
        .body("public_key.public_key_pem", startsWith("-----BEGIN PUBLIC KEY-----"));
  }

  @Test
  void shouldListNamespaces() {
    given()
        .when()
        .get("/v1/namespaces")
        .then()
        .statusCode(200)
        .body("namespaces[0].name", notNullValue())
        .body("namespaces[0].driver", notNullValue());
  }
}
