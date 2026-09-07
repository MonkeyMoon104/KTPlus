package com.monkey.ktplus.storage.schema;

import com.monkey.ktplus.storage.StorageException;
import db.migration.V1__Initial_schema;
import db.migration.V2__Temp_blocks;
import db.migration.V3__Temp_blocks_block_data;
import db.migration.V4__Pending_inventory;
import db.migration.V5__Review_claims;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogCreator;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.api.logging.LogLevel;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.output.MigrateResult;

public final class FlywaySchemaMigrator {
    private static final JavaMigration[] JAVA_MIGRATIONS = {
        new V1__Initial_schema(),
        new V2__Temp_blocks(),
        new V3__Temp_blocks_block_data(),
        new V4__Pending_inventory(),
        new V5__Review_claims()
    };

    private static final Log QUIET_LOG = new Log() {
        @Override
        public void debug(String message) {}

        @Override
        public void info(String message) {}

        @Override
        public void warn(String message) {}

        @Override
        public void error(String message) {}

        @Override
        public void error(String message, Exception e) {}

        @Override
        public void notice(String message) {}
    };

    private static final LogCreator QUIET_CREATOR = ignored -> QUIET_LOG;

    private final DataSource dataSource;
    private final ClassLoader classLoader;

    public FlywaySchemaMigrator(DataSource dataSource) {
        this(dataSource, FlywaySchemaMigrator.class.getClassLoader());
    }

    public FlywaySchemaMigrator(DataSource dataSource, ClassLoader classLoader) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    public MigrateResult migrate() {
        LogLevel previousLevel = LogFactory.getLogLevel();
        LogFactory.setFallbackLogCreator(QUIET_CREATOR);
        LogFactory.setLogCreator(QUIET_CREATOR);
        LogFactory.setLogLevel(LogLevel.WARN);
        try {
            int legacy = readLegacyVersion();
            MigrateResult result = Flyway.configure(classLoader)
                    .dataSource(dataSource)
                    .locations(new String[0])
                    .javaMigrations(JAVA_MIGRATIONS)
                    .table("kt_flyway_schema_history")
                    .baselineOnMigrate(true)
                    .baselineVersion(MigrationVersion.fromVersion(String.valueOf(Math.max(0, legacy))))
                    .baselineDescription("Legacy SchemaMigrator baseline")
                    .validateOnMigrate(true)
                    .failOnMissingLocations(false)
                    .loggers("console")
                    .load()
                    .migrate();
            if (result == null || !result.success) {
                throw new StorageException("Flyway migration reported failure");
            }
            return result;
        } catch (StorageException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new StorageException("Flyway migration failed", ex);
        } finally {
            LogFactory.setLogCreator(null);
            LogFactory.setFallbackLogCreator(null);
            LogFactory.setLogLevel(previousLevel == null ? LogLevel.INFO : previousLevel);
        }
    }

    private int readLegacyVersion() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            if (!hasLegacySchemaTable(connection)) {
                return 0;
            }
            try (ResultSet resultSet =
                    statement.executeQuery("SELECT version FROM kt_schema WHERE id = 'main'")) {
                if (resultSet.next()) {
                    return Math.max(0, resultSet.getInt(1));
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static boolean hasLegacySchemaTable(Connection connection) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, "kt_schema", null)) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = connection.getMetaData().getTables(null, null, "KT_SCHEMA", null)) {
            return tables.next();
        }
    }
}
