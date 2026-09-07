package com.monkey.ktplus.storage.migration;

import com.monkey.ktplus.config.ConfigManager;
import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.storage.DatabaseService;
import com.monkey.ktplus.storage.migration.connection.MigrationDataSourceFactory;
import com.monkey.ktplus.storage.migration.phases.ConfigSwapPhase;
import com.monkey.ktplus.storage.migration.phases.ExportPhase;
import com.monkey.ktplus.storage.migration.phases.FreezePhase;
import com.monkey.ktplus.storage.migration.phases.ImportPhase;
import com.monkey.ktplus.storage.migration.phases.PrepareTargetSchemaPhase;
import com.monkey.ktplus.storage.migration.phases.PreflightPhase;
import com.monkey.ktplus.storage.migration.phases.SnapshotPhase;
import com.monkey.ktplus.storage.migration.phases.UnfreezePhase;
import com.monkey.ktplus.storage.migration.phases.ValidationPhase;
import com.monkey.ktplus.storage.repository.TemporaryBlockRepository;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class DatabaseMigrator {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final DatabaseService liveDatabase;
    private final MigrationLock migrationLock;
    private final MigrationDataSourceFactory dataSourceFactory;
    private final @Nullable TemporaryBlockRepository temporaryBlocks;
    private final Logger logger;

    public DatabaseMigrator(
            JavaPlugin plugin,
            ConfigManager configManager,
            DatabaseService liveDatabase,
            MigrationLock migrationLock,
            MigrationDataSourceFactory dataSourceFactory,
            @Nullable TemporaryBlockRepository temporaryBlocks,
            Logger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.liveDatabase = Objects.requireNonNull(liveDatabase, "liveDatabase");
        this.migrationLock = Objects.requireNonNull(migrationLock, "migrationLock");
        this.dataSourceFactory = Objects.requireNonNull(dataSourceFactory, "dataSourceFactory");
        this.temporaryBlocks = temporaryBlocks;
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public MigrationOutcome migrate(MigrationRequest request) {
        Objects.requireNonNull(request, "request");
        if (!migrationLock.tryAcquire()) {
            return new MigrationOutcome(
                    false,
                    "another migration is already running (MigrationLock held)",
                    null,
                    new MigrationContext(request, liveDatabase.writeGate(), backupsDirectory()));
        }
        ConfigSnapshot config = configManager.load();
        MigrationContext context =
                new MigrationContext(request, liveDatabase.writeGate(), backupsDirectory());
        context.markOwnsMigrationLock();

        MigrationPipeline pipeline = new MigrationPipeline(
                new PreflightPhase(plugin, config, liveDatabase, dataSourceFactory, logger),
                new SnapshotPhase(logger),
                new FreezePhase(migrationLock, liveDatabase, temporaryBlocks, logger),
                new ExportPhase(logger),
                new PrepareTargetSchemaPhase(logger),
                new ImportPhase(logger),
                new ValidationPhase(logger),
                new ConfigSwapPhase(configManager::saveDatabaseType, logger),
                new UnfreezePhase(logger),
                migrationLock,
                logger);
        return pipeline.run(context);
    }

    public static @Nullable MigrationRequest parseArgs(String[] args, MigrationDialect liveSource) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(liveSource, "liveSource");
        if (args.length < 1) {
            return null;
        }
        MigrationDialect target;
        try {
            target = MigrationDialect.parse(args[0]);
        } catch (IllegalArgumentException error) {
            return null;
        }
        boolean dryRun = false;
        boolean includeTempBlocks = false;
        boolean forcePendingInventory = false;
        boolean allowNonemptyTarget = false;
        String confirmation = "";
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ("--dry-run".equalsIgnoreCase(arg)) {
                dryRun = true;
            } else if ("--include-temp-blocks".equalsIgnoreCase(arg)) {
                includeTempBlocks = true;
            } else if ("--force-pending-inventory".equalsIgnoreCase(arg)) {
                forcePendingInventory = true;
            } else if ("--allow-nonempty-target".equalsIgnoreCase(arg)) {
                allowNonemptyTarget = true;
            } else if (arg.startsWith("--")) {
                
            } else {
                confirmation = arg;
            }
        }
        return MigrationRequest.builder()
                .sourceDialect(liveSource)
                .targetDialect(target)
                .dryRun(dryRun)
                .includeTempBlocks(includeTempBlocks)
                .forcePendingInventory(forcePendingInventory)
                .allowNonemptyTarget(allowNonemptyTarget)
                .typedTargetConfirmation(confirmation)
                .build();
    }

    public static MigrationDialect liveDialectFromConfig(ConfigSnapshot config) {
        String type = config.database().getString("database.type", "sqlite");
        if (type == null) {
            type = "sqlite";
        }
        return MigrationDialect.parse(type.trim().toLowerCase(Locale.ROOT));
    }

    private Path backupsDirectory() {
        return plugin.getDataFolder().toPath().resolve("backups");
    }
}
