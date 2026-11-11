package com.bitmarket.catalog.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "category")
class Category {

  @Id private UUID id;

  private String name;

  protected Category() {}

  String getName() {
    return name;
  }
}
