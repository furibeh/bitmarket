package com.bitmarket.catalog.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "product")
class Product {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id")
  private Category category;

  private String name;

  private String description;

  private boolean active;

  protected Product() {}

  UUID getId() {
    return id;
  }

  Category getCategory() {
    return category;
  }

  String getName() {
    return name;
  }

  String getDescription() {
    return description;
  }
}
