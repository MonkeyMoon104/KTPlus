package com.monkey.ktplus.storage.driver;

import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

final class SqlDriverCatalog {
    private SqlDriverCatalog() {}

    static @Nullable DriverEntry entryFor(String dialect) {
        Objects.requireNonNull(dialect, "dialect");
        String type = dialect.toLowerCase(Locale.ROOT);
        if ("sqlite".equals(type)) {
            return new DriverEntry("sqlite-jdbc", "SQLite JDBC", "org.sqlite.JDBC");
        }
        if ("mysql".equals(type)) {
            return new DriverEntry(
                    "mysql-connector",
                    "MySQL Connector/J",
                    "com.mysql.cj.jdbc.Driver");
        }
        if ("mariadb".equals(type)) {
            return new DriverEntry(
                    "mariadb-connector",
                    "MariaDB Connector/J",
                    "org.mariadb.jdbc.Driver");
        }
        if ("postgresql".equals(type) || "postgres".equals(type)) {
            return new DriverEntry(
                    "postgresql", "PostgreSQL JDBC", "org.postgresql.Driver");
        }
        return null;
    }

    static final class DriverEntry {
        private final String libraryId;
        private final String displayName;
        private final String driverClass;

        DriverEntry(String libraryId, String displayName, String driverClass) {
            this.libraryId = libraryId;
            this.displayName = displayName;
            this.driverClass = driverClass;
        }

        String libraryId() {
            return libraryId;
        }

        String displayName() {
            return displayName;
        }

        String driverClass() {
            return driverClass;
        }
    }
}
