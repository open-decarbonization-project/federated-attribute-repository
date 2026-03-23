package net.far.repository.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourcePolicyIntegrationTests {

  private static String schemaId;

  @Test
  @Order(1)
  @TestSecurity(user = "admin", roles = "repository-admin")
  void setup() {
    schemaId =
        given()
            .contentType(ContentType.JSON)
            .body(
                """
{
  "namespace": "test",
  "name": "policy-mask-test",
  "fields": [
    {"name": "operator", "label": "Operator", "type": {"name": "string"}, "required": true, "position": 0},
    {"name": "volume", "label": "Volume", "type": {"name": "string"}, "required": true, "position": 1}
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
            [
              {"field": "operator", "policy": {"kind": "masked"}},
              {"field": "volume", "policy": {"kind": "public"}}
            ]
            """)
        .when()
        .put("/v1/schemas/" + schemaId + "/policies")
        .then()
        .statusCode(200);

    given()
        .contentType(ContentType.JSON)
        .body(
            """
{
  "namespace": "test",
  "identifier": "MASK-001",
  "schema": "%s",
  "attributes": {
    "operator": {"name": "operator", "value": "Acme Corp", "source": "test", "verified": true},
    "volume": {"name": "volume", "value": "5000", "source": "test", "verified": true}
  }
}
"""
                .formatted(schemaId))
        .when()
        .post("/v1/certificates")
        .then()
        .statusCode(201)
        .body("urn", equalTo("urn:far:test:MASK-001"))
        .body("schema", equalTo(schemaId));
  }

  @Test
  @Order(2)
  @TestSecurity(user = "admin", roles = "repository-admin")
  void shouldMaskFieldsForDelegatedAdminAccess() {
    given()
        .urlEncodingEnabled(false)
        .header("Far-Delegation-Chain", "https://repository-b.example.com")
        .when()
        .get("/v1/resources/urn:far:test:MASK-001")
        .then()
        .statusCode(200)
        .body("attributes.operator", notNullValue())
        .body("attributes.operator.value", equalTo("***"))
        .body("attributes.volume.value", equalTo("5000"));
  }

  @Test
  @Order(3)
  @TestSecurity(user = "admin", roles = "repository-admin")
  void shouldNotMaskFieldsForDirectAdminAccess() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .get("/v1/resources/urn:far:test:MASK-001")
        .then()
        .statusCode(200)
        .body("attributes.operator.value", equalTo("Acme Corp"))
        .body("attributes.volume.value", equalTo("5000"));
  }

  @Test
  @Order(4)
  @TestSecurity(user = "viewer")
  void shouldMaskFieldsForRegularUserAccess() {
    given()
        .urlEncodingEnabled(false)
        .when()
        .get("/v1/resources/urn:far:test:MASK-001")
        .then()
        .statusCode(200)
        .body("attributes.operator.value", equalTo("***"))
        .body("attributes.volume.value", equalTo("5000"));
  }
}
