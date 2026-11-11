package com.bitmarket.inventory.internal;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static java.util.stream.Collectors.toMap;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Public availability per SKU. The BFF combines it with the catalog (RN-12). */
@RestController
class AvailabilityController {

  private static final int MAX_SKUS = 100;

  private final StockRepository stock;

  AvailabilityController(StockRepository stock) {
    this.stock = stock;
  }

  @GetMapping(path = "/api/inventory/availability", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(operationId = "getAvailability")
  @ApiResponse(responseCode = "200")
  @ApiResponse(
      responseCode = "400",
      description = "skus is missing, empty, has a blank SKU or has more than 100 SKUs",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(implementation = ProblemDetail.class)))
  List<SkuAvailability> availability(@RequestParam List<String> skus) {
    if (skus.isEmpty() || skus.size() > MAX_SKUS || skus.stream().anyMatch(String::isBlank)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "skus must have between 1 and %d non-blank SKUs".formatted(MAX_SKUS));
    }
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
