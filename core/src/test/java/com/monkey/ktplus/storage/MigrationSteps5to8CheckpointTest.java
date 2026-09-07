package com.monkey.ktplus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.storage.migration.DatabaseTypeSwapper;
import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationDialect;
import com.monkey.ktplus.storage.migration.MigrationLock;
import com.monkey.ktplus.storage.migration.MigrationOutcome;
import com.monkey.ktplus.storage.migration.MigrationPhase;
import com.monkey.ktplus.storage.migration.MigrationPipeline;
import com.monkey.ktplus.storage.migration.MigrationRequest;
import com.monkey.ktplus.storage.migration.PhaseResult;
import com.monkey.ktplus.storage.migration.PhaseStatus;
import com.monkey.ktplus.storage.migration.connection.MigrationEndpoint;
import com.monkey.ktplus.storage.migration.phases.ConfigSwapPhase;
import com.monkey.ktplus.storage.migration.phases.ExportPhase;
import com.monkey.ktplus.storage.migration.phases.FreezePhase;
import com.monkey.ktplus.storage.migration.phases.ImportPhase;
import com.monkey.ktplus.storage.migration.phases.PrepareTargetSchemaPhase;
import com.monkey.ktplus.storage.migration.phases.SnapshotPhase;
import com.monkey.ktplus.storage.migration.phases.UnfreezePhase;
import com.monkey.ktplus.storage.migration.phases.ValidationPhase;
import com.monkey.ktplus.storage.migration.report.MigrationReportWriter;
import com.monkey.ktplus.storage.migration.snapshot.SnapshotManifest;
import com.monkey.ktplus.storage.migration.snapshot.SqliteFileSnapshot;
import com.monkey.ktplus.storage.migration.transfer.ConflictPolicy;
import com.monkey.ktplus.storage.migration.transfer.MigrationTableCatalog;
import com.monkey.ktplus.storage.migration.transfer.TableExporter;
import com.monkey.ktplus.storage.migration.transfer.TableImporter;
import com.monkey.ktplus.storage.migration.transfer.TableSpec;
import com.monkey.ktplus.storage.migration.writegate.StorageWriteGate;
import com.monkey.ktplus.storage.repository.KillCoinsRepository;
import com.monkey.ktplus.storage.repository.PurchaseRepository;
import com.monkey.ktplus.storage.schema.FlywaySchemaMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationSteps5to8CheckpointTest {
    @TempDir
    Path tempDir;

    private Path sourceDb;
    private Path targetDb;
    private HikariDataSource sourceDs;
    private HikariDataSource targetDs;
    private StorageWriteGate writeGate;
    private DatabaseService sourceDatabase;
    private KillCoinsRepository killCoins;
    private PurchaseRepository purchases;
    private final Logger logger = Logger.getLogger("migrate-test");

    @BeforeEach
    void setUp() throws Exception {
        sourceDb = tempDir.resolve("source.db");
        targetDb = tempDir.resolve("target.db");
        sourceDs = openSqlite(sourceDb, "src");
        targetDs = openSqlite(targetDb, "tgt");
        new FlywaySchemaMigrator(sourceDs).migrate();
        new FlywaySchemaMigrator(targetDs).migrate();
        writeGate = new StorageWriteGate();
        sourceDatabase = new DatabaseService(writeGate);
        sourceDatabase.attachRunningStateForTests("sqlite", sourceDs);
        purchases = new PurchaseRepository(sourceDatabase);
        killCoins = new KillCoinsRepository(sourceDatabase, purchases, 0);
    }

    @AfterEach
    void tearDown() {
        writeGate.unfreezeWrites();
        if (sourceDs != null) {
            sourceDs.close();
        }
        if (targetDs != null) {
            targetDs.close();
        }
    }

    @Test
    void checkpoint1_skipOnConflictDoesNotOverwriteAndAppearsInReport() throws Exception {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 900L);

        try (Connection connection = targetDs.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO kt_killcoins (uuid, balance) VALUES (?, ?)")) {
            insert.setString(1, player.toString());
            insert.setLong(2, 1L);
            insert.executeUpdate();
        }

        Path exportDir = tempDir.resolve("export-skip");
        Files.createDirectories(exportDir);
        writeGate.freezeWrites();
        try (Connection connection = sourceDs.getConnection()) {
            connection.setAutoCommit(false);
            TableExporter.exportEconomyPair(
                    connection,
                    exportDir.resolve("kt_killcoins.tsv"),
                    exportDir.resolve("kt_purchases.tsv"));
            connection.commit();
        }

        try (Connection connection = targetDs.getConnection()) {
            TableImporter.ImportCounts counts = TableImporter.importTable(
                    connection,
                    MigrationTableCatalog.KILLCOINS,
                    exportDir.resolve("kt_killcoins.tsv"),
                    "sqlite",
                    ConflictPolicy.SKIP_EXISTING);
            assertEquals(0L, counts.inserted());
            assertEquals(1L, counts.skipped());
        }

        try (Connection connection = targetDs.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT balance FROM kt_killcoins WHERE uuid = ?")) {
            select.setString(1, player.toString());
            try (ResultSet rs = select.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1L, rs.getLong(1), "existing target row must not be overwritten");
            }
        }

        MigrationRequest request = baseRequest(false);
        MigrationContext context = new MigrationContext(request, writeGate, tempDir.resolve("backups"));
        context.setSnapshotDirectory(tempDir.resolve("snap-dir"));
        Files.createDirectories(context.snapshotDirectory());
        context.putExportedRowCount("kt_killcoins", 1L);
        context.putImportedRowCount("kt_killcoins", 0L);
        context.putSkippedRowCount("kt_killcoins", 1L);
        Path report = MigrationReportWriter.write(context);
        String md = Files.readString(report, StandardCharsets.UTF_8);
        assertTrue(md.contains("| kt_killcoins | 1 | 0 | 1 |"), md);
        assertTrue(md.contains("snapshotPath:"));
    }

    @Test
    void checkpoint2_killcoinsSumHardFailBlocksConfigSwap() throws Exception {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 500L);

        try (Connection connection = targetDs.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO kt_killcoins (uuid, balance) VALUES (?, ?)")) {
            insert.setString(1, player.toString());
            insert.setLong(2, 1L);
            insert.executeUpdate();
        }

        Path exportDir = tempDir.resolve("export-hardfail");
        MigrationLock lock = new MigrationLock();
        assertTrue(lock.tryAcquire());
        MigrationRequest request = baseRequest(false);
        MigrationContext context = seededContext(request, exportDir, lock);
        seedFullExport(context, exportDir);

        AtomicBoolean swapped = new AtomicBoolean(false);
        MigrationOutcome outcome = pipeline(lock, type -> swapped.set(true)).runPostExport(context);
        assertFalse(outcome.success());
        assertTrue(context.economicValidationHardFailed());
        assertFalse(context.validationPassed());
        assertFalse(swapped.get(), "ConfigSwap must not write database.type after economic HARD FAIL");
        PhaseResult swapResult = context.phaseResults().get("config-swap");
        assertNotNull(swapResult);
        assertEquals(PhaseStatus.FAILED, swapResult.status());
        assertTrue(swapResult.message().toLowerCase().contains("refused")
                || swapResult.message().contains("HARD FAIL"));
        assertFalse(context.configSwapped());
    }

    @Test
    void checkpoint3_reportAlwaysIncludesSnapshotPathOnSuccess() throws Exception {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 42L);

        Path exportDir = tempDir.resolve("export-ok");
        MigrationLock lock = new MigrationLock();
        assertTrue(lock.tryAcquire());
        MigrationRequest request = baseRequest(false);
        MigrationContext context = seededContext(request, exportDir, lock);
        Path snapDir = context.snapshotDirectory();
        SqliteFileSnapshot.create(sourceDs, MigrationEndpoint.sqlite(sourceDb), snapDir, writeGate);
        context.setSnapshotManifest(SnapshotManifest.readFrom(snapDir));
        seedFullExport(context, exportDir);

        AtomicReference<String> swappedTo = new AtomicReference<String>();
        MigrationOutcome outcome = pipeline(lock, swappedTo::set).runPostExport(context);
        assertTrue(outcome.success(), outcome.message());
        assertTrue(context.configSwapped());
        assertEquals("mysql", swappedTo.get());
        assertNotNull(outcome.reportPath());
        String md = Files.readString(outcome.reportPath(), StandardCharsets.UTF_8);
        assertTrue(md.contains("snapshotPath: " + snapDir.toString().replace('\\', '/'))
                        || md.contains("snapshotPath: " + snapDir),
                md);
        assertFalse(md.contains("snapshotPath: (none)"), md);
    }

    @Test
    void checkpoint4_dryRunNeverWritesTargetOrSwapsConfig() throws Exception {
        killCoins.setBalance(UUID.randomUUID(), 10L);

        Path exportDir = tempDir.resolve("export-dry");
        MigrationLock lock = new MigrationLock();
        assertTrue(lock.tryAcquire());
        MigrationRequest request = baseRequest(true);
        MigrationContext context = seededContext(request, exportDir, lock);
        seedFullExport(context, exportDir);

        AtomicBoolean swapped = new AtomicBoolean(false);
        long targetRowsBefore = countRows(targetDs, "kt_killcoins");

        MigrationOutcome outcome = pipeline(lock, type -> swapped.set(true)).runPostExport(context);
        assertTrue(outcome.success());
        assertFalse(swapped.get());
        assertFalse(context.configSwapped());
        assertFalse(context.targetWritesAttempted());
        assertEquals(targetRowsBefore, countRows(targetDs, "kt_killcoins"));
        assertEquals(PhaseStatus.SKIPPED, context.phaseResults().get("config-swap").status());
        assertEquals(PhaseStatus.SKIPPED, context.phaseResults().get("import").status());
        assertEquals(PhaseStatus.SKIPPED, context.phaseResults().get("prepare-target-schema").status());
    }

    @Test
    void checkpoint5_hardCrashMidPipelineRestartsCleanWithUsableSnapshot() throws Exception {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 777L);
        assertTrue(purchases.claimPurchaseIfAbsent(player, "crash"));

        Path backups = tempDir.resolve("backups-crash");
        MigrationLock lock = new MigrationLock();
        assertTrue(lock.tryAcquire());
        MigrationRequest request = baseRequest(false);
        MigrationContext context = new MigrationContext(request, writeGate, backups);
        context.markOwnsMigrationLock();
        context.setSourceDataSource(sourceDs, false);
        context.setSourceEndpoint(MigrationEndpoint.sqlite(sourceDb));

        assertEquals(PhaseStatus.SUCCESS, new SnapshotPhase(logger).run(context).status());
        assertEquals(
                PhaseStatus.SUCCESS,
                new FreezePhase(lock, sourceDatabase, null, logger).run(context).status());
        assertNotNull(context.snapshotDirectory());
        assertNotNull(context.snapshotManifest());
        Path snapshotDb = context.snapshotDirectory().resolve(sourceDb.getFileName().toString());
        assertTrue(Files.isRegularFile(snapshotDb));
        assertTrue(writeGate.writesFrozen());

        StorageWriteGate restartedGate = new StorageWriteGate();
        assertFalse(restartedGate.writesFrozen());
        DatabaseService restartedDb = new DatabaseService(restartedGate);
        restartedDb.attachRunningStateForTests("sqlite", sourceDs);
        assertEquals("sqlite", restartedDb.dialect());
        restartedDb.update(
                "UPDATE kt_killcoins SET balance = balance WHERE uuid = ?",
                statement -> statement.setString(1, player.toString()));

        SnapshotManifest reloaded = SnapshotManifest.readFrom(context.snapshotDirectory());
        assertEquals(1L, reloaded.table("kt_killcoins").rowCount());
        assertEquals(Long.valueOf(777L), reloaded.table("kt_killcoins").sumBalance());
        assertTrue(Files.isRegularFile(snapshotDb), "manual restore artifact must survive crash");
    }

    private MigrationRequest baseRequest(boolean dryRun) {
        return MigrationRequest.builder()
                .sourceDialect(MigrationDialect.SQLITE)
                .targetDialect(MigrationDialect.MYSQL)
                .dryRun(dryRun)
                .allowNonemptyTarget(true)
                .typedTargetConfirmation("localhost:3306/ktplus")
                .build();
    }

    private MigrationContext seededContext(MigrationRequest request, Path exportDir, MigrationLock lock)
            throws Exception {
        MigrationContext context = new MigrationContext(request, writeGate, tempDir.resolve("backups"));
        context.markOwnsMigrationLock();
        context.setSourceDataSource(sourceDs, false);
        context.setTargetDataSource(targetDs, false);
        context.setSourceEndpoint(MigrationEndpoint.sqlite(sourceDb));
        context.setTargetEndpoint(MigrationEndpoint.mysql("localhost", 3306, "ktplus"));
        context.setExportDirectory(exportDir);
        Path snap = tempDir.resolve("snap-" + exportDir.getFileName());
        Files.createDirectories(snap);
        context.setSnapshotDirectory(snap);
        Files.createDirectories(exportDir);
        writeGate.freezeWrites();
        context.markWritesFrozen();
        return context;
    }

    private void seedFullExport(MigrationContext context, Path exportDir) throws Exception {
        try (Connection connection = sourceDs.getConnection()) {
            TableExporter.exportForTransfer(connection, exportDir, context.request());
        }
        for (TableSpec spec : MigrationTableCatalog.forTransfer(context.request())) {
            long lines = Files.readAllLines(exportDir.resolve(spec.name() + ".tsv")).size() - 1L;
            context.putExportedRowCount(spec.name(), Math.max(0L, lines));
        }
    }

    private MigrationPipeline pipeline(MigrationLock lock, DatabaseTypeSwapper swapper) {
        MigrationPhase noop = new MigrationPhase() {
            @Override
            public String name() {
                return "noop";
            }

            @Override
            public PhaseResult run(MigrationContext context) {
                return PhaseResult.skipped("noop");
            }
        };
        return new MigrationPipeline(
                noop,
                new SnapshotPhase(logger),
                new FreezePhase(lock, sourceDatabase, null, logger),
                new ExportPhase(logger),
                new PrepareTargetSchemaPhase(logger),
                new ImportPhase(logger),
                new ValidationPhase(logger),
                new ConfigSwapPhase(swapper, logger),
                new UnfreezePhase(logger),
                lock,
                logger);
    }

    private static HikariDataSource openSqlite(Path file, String poolName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + file.toAbsolutePath());
        config.setMaximumPoolSize(2);
        config.setPoolName(poolName);
        return new HikariDataSource(config);
    }

    private static long countRows(HikariDataSource dataSource, String table) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
                ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
