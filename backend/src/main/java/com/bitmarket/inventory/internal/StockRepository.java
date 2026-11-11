package com.bitmarket.inventory.internal;

import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.Repository;

interface StockRepository extends Repository<Stock, String> {

  List<Stock> findBySkuIn(Collection<String> skus);
}
