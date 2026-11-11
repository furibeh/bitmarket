package com.bitmarket.catalog.internal;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Public storefront catalog: only active products and active variants are visible (RN-07). */
@RestController
@RequestMapping(path = "/api/catalog/products", produces = MediaType.APPLICATION_JSON_VALUE)
class CatalogController {

  private final ProductRepository products;
  private final VariantRepository variants;

  CatalogController(ProductRepository products, VariantRepository variants) {
    this.products = products;
    this.variants = variants;
  }

  @GetMapping
  List<ProductSummary> list() {
    return products.findByActiveTrueOrderByName().stream()
        .map(
            product ->
                new ProductSummary(
                    product.getId(), product.getName(), product.getCategory().getName()))
        .toList();
  }

  @GetMapping("/{id}")
  ProductDetail detail(@PathVariable UUID id) {
    Product product =
        products
            .findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return new ProductDetail(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getCategory().getName(),
        variants.findByProductIdAndActiveTrueOrderBySku(id).stream()
            .map(
                variant ->
                    new VariantView(
                        variant.getSku(), variant.getAttributes(), variant.getPriceCop()))
            .toList());
  }

  record ProductSummary(
      @Schema(requiredMode = REQUIRED) UUID id,
      @Schema(requiredMode = REQUIRED) String name,
      @Schema(requiredMode = REQUIRED) String category) {}

  record ProductDetail(
      @Schema(requiredMode = REQUIRED) UUID id,
      @Schema(requiredMode = REQUIRED) String name,
      @Schema(requiredMode = REQUIRED) String description,
      @Schema(requiredMode = REQUIRED) String category,
      @Schema(requiredMode = REQUIRED) List<VariantView> variants) {}

  record VariantView(
      @Schema(requiredMode = REQUIRED) String sku,
      @Schema(requiredMode = REQUIRED) Map<String, String> attributes,
      @Schema(requiredMode = REQUIRED) long priceCop) {}
}
