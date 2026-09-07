package com.monkey.ktplus.storage.migration.connection;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.storage.driver.SqlDriverInstaller;
import com.monkey.ktplus.storage.migration.MigrationDialect;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class MigrationDataSourceFactory {
    private static final long CONNECTION_TIMEOUT_MS = 10_000L;
    private static final long VALIDATION_TIMEOUT_MS = 5_000L;

    private final JavaPlugin plugin;

    public MigrationDataSourceFactory(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public HikariDataSource open(ConfigSnapshot config, MigrationEndpoint endpoint) throws Exception {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(endpoint, "endpoint");
        String dialect = endpoint.dialect().configValue();
        String driverClass = SqlDriverInstaller.install(plugin, dialect);
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("KTMigrate-" + dialect);
        hikari.setMaximumPoolSize(endpoint.dialect() == MigrationDialect.SQLITE ? 1 : 2);
        hikari.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        hikari.setValidationTimeout(VALIDATION_TIMEOUT_MS);
        hikari.setJdbcUrl(MigrationJdbcUrls.jdbcUrl(endpoint));
        String username = config.database().getString("database.username", "");
        String password = config.database().getString("database.password", "");
        if (username != null) {
            hikari.setUsername(username);
        }
        if (password != null) {
            hikari.setPassword(password);
        }
        if (driverClass != null) {
            hikari.setDriverClassName(driverClass);
        }
        if (endpoint.dialect() == MigrationDialect.MYSQL) {
            hikari.addDataSourceProperty("cachePrepStmts", "true");
            hikari.addDataSourceProperty("prepStmtCacheSize", "250");
            hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikari.addDataSourceProperty("useServerPrepStmts", "true");
            hikari.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikari.addDataSourceProperty("maintainTimeStats", "false");
        }
        return new HikariDataSource(hikari);
    }
}
