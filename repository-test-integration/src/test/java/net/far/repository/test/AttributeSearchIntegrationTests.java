package net.far.repository.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestSecurity(user = "searcher")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AttributeSearchIntegrationTests {

  @Test
  @Order(0)
  void setup() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "namespace": "attrsearch",
              "identifier": "SRCH-001",
              "attributes": {
                "region": {
                  "name": "region",
                  "value": "EU",
                  "source": "repository",
                  "verified": true
                },
                "project": {
                  "name": "project",
                  "value": "WindFarm-Alpha",
                  "source": "repository",
                  "verified": true
                },
                "volume": {
                  "name": "volume",
                  "value": "1000",
                  "source": "repository",
                  "verified": true
                },
                "vintage": {
                  "name": "vintage",
                  "value": "2024",
                  "source": "repository",
                  "verified": true
                }
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
              "namespace": "attrsearch",
              "identifier": "SRCH-002",
              "attributes": {
                "region": {
                  "name": "region",
                  "value": "US",
                  "source": "repository",
                  "verified": true
                },
                "project": {
                  "name": "project",
                  "value": "SolarGrid-Beta",
                  "source": "repository",
                  "verified": true
                },
                "volume": {
                  "name": "volume",
                  "value": "500",
                  "source": "repository",
                  "verified": true
                },
                "vintage": {
                  "name": "vintage",
                  "value": "2023",
                  "source": "repository",
                  "verified": true
                }
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
              "namespace": "attrsearch",
              "identifier": "SRCH-003",
              "attributes": {
                "region": {
                  "name": "region",
                  "value": "EU",
                  "source": "repository",
                  "verified": true
                },
                "project": {
                  "name": "project",
                  "value": "HydroPlant-Gamma",
                  "source": "repository",
                  "verified": true
                },
                "volume": {
                  "name": "volume",
                  "value": "2000",
                  "source": "repository",
                  "verified": true
                },
                "vintage": {
                  "name": "vintage",
                  "value": "2024",
                  "source": "repository",
                  "verified": true
                }
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
  void shouldSearchByCustomAttributeEquals() {
    given()
        .queryParam("$filter", "namespace eq 'attrsearch' and region eq 'EU'")
        .when()
        .get("/v1/certificates")
        .then()
        .statusCode(200)
        .body("count", equalTo(2))
        .body("value.size()", equalTo(2))
        .body("value.identifier", hasItems("SRCH-001", "SRCH-003"));
  }

  @Test
  @Order(2)
  void shouldSearchByCustomAttributeContains() {
    given()
        .queryParam("$filter", "namespace eq 'attrsearch' and contains(project, 'Wind')")
        .when()
        .get("/v1/certificates")
        .then()
        .statusCode(200)
        .body("count", equalTo(1))
        .body("value[0].identifier", equalTo("SRCH-001"));
  }

  @Test
  @Order(3)
  void shouldSearchByCustomAttributeContainsCaseInsensitive() {
    given()
        .queryParam("$filter", "namespace eq 'attrsearch' and contains(project, 'solar')")
        .when()
        .get("/v1/certificates")
        .then()
        .statusCode(200)
        .body("count", equalTo(1))
        .body("value[0].identifier", equalTo("SRCH-002"));
  }

  @Test
  @Order(4)
  void shouldSearchByCustomAttributeNumericGreaterThan() {
    given()
        .queryParam("$filter", "namespace eq 'attrsearch' and volume gt 500")
        .when()
        .get("/v1/certificates")
        .then()
        .statusCode(200)
        .body("count", equalTo(2))
        .body("value.identifier", hasItems("SRCH-001", "SRCH-003"));
  }

  @Test
  @Order(5)
  void shouldSearchByCustomAttributeNumericLessThan() {
    given()
        .queryParam("$filter", "namespace eq 'attrsearch' and volume lt 1000")
        .when()
        .get("/v1/certificates")
        .then()
        .statusCode(200)
        .body("count", equalTo(1))
        .body("value[0].identifier", equalTo("SRCH-002"));
  }

  @Test
  @Order(6)
  void shouldSearchByMultipleCustomAttributes() {
    given()
        .queryParam("$filter", "namespace eq 'attrsearch' and region eq 'EU' and volume gt 1500")
        .when()
        .get("/v1/certificates")
        .then()
        .statusCode(200)
        .body("count", equalTo(1))
        .body("value[0].identifier", equalTo("SRCH-003"));
  }

  @Test
  @Order(7)
  void shouldSearchByVintageAttribute() {
    given()
        .queryParam("$filter", "namespace eq 'attrsearch' and vintage eq '2024'")
        .when()
        .get("/v1/certificates")
        .then()
        .statusCode(200)
        .body("count", equalTo(2))
        .body("value.identifier", hasItems("SRCH-001", "SRCH-003"));
  }

  @Test
  @Order(8)
  void shouldReturnEmptyForNonMatchingCustomAttribute() {
    given()
        .queryParam("$filter", "namespace eq 'attrsearch' and region eq 'APAC'")
        .when()
        .get("/v1/certificates")
        .then()
        .statusCode(200)
        .body("count", equalTo(0))
        .body("value.size()", equalTo(0));
  }

  @Test
  @Order(9)
  void shouldSearchByCustomAttributeWithBuiltinFilter() {
    given()
        .queryParam("$filter", "namespace eq 'attrsearch' and status eq 'DRAFT' and region eq 'US'")
        .when()
        .get("/v1/certificates")
        .then()
        .statusCode(200)
        .body("count", equalTo(1))
        .body("value[0].identifier", equalTo("SRCH-002"));
  }
}
