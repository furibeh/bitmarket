package com.bitmarket.catalog;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Inserts catalog rows with unique names and SKUs, so tests can share one database. */
final class CatalogFixtures {

  private final JdbcTemplate jdbc;

  CatalogFixtures(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  static String uniqueSku(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }

  UUID category(String name) {
    UUID id = UUID.randomUUID();
    jdbc.update("INSERT INTO category (id, name) VALUES (?, ?)", id, name + " " + id);
    return id;
  }

  String categoryName(UUID categoryId) {
    return jdbc.queryForObject("SELECT name FROM category WHERE id = ?", String.class, categoryId);
  }

  UUID product(UUID categoryId, String name, boolean active) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO product (id, category_id, name, description, active) VALUES (?, ?, ?, ?, ?)",
        id,
        categoryId,
        name,
        "Description of " + name,
        active);
    return id;
  }

  void variant(UUID productId, String sku, String attributesJson, long priceCop, boolean active) {
    jdbc.update(
        "INSERT INTO variant (id, product_id, sku, attributes, price_cop, active)"
            + " VALUES (?, ?, ?, ?::jsonb, ?, ?)",
        UUID.randomUUID(),
        productId,
        sku,
        attributesJson,
        priceCop,
        active);
  }
}
