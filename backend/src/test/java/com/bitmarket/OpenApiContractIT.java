package com.bitmarket;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Keeps {@code backend/openapi.json}, the file the frontend generates its types from, equal to the
 * contract springdoc serves. Run with {@code -Dopenapi.update=true} to rewrite the file.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class OpenApiContractIT {

  // Maven runs the tests from the backend/ directory.
  private static final Path COMMITTED_CONTRACT = Path.of("openapi.json");
  private static final String UPDATE_PROPERTY = "openapi.update";
  private static final String PROBLEM_DETAIL_SCHEMA = "#/components/schemas/ProblemDetail";

  private final ObjectMapper json = new ObjectMapper();

  @Autowired RestTestClient client;

  @Test
  @DisplayName("El OpenAPI describe la lista de productos del catálogo")
  void apiDocsDescribeProductList() {
    assertThat(generatedContract().at("/paths/~1api~1catalog~1products/get").isObject()).isTrue();
  }

  @Test
  @DisplayName("El OpenAPI describe el detalle de un producto")
  void apiDocsDescribeProductDetail() {
    assertThat(generatedContract().at("/paths/~1api~1catalog~1products~1{id}/get").isObject())
        .isTrue();
  }

  @Test
  @DisplayName("El OpenAPI describe la consulta de disponibilidad")
  void apiDocsDescribeAvailability() {
    assertThat(generatedContract().at("/paths/~1api~1inventory~1availability/get").isObject())
        .isTrue();
  }

  @Test
  @DisplayName("El OpenAPI da a cada operación un identificador propio y estable")
  void apiDocsNameEachOperation() {
    JsonNode paths = generatedContract().at("/paths");

    assertThat(paths.at("/~1api~1catalog~1products/get/operationId").asString())
        .isEqualTo("listProducts");
    assertThat(paths.at("/~1api~1catalog~1products~1{id}/get/operationId").asString())
        .isEqualTo("getProduct");
    assertThat(paths.at("/~1api~1inventory~1availability/get/operationId").asString())
        .isEqualTo("getAvailability");
  }

  @Test
  @DisplayName("El OpenAPI declara los errores 400 y 404 del detalle como problem+json")
  void apiDocsDeclareProblemResponsesOfProductDetail() {
    JsonNode responses =
        generatedContract().at("/paths/~1api~1catalog~1products~1{id}/get/responses");

    assertThat(problemSchemaOf(responses, "400")).isEqualTo(PROBLEM_DETAIL_SCHEMA);
    assertThat(problemSchemaOf(responses, "404")).isEqualTo(PROBLEM_DETAIL_SCHEMA);
  }

  @Test
  @DisplayName("El OpenAPI declara el error 400 de la disponibilidad como problem+json")
  void apiDocsDeclareProblemResponseOfAvailability() {
    JsonNode responses =
        generatedContract().at("/paths/~1api~1inventory~1availability/get/responses");

    assertThat(problemSchemaOf(responses, "400")).isEqualTo(PROBLEM_DETAIL_SCHEMA);
  }

  @Test
  @DisplayName("El archivo openapi.json versionado coincide con el OpenAPI generado")
  void committedContractMatchesGenerated() throws IOException {
    JsonNode generated = generatedContract();
    if (Boolean.getBoolean(UPDATE_PROPERTY)) {
      Files.writeString(
          COMMITTED_CONTRACT,
          json.writerWithDefaultPrettyPrinter().writeValueAsString(generated) + "\n");
    }

    assertThat(COMMITTED_CONTRACT).as("committed OpenAPI contract").exists();
    assertThat(json.readTree(Files.readString(COMMITTED_CONTRACT))).isEqualTo(generated);
  }

  private static String problemSchemaOf(JsonNode responses, String status) {
    return responses
        .at("/" + status + "/content/application~1problem+json/schema/$ref")
        .asString("");
  }

  /** The served contract without {@code servers}, which holds the random port of the test. */
  private JsonNode generatedContract() {
    String body =
        client
            .get()
            .uri("/v3/api-docs")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    JsonNode contract = json.readTree(body);
    if (contract instanceof ObjectNode root) {
      root.remove("servers");
    }
    return contract;
  }
}
