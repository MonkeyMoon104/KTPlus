package com.monkey.ktplus.storage;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.monkey.ktplus.storage.driver.SqlDriverInstaller;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;
import com.monkey.ktplus.storage.driver.SnapshotCredentialsProvider;
import com.monkey.ktplus.storage.migration.writegate.StorageWriteGate;
import com.monkey.ktplus.storage.schema.SchemaInitializer;
import com.monkey.ktplus.storage.sql.ResultMapper;
import com.monkey.ktplus.storage.sql.SqlBinder;
import com.monkey.ktplus.storage.sql.SqlTransaction;
import org.flywaydb.core.api.output.MigrateResult;

public final class DatabaseService {
    private static final long CONNECTION_TIMEOUT_MS = 10_000L;
    private static final long VALIDATION_TIMEOUT_MS = 5_000L;
    private static final long KEEPALIVE_MS = 120_000L;

    private final @Nullable JavaPlugin plugin;
    private final StorageWriteGate writeGate;
    private final AtomicInteger inFlightWrites = new AtomicInteger(0);
    private HikariDataSource dataSource;
    private ExecutorService executor;
    private String dialect;

    public DatabaseService(JavaPlugin plugin) {
        this(Objects.requireNonNull(plugin, "plugin"), new StorageWriteGate());
    }

    DatabaseService(JavaPlugin plugin, StorageWriteGate writeGate) {
        this.plugin = plugin;
        this.writeGate = Objects.requireNonNull(writeGate, "writeGate");
    }

    DatabaseService(StorageWriteGate writeGate) {
        this.plugin = null;
        this.writeGate = Objects.requireNonNull(writeGate, "writeGate");
    }

    public StorageWriteGate writeGate() {
        return writeGate;
    }

    
    public int inFlightWrites() {
        return inFlightWrites.get();
    }

    void attachRunningStateForTests(String dialect, HikariDataSource dataSource) {
        this.dialect = Objects.requireNonNull(dialect, "dialect");
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public MigrateResult start(ConfigSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(plugin, "plugin");
        dialect = snapshot.database().getString("database.type", "sqlite").toLowerCase();
        int poolSize = Math.max(1, snapshot.database().getInt("database.pool-size", 5));
        HikariConfig config = new HikariConfig();
        config.setPoolName("KTPool");
        config.setMaximumPoolSize("sqlite".equals(dialect) ? 1 : poolSize);
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        config.setValidationTimeout(VALIDATION_TIMEOUT_MS);
        config.setKeepaliveTime(KEEPALIVE_MS);
        config.setCredentialsProvider(new SnapshotCredentialsProvider(
                snapshot.database().getString("database.username", ""),
                snapshot.database().getString("database.password", "")));
        applyDialectProperties(config, dialect);
        config.setJdbcUrl(jdbcUrl(snapshot));
        try {
            String driverClass = SqlDriverInstaller.install(plugin, dialect);
            if (driverClass != null) {
                config.setDriverClassName(driverClass);
            }
            executor = databaseExecutor(poolSize);
            dataSource = new HikariDataSource(config);
        } catch (Exception error) {
            throw new StorageException("Failed to install SQL driver for dialect " + dialect, error);
        }
        ClassLoader classLoader = plugin.getClass().getClassLoader();
        return new SchemaInitializer(this, classLoader).initialize();
    }

    public String dialect() {
        return dialect;
    }

    public int update(String sql, @Nullable SqlBinder binder) {
        writeGate.requireWritable();
        inFlightWrites.incrementAndGet();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Database update failed", ex);
        } finally {
            inFlightWrites.decrementAndGet();
        }
    }

    public <T> Optional<T> queryOne(String sql, @Nullable SqlBinder binder, ResultMapper<T> mapper) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(mapper.map(resultSet));
            }
        } catch (SQLException ex) {
            throw new StorageException("Database query failed", ex);
        }
    }

    public <T> java.util.List<T> queryList(String sql, @Nullable SqlBinder binder, ResultMapper<T> mapper) {
        java.util.List<T> values = new java.util.ArrayList<T>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    T mapped = mapper.map(resultSet);
                    if (mapped != null) {
                        values.add(mapped);
                    }
                }
            }
            return values;
        } catch (SQLException ex) {
            throw new StorageException("Database query failed", ex);
        }
    }

    public void transaction(SqlTransaction transaction) {
        writeGate.requireWritable();
        inFlightWrites.incrementAndGet();
        try (Connection connection = openConnection()) {
            boolean previous = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                transaction.execute(connection);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previous);
            }
        } catch (SQLException ex) {
            throw new StorageException("Database transaction failed", ex);
        } finally {
            inFlightWrites.decrementAndGet();
        }
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executor);
    }

    public void close() {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public HikariDataSource dataSource() {
        return dataSource;
    }

    private Connection openConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static ExecutorService databaseExecutor(int poolSize) {
        return Executors.newFixedThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable, "KTPlus-Database");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static void applyDialectProperties(HikariConfig config, String dialect) {
        if ("postgresql".equals(dialect) || "postgres".equals(dialect)) {
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
            config.addDataSourceProperty("ApplicationName", "KTPlus");
            return;
        }
        if ("mysql".equals(dialect)) {
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            return;
        }
        if ("mariadb".equals(dialect)) {
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        }
    }

    private String jdbcUrl(ConfigSnapshot snapshot) {
        if ("sqlite".equals(dialect)) {
            File file = new File(plugin.getDataFolder(), snapshot.database().getString("database.file", "ktplus.db"));
            return "jdbc:sqlite:" + file.getAbsolutePath();
        }
        String host = snapshot.database().getString("database.host", "localhost");
        int port = snapshot.database().getInt("database.port", defaultPort(dialect));
        String database = snapshot.database().getString("database.database", "ktplus");
        if ("postgresql".equals(dialect) || "postgres".equals(dialect)) {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database;
        }
        if ("mariadb".equals(dialect)) {
            return "jdbc:mariadb://" + host + ":" + port + "/" + database + "?sslMode=trust";
        }
        return "jdbc:mysql://" + host + ":" + port + "/"
                + database
                + "?sslMode=PREFERRED"
                + "&allowPublicKeyRetrieval=true"
                + "&connectionTimeZone=UTC"
                + "&characterEncoding=UTF-8";
    }

    private int defaultPort(String type) {
        if ("postgresql".equals(type) || "postgres".equals(type)) {
            return 5432;
        }
        return 3306;
    }
}
