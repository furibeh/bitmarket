package com.bitmarket.inventory;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.bitmarket.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class AvailabilityApiIT {

  private static final ParameterizedTypeReference<List<Map<String, Object>>> JSON_LIST =
      new ParameterizedTypeReference<>() {};

  @Autowired RestTestClient client;
  @Autowired JdbcTemplate jdbc;

  @Test
  @DisplayName("CU-02: devuelve la cantidad disponible de cada SKU pedido")
  void returnsAvailablePerSku() {
    String first = stock("FIRST", 7);
    String second = stock("SECOND", 3);

    assertThat(availabilityOf(first, second)).isEqualTo(Map.of(first, 7, second, 3));
  }

  @Test
  @DisplayName("RN-12: una variante agotada devuelve disponible 0")
  void soldOutSkuIsZero() {
    String soldOut = stock("SOLD-OUT", 0);

    assertThat(availabilityOf(soldOut)).isEqualTo(Map.of(soldOut, 0));
  }

  @Test
  @DisplayName("RN-12: un SKU sin fila de stock devuelve disponible 0")
  void unknownSkuIsZero() {
    String unknown = "UNKNOWN-" + UUID.randomUUID();

    assertThat(availabilityOf(unknown)).isEqualTo(Map.of(unknown, 0));
  }

  @Test
  @DisplayName("CU-02: un SKU repetido en la consulta se devuelve una sola vez")
  void duplicatedSkuIsReturnedOnce() {
    String sku = stock("REPEATED", 4);

    assertThat(getAvailability(sku + "," + sku))
        .containsExactly(Map.of("sku", sku, "available", 4));
  }

  @Test
  @DisplayName("CU-02: acepta hasta 100 SKU en una consulta")
  void maximumNumberOfSkusIsAccepted() {
    assertThat(getAvailability(numberedSkus(100))).hasSize(100);
  }

  @Test
  @DisplayName("CU-02: más de 100 SKU en una consulta responde 400 problem+json")
  void moreThanMaximumSkusIsBadRequestProblem() {
    expectBadRequestProblem("/api/inventory/availability?skus={skus}", numberedSkus(101));
  }

  @Test
  @DisplayName("CU-02: el parámetro skus vacío responde 400 problem+json")
  void emptySkusParamIsBadRequestProblem() {
    expectBadRequestProblem("/api/inventory/availability?skus={skus}", "");
  }

  @Test
  @DisplayName("CU-02: un SKU en blanco en la consulta responde 400 problem+json")
  void blankSkuIsBadRequestProblem() {
    expectBadRequestProblem("/api/inventory/availability?skus={skus}", "FIRST,,SECOND");
  }

  @Test
  @DisplayName("CU-02: sin el parámetro skus responde 400 problem+json")
  void missingSkusParamIsBadRequestProblem() {
    client
        .get()
        .uri("/api/inventory/availability")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  @DisplayName("CU-02: la disponibilidad se consulta sin sesión")
  void availabilityIsPublic() {
    client
        .get()
        .uri("/api/inventory/availability?skus={skus}", stock("PUBLIC", 1))
        .exchange()
        .expectStatus()
        .isOk();
  }

  private void expectBadRequestProblem(String uriTemplate, String skus) {
    client
        .get()
        .uri(uriTemplate, skus)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
  }

  private static String numberedSkus(int count) {
    String prefix = "BULK-" + UUID.randomUUID() + "-";
    return IntStream.range(0, count).mapToObj(index -> prefix + index).collect(joining(","));
  }

  private String stock(String prefix, int available) {
    String sku = prefix + "-" + UUID.randomUUID();
    jdbc.update("INSERT INTO stock (sku, available) VALUES (?, ?)", sku, available);
    return sku;
  }

  private Map<String, Object> availabilityOf(String... skus) {
    return getAvailability(String.join(",", skus)).stream()
        .collect(toMap(item -> (String) item.get("sku"), item -> item.get("available")));
  }

  private List<Map<String, Object>> getAvailability(String skus) {
    return client
        .get()
        .uri("/api/inventory/availability?skus={skus}", skus)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(JSON_LIST)
        .returnResult()
        .getResponseBody();
  }
}
