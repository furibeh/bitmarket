package com.bitmarket.catalog.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

interface VariantRepository extends Repository<Variant, UUID> {

  List<Variant> findByProductIdAndActiveTrueOrderBySku(UUID productId);
}
