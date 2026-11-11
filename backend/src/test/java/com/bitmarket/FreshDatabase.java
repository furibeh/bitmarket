package com.bitmarket;

import java.util.Map;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Creates an empty database in the test container and runs every Flyway migration on it, so a test
 * can inspect the seed data without the rows other tests insert in the shared database.
 */
public final class FreshDatabase {

  // Same default as application.yml: the role is shared by every database of the cluster.
  private static final String BFF_PASSWORD = "bitmarket_bff";

  private FreshDatabase() {}

  public static JdbcTemplate migrate(
      JdbcTemplate admin, PostgreSQLContainer postgres, String databaseName) {
    admin.execute("CREATE DATABASE " + databaseName);
    String url =
        "jdbc:postgresql://%s:%d/%s"
            .formatted(
                postgres.getHost(),
                postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                databaseName);

    Flyway.configure()
        .dataSource(url, postgres.getUsername(), postgres.getPassword())
        .placeholders(Map.of("bff_password", BFF_PASSWORD))
        .load()
        .migrate();

    return new JdbcTemplate(
        new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword()));
  }
}
