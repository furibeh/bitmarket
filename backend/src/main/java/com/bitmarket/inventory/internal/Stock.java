package com.bitmarket.inventory.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Read model of the available stock. Immutable on purpose: stock only changes through conditional
 * UPDATE statements, never by loading and saving this entity (ADR-0003).
 */
@Entity
@Immutable
@Table(name = "stock")
class Stock {

  @Id private String sku;

  private int available;

  protected Stock() {}

  String getSku() {
    return sku;
  }

  int getAvailable() {
    return available;
  }
}
