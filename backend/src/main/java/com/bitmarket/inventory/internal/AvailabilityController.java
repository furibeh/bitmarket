package com.bitmarket.inventory.internal;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static java.util.stream.Collectors.toMap;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public availability per SKU. The BFF combines it with the catalog (RN-12). */
@RestController
class AvailabilityController {

  private final StockRepository stock;

  AvailabilityController(StockRepository stock) {
    this.stock = stock;
  }

  @GetMapping(path = "/api/inventory/availability", produces = MediaType.APPLICATION_JSON_VALUE)
  List<SkuAvailability> availability(@RequestParam List<String> skus) {
    Set<String> requested = new LinkedHashSet<>(skus);
    Map<String, Integer> available =
        stock.findBySkuIn(requested).stream().collect(toMap(Stock::getSku, Stock::getAvailable));

    // A SKU without a stock row has no stock yet (architecture.md §5.2).
    return requested.stream()
        .map(sku -> new SkuAvailability(sku, available.getOrDefault(sku, 0)))
        .toList();
  }

  record SkuAvailability(
      @Schema(requiredMode = REQUIRED) String sku,
      @Schema(requiredMode = REQUIRED) int available) {}
}
