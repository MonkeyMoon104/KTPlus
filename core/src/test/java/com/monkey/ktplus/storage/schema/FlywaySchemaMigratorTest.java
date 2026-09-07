package com.monkey.ktplus.storage.schema;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;

class FlywaySchemaMigratorTest {
    @Test
    void migratesFreshSqliteDatabaseToReviewClaims() throws Exception {
        Path dbFile = Files.createTempFile("ktplus-flyway-", ".db");
        Files.deleteIfExists(dbFile);
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
        try {
            new FlywaySchemaMigrator(dataSource).migrate();
            try (Connection connection = dataSource.getConnection();
                    ResultSet tables = connection.getMetaData().getTables(null, null, "kt_review_claims", null)) {
                assertTrue(tables.next());
            }
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    void baselinesLegacySchemaVersion() throws Exception {
        Path dbFile = Files.createTempFile("ktplus-flyway-legacy-", ".db");
        Files.deleteIfExists(dbFile);
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
        try (Connection connection = dataSource.getConnection()) {
            connection
                    .createStatement()
                    .executeUpdate(
                            "CREATE TABLE kt_schema (id VARCHAR(32) PRIMARY KEY, version INTEGER NOT NULL)");
            connection
                    .createStatement()
                    .executeUpdate("INSERT INTO kt_schema (id, version) VALUES ('main', 5)");
            connection
                    .createStatement()
                    .executeUpdate(
                            "CREATE TABLE kt_review_claims ("
                                    + "uuid VARCHAR(36) NOT NULL, "
                                    + "platform VARCHAR(16) NOT NULL, "
                                    + "account_key VARCHAR(64) NOT NULL, "
                                    + "claimed_at BIGINT NOT NULL, "
                                    + "PRIMARY KEY (uuid, platform))");
        }
        try {
            new FlywaySchemaMigrator(dataSource).migrate();
            try (Connection connection = dataSource.getConnection();
                    ResultSet history = connection
                            .createStatement()
                            .executeQuery(
                                    "SELECT version FROM kt_flyway_schema_history WHERE success = 1")) {
                assertTrue(history.next());
            }
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }
}
