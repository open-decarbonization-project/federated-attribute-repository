package net.far.repository.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(user = "tester")
class SchemaIntegrationTests {

  @Test
  void shouldCreate() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
{
  "namespace": "test",
  "name": "carbon-credit",
  "description": "Carbon credit schema",
  "fields": [
    {"name": "volume", "label": "Volume", "type": {"name": "quantity", "unit": "tCO2e"}, "required": true, "position": 0},
    {"name": "vintage", "label": "Vintage", "type": {"name": "string"}, "required": false, "position": 1}
  ]
}
""")
        .when()
        .post("/v1/schemas")
        .then()
        .statusCode(201)
        .body("namespace", equalTo("test"))
        .body("name", equalTo("carbon-credit"))
        .body("version", equalTo(1))
        .body("active", equalTo(true))
        .body("owner", equalTo("tester"))
        .body("fields", hasSize(2))
        .body("id", notNullValue());
  }

  @Test
  void shouldFindById() {
    final var id =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "namespace": "test",
                  "name": "find-test",
                  "fields": []
                }
                """)
            .when()
            .post("/v1/schemas")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given()
        .when()
        .get("/v1/schemas/" + id)
        .then()
        .statusCode(200)
        .body("id", equalTo(id))
        .body("name", equalTo("find-test"));
  }

  @Test
  void shouldList() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "namespace": "list-ns",
              "name": "schema-a",
              "fields": []
            }
            """)
        .when()
        .post("/v1/schemas")
        .then()
        .statusCode(201);

    given()
        .queryParam("namespace", "list-ns")
        .when()
        .get("/v1/schemas")
        .then()
        .statusCode(200)
        .body("", hasSize(greaterThanOrEqualTo(1)));
  }

  @Test
  void shouldUpdate() {
    final var id =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "namespace": "test",
                  "name": "update-test",
                  "description": "original",
                  "fields": []
                }
                """)
            .when()
            .post("/v1/schemas")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given()
        .contentType(ContentType.JSON)
        .body(
            """
{
  "description": "updated",
  "fields": [{"name": "status", "label": "Status", "type": {"name": "string"}, "required": true, "position": 0}],
  "active": true
}
""")
        .when()
        .put("/v1/schemas/" + id)
        .then()
        .statusCode(200)
        .body("version", equalTo(2))
        .body("description", equalTo("updated"))
        .body("fields", hasSize(1));
  }

  @Test
  void shouldReturnNotFound() {
    given().when().get("/v1/schemas/00000000-0000-0000-0000-000000000000").then().statusCode(404);
  }

  @Test
  void shouldCreateCertificateWithSchema() {
    final var id =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
{
  "namespace": "test",
  "name": "validated-schema",
  "fields": [
    {"name": "title", "label": "Title", "type": {"name": "string"}, "required": true, "position": 0}
  ]
}
""")
            .when()
            .post("/v1/schemas")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "namespace": "test",
              "identifier": "SCHEMA-CERT-001",
              "schema": "%s",
              "attributes": {
                "title": {
                  "name": "title",
                  "value": "Test Certificate",
                  "source": "test",
                  "verified": true
                }
              }
            }
            """
                .formatted(id))
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201)
        .body("schema", equalTo(id));
  }

  @Test
  void shouldRejectInvalidCertificateForSchema() {
    final var id =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
{
  "namespace": "test",
  "name": "strict-schema",
  "fields": [
    {"name": "volume", "label": "Volume", "type": {"name": "quantity", "unit": "tCO2e"}, "required": true, "position": 0}
  ]
}
""")
            .when()
            .post("/v1/schemas")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "namespace": "test",
              "identifier": "SCHEMA-CERT-002",
              "schema": "%s",
              "attributes": {}
            }
            """
                .formatted(id))
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(400);
  }
}
