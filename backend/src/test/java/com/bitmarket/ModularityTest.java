package com.bitmarket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

  private final ApplicationModules modules = ApplicationModules.of(BitmarketApplication.class);

  @Test
  @DisplayName("Los módulos respetan sus límites y dependencias permitidas")
  void verifiesModuleBoundaries() {
    modules.verify();
  }

  @Test
  @DisplayName("Detecta los siete módulos de negocio y shared")
  void detectsSevenBusinessModulesAndShared() {
    assertThat(modules.stream().map(ApplicationModule::getIdentifier).map(Object::toString))
        .containsExactlyInAnyOrder(
            "customers", "catalog", "inventory", "cart", "orders", "payments", "mcp", "shared");
  }
}
