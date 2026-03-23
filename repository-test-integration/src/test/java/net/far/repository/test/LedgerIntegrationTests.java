package net.far.repository.test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgerIntegrationTests {

  @Test
  @Order(0)
  @TestSecurity(user = "tester", roles = "repository-admin")
  void setup() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {"namespace":"test","identifier":"LED-001"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body("""
            {"namespace":"test","identifier":"LED-002"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);
  }

  @Test
  @Order(1)
  @TestSecurity(user = "tester", roles = "repository-admin")
  void shouldPublishEntry() {
    final var body =
        given()
            .urlEncodingEnabled(false)
            .when()
            .post("/v1/certificates/urn:far:test:LED-001/entries")
            .then()
            .statusCode(201)
            .extract()
            .body()
            .asString();

    assertThat(body).contains("database");
  }

  @Test
  @Order(2)
  @TestSecurity(user = "tester", roles = "repository-admin")
  void shouldListEntries() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .get("/v1/certificates/urn:far:test:LED-001/entries")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(1)))
        .body("[0].ledger", equalTo("database"))
        .body("[0].hash", startsWith("sha-256=:"))
        .body("[0].proof", notNullValue());
  }

  @Test
  @Order(3)
  @TestSecurity(user = "tester", roles = "repository-admin")
  void shouldComputeChainedProof() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .post("/v1/certificates/urn:far:test:LED-001/entries")
        .then()
        .statusCode(201);

    final var entries =
        given()
            .urlEncodingEnabled(false)
            .when()
            .get("/v1/certificates/urn:far:test:LED-001/entries")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();

    final var count = entries.getList("$").size();
    assertThat(count).isGreaterThanOrEqualTo(2);

    final var first = entries.getString("[" + (count - 1) + "].hash");
    final var second = entries.getString("[" + (count - 2) + "].proof");
    assertThat(first).isNotEqualTo(second);
    assertThat(second).startsWith("sha-256=:");
  }

  @Test
  @Order(4)
  @TestSecurity(user = "tester", roles = "repository-admin")
  void shouldPublishDifferentCertificate() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .post("/v1/certificates/urn:far:test:LED-002/entries")
        .then()
        .statusCode(201);

    given()
        .urlEncodingEnabled(false)
        .when()
        .get("/v1/certificates/urn:far:test:LED-002/entries")
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].hash", startsWith("sha-256=:"));
  }

  @Test
  @Order(5)
  @TestSecurity(user = "viewer")
  void shouldRejectPublishWithoutAdminRole() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .post("/v1/certificates/urn:far:test:LED-001/entries")
        .then()
        .statusCode(403);
  }
}
