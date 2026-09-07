package com.monkey.ktplus.storage.schema;

import com.monkey.ktplus.storage.DatabaseService;
import java.util.Objects;
import javax.sql.DataSource;
import org.flywaydb.core.api.output.MigrateResult;

public final class SchemaInitializer {
    private final DatabaseService database;
    private final ClassLoader classLoader;

    public SchemaInitializer(DatabaseService database) {
        this(database, SchemaInitializer.class.getClassLoader());
    }

    public SchemaInitializer(DatabaseService database, ClassLoader classLoader) {
        this.database = Objects.requireNonNull(database, "database");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    public MigrateResult initialize() {
        DataSource dataSource = database.dataSource();
        return new FlywaySchemaMigrator(dataSource, classLoader).migrate();
    }
}
