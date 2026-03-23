package net.far.repository.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(user = "tester")
class CertificateIntegrationTests {

  @Test
  void shouldCreate() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"namespace":"test","identifier":"INT-001"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201)
        .body("urn", equalTo("urn:far:test:INT-001"))
        .body("status", equalTo("DRAFT"))
        .body("namespace", equalTo("test"))
        .body("identifier", equalTo("INT-001"))
        .body("owner", equalTo("tester"))
        .body("created", notNullValue())
        .body("integrity", notNullValue());
  }

  @Test
  void shouldFindByUrn() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"namespace":"test","identifier":"INT-002"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);

    given()
        .urlEncodingEnabled(false)
        .when()
        .get("/v1/certificates/urn:far:test:INT-002")
        .then()
        .statusCode(200)
        .body("urn", equalTo("urn:far:test:INT-002"))
        .body("namespace", equalTo("test"))
        .body("identifier", equalTo("INT-002"))
        .body("owner", equalTo("tester"));
  }

  @Test
  void shouldCheckExists() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"namespace":"test","identifier":"INT-003"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);

    given()
        .urlEncodingEnabled(false)
        .when()
        .head("/v1/certificates/urn:far:test:INT-003")
        .then()
        .statusCode(200);
  }

  @Test
  void shouldReturnNotFound() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .head("/v1/certificates/urn:far:test:MISSING")
        .then()
        .statusCode(404);
  }

  @Test
  void shouldUpdate() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"namespace":"test","identifier":"INT-004"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201)
        .body("status", equalTo("DRAFT"));

    given()
        .urlEncodingEnabled(false)
        .contentType(ContentType.JSON)
        .body(
            """
            {"status":"ACTIVE"}
            """)
        .when()
        .put("/v1/certificates/urn:far:test:INT-004")
        .then()
        .statusCode(200)
        .body("status", equalTo("ACTIVE"))
        .body("urn", equalTo("urn:far:test:INT-004"));
  }

  @Test
  void shouldRetire() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"namespace":"test","identifier":"INT-005"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);

    given()
        .urlEncodingEnabled(false)
        .when()
        .delete("/v1/certificates/urn:far:test:INT-005")
        .then()
        .statusCode(200)
        .body("status", equalTo("RETIRED"))
        .body("urn", equalTo("urn:far:test:INT-005"));
  }

  @Test
  void shouldRejectDuplicate() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"namespace":"test","identifier":"INT-006"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"namespace":"test","identifier":"INT-006"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(409);
  }

  @Test
  void shouldSearch() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"namespace":"test","identifier":"INT-007"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"namespace":"test","identifier":"INT-008"}
            """)
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201);

    given()
        .queryParam("$top", 10)
        .when()
        .get("/v1/certificates")
        .then()
        .statusCode(200)
        .body("count", greaterThanOrEqualTo(2))
        .body("value", notNullValue());
  }

  @Test
  void shouldCreateWithAttributes() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "namespace": "test",
              "identifier": "INT-009",
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
        .body("urn", equalTo("urn:far:test:INT-009"))
        .body("attributes", notNullValue())
        .body("attributes.volume", notNullValue())
        .body("attributes.volume.name", equalTo("volume"));
  }
}
