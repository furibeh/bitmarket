package com.bitmarket.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bitmarket.FreshDatabase;
import com.bitmarket.TestcontainersConfiguration;
import java.util.List;
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
class StockMigrationIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired PostgreSQLContainer postgres;

  private JdbcTemplate freshDatabase;

  @BeforeAll
  void migrateFreshDatabase() {
    freshDatabase = FreshDatabase.migrate(jdbc, postgres, "stock_seed");
  }

  @Test
  @DisplayName("Flyway crea la tabla stock del inventario")
  void flywayCreatesStockTable() {
    Boolean exists =
        jdbc.queryForObject("SELECT to_regclass('public.stock') IS NOT NULL", Boolean.class);

    assertThat(exists).as("table stock exists").isTrue();
  }

  @Test
  @DisplayName("RN-30: un stock disponible negativo se rechaza")
  void rejectsNegativeAvailable() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO stock (sku, available) VALUES (?, ?)",
                    "NEGATIVE-" + UUID.randomUUID(),
                    -1))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("RN-12: la semilla trae una variante activa con stock disponible 0")
  void seedHasAnActiveVariantWithZeroStock() {
    Integer soldOutVariants =
        freshDatabase.queryForObject(
            """
            SELECT count(*) FROM variant v
            JOIN product p ON p.id = v.product_id
            JOIN stock s ON s.sku = v.sku
            WHERE v.active AND p.active AND s.available = 0
            """,
            Integer.class);

    assertThat(soldOutVariants).isPositive();
  }

  @Test
  @DisplayName("RN-01: cada variante de la semilla tiene su fila de stock")
  void everySeedVariantHasAStockRow() {
    List<String> variantSkus = freshDatabase.queryForList("SELECT sku FROM variant", String.class);
    List<String> stockSkus = freshDatabase.queryForList("SELECT sku FROM stock", String.class);

    assertThat(stockSkus).isNotEmpty().containsAll(variantSkus);
  }
}
