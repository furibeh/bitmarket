package com.bitmarket;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModule.AllowedDependency;
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

  @Test
  @DisplayName("Cada módulo de negocio declara exactamente las dependencias de la arquitectura")
  void businessModulesDeclareArchitectureDependencies() {
    Map<String, Set<String>> declared =
        modules.stream()
            .filter(module -> !module.getIdentifier().toString().equals("shared"))
            .collect(
                toMap(
                    module -> module.getIdentifier().toString(),
                    module ->
                        module.getAllowedDependencies(modules).stream()
                            .map(AllowedDependency::getTargetModule)
                            .map(target -> target.getIdentifier().toString())
                            .collect(toSet())));

    assertThat(declared)
        .isEqualTo(
            Map.of(
                "customers", Set.of("shared"),
                "catalog", Set.of("shared"),
                "inventory", Set.of("catalog", "shared"),
                "cart", Set.of("customers", "catalog", "inventory", "shared"),
                "orders", Set.of("customers", "cart", "catalog", "inventory", "shared"),
                "payments", Set.of("orders", "shared"),
                "mcp", Set.of("orders", "inventory", "catalog", "shared")));
  }
}
