package com.monkey.ktplus.storage.migration;

import java.util.Locale;
import java.util.Objects;

public enum MigrationDialect {
    SQLITE("sqlite"),
    MYSQL("mysql");

    private final String configValue;

    MigrationDialect(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static MigrationDialect parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("sqlite".equals(normalized)) {
            return SQLITE;
        }
        if ("mysql".equals(normalized)) {
            return MYSQL;
        }
        throw new IllegalArgumentException("unsupported migration dialect: " + raw);
    }

    public static boolean isSupportedConfigType(String raw) {
        if (raw == null) {
            return false;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return "sqlite".equals(normalized) || "mysql".equals(normalized);
    }
}
