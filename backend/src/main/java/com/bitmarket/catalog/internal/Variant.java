package com.bitmarket.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "variant")
class Variant {

  @Id private UUID id;

  @Column(name = "product_id")
  private UUID productId;

  private String sku;

  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, String> attributes;

  @Column(name = "price_cop")
  private long priceCop;

  private boolean active;

  protected Variant() {}

  String getSku() {
    return sku;
  }

  Map<String, String> getAttributes() {
    return attributes;
  }

  long getPriceCop() {
    return priceCop;
  }
}
