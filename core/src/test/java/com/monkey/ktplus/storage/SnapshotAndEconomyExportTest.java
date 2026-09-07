package com.monkey.ktplus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.storage.migration.MigrationContext;
import com.monkey.ktplus.storage.migration.MigrationDialect;
import com.monkey.ktplus.storage.migration.MigrationLock;
import com.monkey.ktplus.storage.migration.MigrationRequest;
import com.monkey.ktplus.storage.migration.PhaseResult;
import com.monkey.ktplus.storage.migration.PhaseStatus;
import com.monkey.ktplus.storage.migration.connection.MigrationEndpoint;
import com.monkey.ktplus.storage.migration.phases.ExportPhase;
import com.monkey.ktplus.storage.migration.phases.FreezePhase;
import com.monkey.ktplus.storage.migration.phases.SnapshotPhase;
import com.monkey.ktplus.storage.migration.phases.UnfreezePhase;
import com.monkey.ktplus.storage.migration.snapshot.SnapshotManifest;
import com.monkey.ktplus.storage.migration.snapshot.SqliteFileSnapshot;
import com.monkey.ktplus.storage.migration.transfer.MigrationTableCatalog;
import com.monkey.ktplus.storage.migration.transfer.TableExporter;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprint;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprinter;
import com.monkey.ktplus.storage.migration.writegate.StorageWriteFrozenException;
import com.monkey.ktplus.storage.migration.writegate.StorageWriteGate;
import com.monkey.ktplus.storage.repository.KillCoinsRepository;
import com.monkey.ktplus.storage.repository.PurchaseRepository;
import com.monkey.ktplus.storage.schema.FlywaySchemaMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SnapshotAndEconomyExportTest {
    @TempDir
    Path tempDir;

    private Path dbFile;
    private HikariDataSource dataSource;
    private StorageWriteGate writeGate;
    private DatabaseService database;
    private KillCoinsRepository killCoins;
    private PurchaseRepository purchases;
    private final Logger logger = Logger.getLogger("test");

    @BeforeEach
    void setUp() throws Exception {
        dbFile = tempDir.resolve("ktplus-snapshot.db");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
        config.setMaximumPoolSize(2);
        config.setPoolName("KTSnapshotTest");
        dataSource = new HikariDataSource(config);
        new FlywaySchemaMigrator(dataSource).migrate();
        writeGate = new StorageWriteGate();
        database = new DatabaseService(writeGate);
        database.attachRunningStateForTests("sqlite", dataSource);
        purchases = new PurchaseRepository(database);
        killCoins = new KillCoinsRepository(database, purchases, 0);
    }

    @AfterEach
    void tearDown() {
        writeGate.unfreezeWrites();
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void sqliteFileSnapshotRequiresFreezeAndCopiesAfterCheckpoint() throws Exception {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 420L);
        assertTrue(purchases.claimPurchaseIfAbsent(player, "fire"));

        Path snapshotDir = tempDir.resolve("backups").resolve("snap");
        MigrationEndpoint endpoint = MigrationEndpoint.sqlite(dbFile);

        assertThrows(
                IllegalStateException.class,
                () -> SqliteFileSnapshot.create(dataSource, endpoint, snapshotDir, writeGate));

        writeGate.freezeWrites();
        var result = SqliteFileSnapshot.create(dataSource, endpoint, snapshotDir, writeGate);

        assertTrue(Files.isRegularFile(result.directory().resolve(dbFile.getFileName().toString())));
        assertTrue(Files.isRegularFile(result.directory().resolve(SnapshotManifest.FILE_NAME)));
        assertEquals("sqlite-file-copy", result.manifest().kind());
        assertEquals(1L, result.manifest().table("kt_killcoins").rowCount());
        assertEquals(Long.valueOf(420L), result.manifest().table("kt_killcoins").sumBalance());
    }

    @Test
    void snapshotPhaseDefersSqlitePhysicalCopyUntilFreeze() throws Exception {
        killCoins.setBalance(UUID.randomUUID(), 10L);

        MigrationLock lock = new MigrationLock();
        assertTrue(lock.tryAcquire());
        MigrationRequest request = MigrationRequest.builder()
                .sourceDialect(MigrationDialect.SQLITE)
                .targetDialect(MigrationDialect.MYSQL)
                .typedTargetConfirmation("localhost:3306/ktplus")
                .build();
        MigrationContext context = new MigrationContext(request, writeGate, tempDir.resolve("backups"));
        context.markOwnsMigrationLock();
        context.setSourceDataSource(dataSource, false);
        context.setSourceEndpoint(MigrationEndpoint.sqlite(dbFile));

        PhaseResult snapshot = new SnapshotPhase(logger).run(context);
        assertEquals(PhaseStatus.SUCCESS, snapshot.status());
        assertTrue(context.snapshotDirectory() != null);
        assertEquals(null, context.snapshotManifest());
        assertFalse(Files.isRegularFile(context.snapshotDirectory().resolve(dbFile.getFileName().toString())));

        PhaseResult freeze = new FreezePhase(lock, database, null, logger).run(context);
        assertEquals(PhaseStatus.SUCCESS, freeze.status());
        assertTrue(context.snapshotManifest() != null);
        assertTrue(Files.isRegularFile(context.snapshotDirectory().resolve(dbFile.getFileName().toString())));
        assertTrue(writeGate.writesFrozen());

        new UnfreezePhase(logger).run(context);
        lock.release();
    }

    @Test
    void freezeRefusesAlreadyFrozenGateAndForeignLock() {
        MigrationRequest request = MigrationRequest.builder()
                .sourceDialect(MigrationDialect.SQLITE)
                .targetDialect(MigrationDialect.MYSQL)
                .typedTargetConfirmation("x")
                .build();
        MigrationLock lock = new MigrationLock();
        assertTrue(lock.tryAcquire());

        MigrationContext unclean = new MigrationContext(request, writeGate, tempDir.resolve("b1"));
        unclean.markOwnsMigrationLock();
        unclean.setSourceDataSource(dataSource, false);
        unclean.setSourceEndpoint(MigrationEndpoint.sqlite(dbFile));
        writeGate.freezeWrites();
        assertEquals(PhaseStatus.FAILED, new FreezePhase(lock, database, null, logger).run(unclean).status());
        writeGate.unfreezeWrites();

        MigrationContext noOwnership = new MigrationContext(request, writeGate, tempDir.resolve("b2"));
        noOwnership.setSourceDataSource(dataSource, false);
        noOwnership.setSourceEndpoint(MigrationEndpoint.sqlite(dbFile));
        
        assertEquals(PhaseStatus.FAILED, new FreezePhase(lock, database, null, logger).run(noOwnership).status());

        lock.release();
        MigrationContext noLock = new MigrationContext(request, writeGate, tempDir.resolve("b3"));
        noLock.markOwnsMigrationLock();
        noLock.setSourceDataSource(dataSource, false);
        noLock.setSourceEndpoint(MigrationEndpoint.sqlite(dbFile));
        assertEquals(PhaseStatus.FAILED, new FreezePhase(lock, database, null, logger).run(noLock).status());
    }

    @Test
    void exportPhaseUsesEconomyPairAndRequiresFreeze() throws Exception {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 250L);
        assertTrue(purchases.claimPurchaseIfAbsent(player, "hearts"));

        MigrationLock lock = new MigrationLock();
        assertTrue(lock.tryAcquire());
        MigrationRequest request = MigrationRequest.builder()
                .sourceDialect(MigrationDialect.SQLITE)
                .targetDialect(MigrationDialect.MYSQL)
                .typedTargetConfirmation("localhost:3306/ktplus")
                .build();
        MigrationContext context = new MigrationContext(request, writeGate, tempDir.resolve("backups"));
        context.markOwnsMigrationLock();
        context.setSourceDataSource(dataSource, false);
        context.setSourceEndpoint(MigrationEndpoint.sqlite(dbFile));
        context.setExportDirectory(tempDir.resolve("export"));

        assertEquals(PhaseStatus.FAILED, new ExportPhase(logger).run(context).status());

        assertEquals(PhaseStatus.SUCCESS, new SnapshotPhase(logger).run(context).status());
        assertEquals(PhaseStatus.SUCCESS, new FreezePhase(lock, database, null, logger).run(context).status());
        assertEquals(PhaseStatus.SUCCESS, new ExportPhase(logger).run(context).status());

        assertEquals(Long.valueOf(1L), context.exportedRowCounts().get("kt_killcoins"));
        assertEquals(Long.valueOf(1L), context.exportedRowCounts().get("kt_purchases"));
        assertTrue(Files.isRegularFile(context.exportDirectory().resolve("kt_killcoins.tsv")));
        assertTrue(Files.isRegularFile(context.exportDirectory().resolve("kt_purchases.tsv")));
        assertThrows(StorageWriteFrozenException.class, () -> killCoins.setBalance(UUID.randomUUID(), 1L));

        assertEquals(PhaseStatus.SUCCESS, new UnfreezePhase(logger).run(context).status());
        lock.release();
    }

    @Test
    void economyPairExportUsesSingleTransactionSnapshot() throws Exception {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 500L);

        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        AtomicReference<Throwable> purchaseError = new AtomicReference<Throwable>();
        AtomicReference<TableExporter.EconomyExport> exportRef = new AtomicReference<TableExporter.EconomyExport>();
        AtomicReference<Throwable> exportError = new AtomicReference<Throwable>();
        CountDownLatch exportDone = new CountDownLatch(1);

        Thread purchaseThread = new Thread(() -> {
            try {
                database.transaction(connection -> {
                    assertTrue(purchases.tryClaimOn(connection, player, "smoke"));
                    entered.countDown();
                    try {
                        assertTrue(allowCommit.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new java.sql.SQLException("interrupted", interrupted);
                    }
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE kt_killcoins SET balance = balance - ? WHERE uuid = ? AND balance >= ?")) {
                        update.setLong(1, 100L);
                        update.setString(2, player.toString());
                        update.setLong(3, 100L);
                        assertEquals(1, update.executeUpdate());
                    }
                });
            } catch (Throwable error) {
                purchaseError.set(error);
            }
        });
        purchaseThread.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));

        Path exportDir = tempDir.resolve("economy-export");
        Files.createDirectories(exportDir);
        Thread exportThread = new Thread(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                exportRef.set(TableExporter.exportEconomyPair(
                        connection,
                        exportDir.resolve("kt_killcoins.tsv"),
                        exportDir.resolve("kt_purchases.tsv")));
                connection.commit();
            } catch (Throwable error) {
                exportError.set(error);
            } finally {
                exportDone.countDown();
            }
        });
        exportThread.start();

        if (!exportDone.await(300, TimeUnit.MILLISECONDS)) {
            allowCommit.countDown();
            assertTrue(exportDone.await(5, TimeUnit.SECONDS));
        } else {
            allowCommit.countDown();
        }
        purchaseThread.join(5_000L);
        exportThread.join(5_000L);

        assertEquals(null, purchaseError.get());
        assertEquals(null, exportError.get());
        TableExporter.EconomyExport midFlight = exportRef.get();
        boolean consistentPre = midFlight.killcoins().rowCount() == 1L
                && midFlight.killcoins().sumBalance().longValue() == 500L
                && midFlight.purchases().rowCount() == 0L;
        boolean consistentPost = midFlight.killcoins().rowCount() == 1L
                && midFlight.killcoins().sumBalance().longValue() == 400L
                && midFlight.purchases().rowCount() == 1L;
        assertTrue(consistentPre || consistentPost);
    }

    @Test
    void fingerprintEconomyPairMatchesExporterChecksums() throws Exception {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 77L);
        assertTrue(purchases.claimPurchaseIfAbsent(player, "ice"));

        Path dir = tempDir.resolve("fp");
        Files.createDirectories(dir);
        TableExporter.EconomyExport exported;
        Map<String, TableFingerprint> fingerprinted;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            exported = TableExporter.exportEconomyPair(
                    connection, dir.resolve("k.tsv"), dir.resolve("p.tsv"));
            fingerprinted = TableFingerprinter.fingerprintEconomyPair(connection);
            connection.commit();
        }
        assertEquals(
                fingerprinted.get(MigrationTableCatalog.KILLCOINS.name()).contentChecksum(),
                exported.killcoins().contentChecksum());
        assertEquals(
                fingerprinted.get(MigrationTableCatalog.PURCHASES.name()).contentChecksum(),
                exported.purchases().contentChecksum());
        assertEquals(Long.valueOf(77L), exported.killcoins().sumBalance());
    }

    @Test
    void transferCatalogExcludesTempBlocksUnlessRequested() {
        MigrationRequest withoutTemp = MigrationRequest.builder()
                .sourceDialect(MigrationDialect.SQLITE)
                .targetDialect(MigrationDialect.MYSQL)
                .typedTargetConfirmation("x")
                .build();
        MigrationRequest withTemp = MigrationRequest.builder()
                .sourceDialect(MigrationDialect.SQLITE)
                .targetDialect(MigrationDialect.MYSQL)
                .includeTempBlocks(true)
                .typedTargetConfirmation("x")
                .build();
        assertEquals(5, MigrationTableCatalog.forTransfer(withoutTemp).size());
        assertEquals(6, MigrationTableCatalog.forTransfer(withTemp).size());
        assertEquals(6, MigrationTableCatalog.forSnapshot().size());
    }
}
