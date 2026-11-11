package com.bitmarket.catalog.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

interface ProductRepository extends Repository<Product, UUID> {

  @EntityGraph(attributePaths = "category")
  List<Product> findByActiveTrueOrderByName();

  @EntityGraph(attributePaths = "category")
  Optional<Product> findByIdAndActiveTrue(UUID id);
}
