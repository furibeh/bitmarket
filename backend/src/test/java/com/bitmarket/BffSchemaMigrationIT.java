package com.bitmarket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "BFF_DB_PASSWORD=" + BffSchemaMigrationIT.BFF_PASSWORD)
@Import(TestcontainersConfiguration.class)
class BffSchemaMigrationIT {

  static final String BFF_PASSWORD = "test-bff-password";

  private static final String BFF_ROLE = "bitmarket_bff";
  private static final String PERMISSION_DENIED = "42501";
  private static final String PUBLIC_PROBE = "public.bff_isolation_probe";

  @Autowired JdbcTemplate jdbc;
  @Autowired PostgreSQLContainer postgres;

  @BeforeEach
  void createPublicTableOwnedByBackend() {
    jdbc.execute("CREATE TABLE IF NOT EXISTS " + PUBLIC_PROBE + " (id integer)");
  }

  @Test
  @DisplayName("RN-40: Flyway crea el esquema bff y el rol bitmarket_bff con permiso de login")
  void flywayCreatesBffSchemaAndRole() {
    Boolean schemaExists =
        jdbc.queryForObject("SELECT to_regnamespace('bff') IS NOT NULL", Boolean.class);

    assertThat(schemaExists).as("schema bff exists").isTrue();
    assertThat(
            jdbc.queryForList(
                "SELECT rolcanlogin FROM pg_roles WHERE rolname = ?", Boolean.class, BFF_ROLE))
        .containsExactly(true);
  }

  @Test
  @DisplayName("RN-40: el rol bitmarket_bff solo entra con la contraseña configurada")
  void bffRoleAuthenticatesOnlyWithConfiguredPassword() {
    assertThatCode(() -> connectAsBff(BFF_PASSWORD).close()).doesNotThrowAnyException();
    assertThatThrownBy(() -> connectAsBff("wrong-password").close())
        .isInstanceOf(SQLException.class);
  }

  @Test
  @DisplayName("RN-40: las tablas sin esquema que crea el rol bitmarket_bff quedan en bff")
  void bffRoleCreatesUnqualifiedTablesInBff() {
    assertThatCode(() -> executeAsBff("CREATE TABLE unqualified_probe (id integer)"))
        .doesNotThrowAnyException();

    assertThat(
            jdbc.queryForObject(
                "SELECT to_regclass('bff.unqualified_probe') IS NOT NULL", Boolean.class))
        .isTrue();
  }

  @Test
  @DisplayName("RN-40: el rol bitmarket_bff no puede leer tablas de public")
  void bffRoleCannotReadPublicTables() {
    assertPermissionDenied("SELECT * FROM " + PUBLIC_PROBE);
    assertPermissionDenied("SELECT * FROM public.flyway_schema_history");
  }

  @Test
  @DisplayName("RN-40: el rol bitmarket_bff no puede escribir ni crear tablas en public")
  void bffRoleCannotWriteOrCreateInPublic() {
    assertPermissionDenied("INSERT INTO " + PUBLIC_PROBE + " (id) VALUES (1)");
    assertPermissionDenied("CREATE TABLE public.bff_forbidden_probe (id integer)");
  }

  @Test
  @DisplayName("RN-40: la migración no falla si el rol ya existe en el clúster")
  void migrationSucceedsWhenRoleAlreadyExists() throws SQLException {
    assertThat(jdbc.queryForList("SELECT 1 FROM pg_roles WHERE rolname = ?", BFF_ROLE))
        .as("role already exists in the cluster")
        .hasSize(1);
    jdbc.execute("CREATE DATABASE second_database");
    String secondDatabaseUrl =
        "jdbc:postgresql://%s:%d/second_database"
            .formatted(
                postgres.getHost(), postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));

    Flyway.configure()
        .dataSource(secondDatabaseUrl, postgres.getUsername(), postgres.getPassword())
        .placeholders(Map.of("bff_password", BFF_PASSWORD))
        .load()
        .migrate();

    try (Connection connection =
            DriverManager.getConnection(
                secondDatabaseUrl, postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT to_regnamespace('bff') IS NOT NULL")) {
      result.next();
      assertThat(result.getBoolean(1)).as("schema bff exists in the second database").isTrue();
    }
  }

  private Connection connectAsBff(String password) throws SQLException {
    return DriverManager.getConnection(postgres.getJdbcUrl(), BFF_ROLE, password);
  }

  private void executeAsBff(String sql) throws SQLException {
    try (Connection connection = connectAsBff(BFF_PASSWORD);
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private void assertPermissionDenied(String sql) {
    assertThatThrownBy(() -> executeAsBff(sql))
        .asInstanceOf(throwable(SQLException.class))
        .extracting(SQLException::getSQLState)
        .isEqualTo(PERMISSION_DENIED);
  }
}
