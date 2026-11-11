package com.bitmarket.catalog;

import static com.bitmarket.catalog.CatalogFixtures.uniqueSku;
import static org.assertj.core.api.Assertions.assertThat;

import com.bitmarket.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class CatalogApiIT {

  private static final ParameterizedTypeReference<List<Map<String, Object>>> JSON_LIST =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
      new ParameterizedTypeReference<>() {};

  @Autowired RestTestClient client;
  @Autowired JdbcTemplate jdbc;

  private CatalogFixtures fixtures;
  private UUID category;

  @BeforeEach
  void createCategory() {
    fixtures = new CatalogFixtures(jdbc);
    category = fixtures.category("Laptops");
  }

  @Test
  @DisplayName("RN-03: la lista muestra cada producto activo con su nombre y su categoría")
  void listShowsActiveProductWithNameAndCategory() {
    UUID product = fixtures.product(category, "Laptop Pro 14", true);

    assertThat(listProducts())
        .filteredOn(item -> product.toString().equals(item.get("id")))
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.get("name")).isEqualTo("Laptop Pro 14");
              assertThat(item.get("category")).isEqualTo(fixtures.categoryName(category));
            });
  }

  @Test
  @DisplayName("RN-07: la lista no muestra los productos inactivos")
  void listHidesInactiveProducts() {
    UUID active = fixtures.product(category, "Active laptop", true);
    UUID inactive = fixtures.product(category, "Retired laptop", false);

    assertThat(listProducts())
        .extracting(item -> item.get("id"))
        .contains(active.toString())
        .doesNotContain(inactive.toString());
  }

  @Test
  @DisplayName("CU-02: la lista de productos se consulta sin sesión")
  void listIsPublic() {
    client.get().uri("/api/catalog/products").exchange().expectStatus().isOk();
  }

  @Test
  @DisplayName("RN-01, RN-05: el detalle muestra SKU, atributos y precio de cada variante activa")
  void detailShowsSkuAttributesAndPriceOfActiveVariants() {
    UUID product = fixtures.product(category, "Laptop Pro 14", true);
    String sku = uniqueSku("LAPTOP-512");
    fixtures.variant(
        product, sku, "{\"capacidad\":\"512GB\",\"color\":\"negro\"}", 5_499_000L, true);

    Map<String, Object> detail = getProduct(product);

    assertThat(detail.get("name")).isEqualTo("Laptop Pro 14");
    assertThat(detail.get("category")).isEqualTo(fixtures.categoryName(category));
    assertThat(variantsOf(detail))
        .singleElement()
        .satisfies(
            variant -> {
              assertThat(variant.get("sku")).isEqualTo(sku);
              assertThat(variant.get("attributes"))
                  .isEqualTo(Map.of("capacidad", "512GB", "color", "negro"));
              assertThat(variant.get("priceCop")).isEqualTo(5_499_000);
            });
  }

  @Test
  @DisplayName("RN-07: el detalle no muestra las variantes inactivas")
  void detailHidesInactiveVariants() {
    UUID product = fixtures.product(category, "Laptop Air 13", true);
    String activeSku = uniqueSku("AIR-256");
    String inactiveSku = uniqueSku("AIR-128");
    fixtures.variant(product, activeSku, "{}", 3_999_000L, true);
    fixtures.variant(product, inactiveSku, "{}", 3_499_000L, false);

    assertThat(variantsOf(getProduct(product)))
        .extracting(variant -> variant.get("sku"))
        .containsExactly(activeSku);
  }

  @Test
  @DisplayName("RN-02: el precio viaja como entero en COP, sin decimales y sin perder dígitos")
  void detailReturnsPriceAsIntegerBeyondIntRange() {
    UUID product = fixtures.product(category, "Workstation", true);
    fixtures.variant(product, uniqueSku("WORKSTATION"), "{}", 3_000_000_000L, true);

    String json =
        client
            .get()
            .uri("/api/catalog/products/{id}", product)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(json).containsPattern("\"priceCop\"\\s*:\\s*3000000000\\s*[,}]");
  }

  @Test
  @DisplayName("RN-07: un producto activo sin variantes activas se muestra sin variantes")
  void detailOfProductWithoutActiveVariantsHasNoVariants() {
    UUID product = fixtures.product(category, "Laptop without variants", true);
    fixtures.variant(product, uniqueSku("ONLY-INACTIVE"), "{}", 2_999_000L, false);

    assertThat(variantsOf(getProduct(product))).isEmpty();
  }

  @Test
  @DisplayName("CU-02: el detalle de un producto se consulta sin sesión")
  void detailIsPublic() {
    UUID product = fixtures.product(category, "Public laptop", true);

    client.get().uri("/api/catalog/products/{id}", product).exchange().expectStatus().isOk();
  }

  @Test
  @DisplayName("RN-07: el detalle de un producto inactivo responde 404 problem+json")
  void detailOfInactiveProductIsNotFoundProblem() {
    UUID product = fixtures.product(category, "Retired laptop", false);

    client
        .get()
        .uri("/api/catalog/products/{id}", product)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  @DisplayName("CU-02: el detalle de un producto inexistente responde 404 problem+json")
  void detailOfUnknownProductIsNotFoundProblem() {
    client
        .get()
        .uri("/api/catalog/products/{id}", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  @DisplayName("CU-02: un identificador de producto que no es UUID responde 400 problem+json")
  void detailOfMalformedIdIsBadRequestProblem() {
    client
        .get()
        .uri("/api/catalog/products/{id}", "not-a-uuid")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
  }

  private List<Map<String, Object>> listProducts() {
    return client
        .get()
        .uri("/api/catalog/products")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(JSON_LIST)
        .returnResult()
        .getResponseBody();
  }

  private Map<String, Object> getProduct(UUID id) {
    return client
        .get()
        .uri("/api/catalog/products/{id}", id)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(JSON_OBJECT)
        .returnResult()
        .getResponseBody();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> variantsOf(Map<String, Object> detail) {
    return (List<Map<String, Object>>) detail.get("variants");
  }
}
