package com.bitmarket.catalog;

import static com.bitmarket.catalog.CatalogFixtures.uniqueSku;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bitmarket.FreshDatabase;
import com.bitmarket.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestInstance(Lifecycle.PER_CLASS)
class CatalogMigrationIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired PostgreSQLContainer postgres;

  private JdbcTemplate freshDatabase;
  private CatalogFixtures fixtures;

  @BeforeAll
  void migrateFreshDatabase() {
    freshDatabase = FreshDatabase.migrate(jdbc, postgres, "catalog_seed");
    fixtures = new CatalogFixtures(jdbc);
  }

  @Test
  @DisplayName("Flyway crea las tablas category, product y variant del catálogo")
  void flywayCreatesCatalogTables() {
    assertThat(tableExists("category")).as("table category exists").isTrue();
    assertThat(tableExists("product")).as("table product exists").isTrue();
    assertThat(tableExists("variant")).as("table variant exists").isTrue();
  }

  @Test
  @DisplayName("RN-01: la semilla trae varios productos activos con al menos dos variantes activas")
  void seedHasSeveralActiveProductsWithAtLeastTwoActiveVariants() {
    Integer productsWithTwoVariants =
        freshDatabase.queryForObject(
            """
            SELECT count(*) FROM product p
            WHERE p.active
              AND (SELECT count(*) FROM variant v WHERE v.product_id = p.id AND v.active) >= 2
            """,
            Integer.class);

    assertThat(productsWithTwoVariants).isGreaterThanOrEqualTo(2);
  }

  @Test
  @DisplayName("RN-07: la semilla trae al menos un producto inactivo")
  void seedHasAnInactiveProduct() {
    Integer inactiveProducts =
        freshDatabase.queryForObject(
            "SELECT count(*) FROM product WHERE NOT active", Integer.class);

    assertThat(inactiveProducts).isPositive();
  }

  @Test
  @DisplayName("RN-03: un producto sin categoría se rechaza")
  void rejectsProductWithoutCategory() {
    assertThatThrownBy(() -> fixtures.product(null, "Product without category", true))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("RN-05: un SKU repetido se rechaza aunque sea de otro producto")
  void rejectsDuplicateSku() {
    UUID category = fixtures.category("Phones");
    UUID firstProduct = fixtures.product(category, "First phone", true);
    UUID secondProduct = fixtures.product(category, "Second phone", true);
    String sku = uniqueSku("DUPLICATED");
    fixtures.variant(firstProduct, sku, "{}", 1_000_000L, true);

    assertThatThrownBy(() -> fixtures.variant(secondProduct, sku, "{}", 2_000_000L, true))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("RN-02: un precio de cero se rechaza")
  void rejectsZeroPrice() {
    UUID product = fixtures.product(fixtures.category("Phones"), "Free phone", true);

    assertThatThrownBy(() -> fixtures.variant(product, uniqueSku("FREE"), "{}", 0L, true))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("RN-02: un precio negativo se rechaza")
  void rejectsNegativePrice() {
    UUID product = fixtures.product(fixtures.category("Phones"), "Negative phone", true);

    assertThatThrownBy(() -> fixtures.variant(product, uniqueSku("NEGATIVE"), "{}", -1L, true))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private boolean tableExists(String table) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class, "public." + table));
  }
}
