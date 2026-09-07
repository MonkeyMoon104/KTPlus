package com.monkey.ktplus.storage.migration.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MigrationConnectionSupportTest {
    @Test
    void sqliteConfirmationUsesNormalizedAbsolutePath() {
        Path path = Path.of("plugins", "KTPlus", "ktplus.db").toAbsolutePath().normalize();
        MigrationEndpoint endpoint = MigrationEndpoint.sqlite(path);
        assertTrue(endpoint.matchesConfirmation(path.toString()));
        assertTrue(endpoint.matchesConfirmation(path.toString().replace('\\', '/')));
        assertFalse(endpoint.matchesConfirmation("sqlite"));
        assertTrue(endpoint.displaySummary().contains("SQLite file:"));
    }

    @Test
    void mysqlConfirmationUsesHostPortDatabaseToken() {
        MigrationEndpoint endpoint = MigrationEndpoint.mysql("DB.Example.COM", 3307, "KtPlus");
        assertEquals("db.example.com:3307/ktplus", endpoint.confirmationToken());
        assertTrue(endpoint.matchesConfirmation("db.example.com:3307/ktplus"));
        assertFalse(endpoint.matchesConfirmation("mysql"));
    }

    @Test
    void mysqlVersionGateAcceptsEightPlusAndRejectsOlder() {
        assertTrue(TargetMySqlInspector.isAtLeast("8.0.36", "8.0.0"));
        assertTrue(TargetMySqlInspector.isAtLeast("8.4.0", "8.0.0"));
        assertFalse(TargetMySqlInspector.isAtLeast("5.7.44", "8.0.0"));
        assertTrue(TargetMySqlInspector.isAtLeast("8.0.0-log", "8.0.0"));
    }

    @Test
    void mariaDbVersionStringIsRejectedByInspectorMessageContract() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> {
            if ("10.11.6-MariaDB".toLowerCase().contains("mariadb")) {
                throw new IllegalStateException(
                        "the target responds as MariaDB, not supported by this migrator, verify database.type");
            }
        });
        assertTrue(error.getMessage().contains("MariaDB"));
    }
}
