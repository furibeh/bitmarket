package com.bitmarket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class BitmarketApplicationIT {

  @Autowired RestTestClient client;
  @Autowired JdbcTemplate jdbc;

  @Test
  @DisplayName("Health responde 200 con status UP y la base de datos UP")
  void healthReportsUpIncludingDatabase() {
    client
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("UP")
        .jsonPath("$.components.db.status")
        .isEqualTo("UP");
  }

  @Test
  @DisplayName("Flyway aplica la migración de línea base V1")
  void flywayAppliesBaselineMigration() {
    Boolean historyExists =
        jdbc.queryForObject(
            "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL", Boolean.class);
    assertThat(historyExists).as("flyway_schema_history exists").isTrue();

    Integer appliedV1 =
        jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success",
            Integer.class);
    assertThat(appliedV1).isEqualTo(1);
  }
}
