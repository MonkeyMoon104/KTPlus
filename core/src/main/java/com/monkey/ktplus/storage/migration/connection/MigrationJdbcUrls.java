package com.monkey.ktplus.storage.migration.connection;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.storage.migration.MigrationDialect;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class MigrationJdbcUrls {
    private MigrationJdbcUrls() {}

    public static MigrationEndpoint resolveEndpoint(
            JavaPlugin plugin, ConfigSnapshot config, MigrationDialect dialect) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(dialect, "dialect");
        if (dialect == MigrationDialect.SQLITE) {
            String fileName = config.database().getString("database.file", "ktplus.db");
            if (fileName == null || fileName.isBlank()) {
                fileName = "ktplus.db";
            }
            Path path = new File(plugin.getDataFolder(), fileName).toPath().toAbsolutePath().normalize();
            return MigrationEndpoint.sqlite(path);
        }
        String host = config.database().getString("database.host", "localhost");
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        int port = config.database().getInt("database.port", 3306);
        String database = config.database().getString("database.database", "ktplus");
        if (database == null || database.isBlank()) {
            database = "ktplus";
        }
        return MigrationEndpoint.mysql(host, port, database);
    }

    public static String jdbcUrl(MigrationEndpoint endpoint) {
        Objects.requireNonNull(endpoint, "endpoint");
        if (endpoint.dialect() == MigrationDialect.SQLITE) {
            return "jdbc:sqlite:" + endpoint.sqliteFile();
        }
        return "jdbc:mysql://"
                + endpoint.host()
                + ":"
                + endpoint.port()
                + "/"
                + endpoint.databaseName()
                + "?sslMode=PREFERRED&allowPublicKeyRetrieval=true&connectionTimeZone=UTC"
                + "&characterEncoding=UTF-8";
    }
}
