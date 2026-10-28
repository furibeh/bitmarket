package com.bitmarket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CiRedProbeTest {

  @Test
  @DisplayName("Prueba de CI: falla a propósito para comprobar que ci queda en rojo (#3)")
  void failsOnPurpose() {
    assertThat(false).isTrue();
  }
}
