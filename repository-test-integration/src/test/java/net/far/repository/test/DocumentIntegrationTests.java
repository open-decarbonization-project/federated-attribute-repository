package net.far.repository.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(user = "tester", roles = "repository-admin")
class DocumentIntegrationTests {

  @Test
  void shouldUpload() {
    given()
        .multiPart("filename", "report.txt")
        .multiPart("media", "text/plain")
        .multiPart(
            "content",
            "report.txt",
            "integration test content".getBytes(),
            "application/octet-stream")
        .when()
        .post("/v1/documents")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("filename", equalTo("report.txt"))
        .body("media", equalTo("text/plain"));
  }

  @Test
  void shouldListDocuments() {
    given()
        .multiPart("filename", "listing.txt")
        .multiPart("media", "text/plain")
        .multiPart(
            "content",
            "listing.txt",
            "document listing content".getBytes(),
            "application/octet-stream")
        .when()
        .post("/v1/documents")
        .then()
        .statusCode(201);

    given()
        .when()
        .get("/v1/documents")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(1)));
  }

  @Test
  void shouldDownloadContent() {
    final var payload = "downloadable content for test";

    final var id =
        given()
            .multiPart("filename", "download.txt")
            .multiPart("media", "text/plain")
            .multiPart("content", "download.txt", payload.getBytes(), "application/octet-stream")
            .when()
            .post("/v1/documents")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given().when().get("/v1/documents/{id}", id).then().statusCode(200).body(equalTo(payload));
  }
}
