package com.monkey.ktplus.storage.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.storage.migration.writegate.StorageWriteFrozenException;
import com.monkey.ktplus.storage.migration.writegate.StorageWriteGate;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MigrationFoundationsTest {
    @Test
    void parseSupportedDialectsOnly() {
        assertEquals(MigrationDialect.SQLITE, MigrationDialect.parse("SQLite"));
        assertEquals(MigrationDialect.MYSQL, MigrationDialect.parse("mysql"));
        assertThrows(IllegalArgumentException.class, () -> MigrationDialect.parse("mariadb"));
        assertThrows(IllegalArgumentException.class, () -> MigrationDialect.parse("postgresql"));
        assertFalse(MigrationDialect.isSupportedConfigType("mariadb"));
        assertTrue(MigrationDialect.isSupportedConfigType("sqlite"));
    }

    @Test
    void requestRejectsIdenticalDialects() {
        assertThrows(IllegalArgumentException.class, () -> MigrationRequest.builder()
                .sourceDialect(MigrationDialect.SQLITE)
                .targetDialect(MigrationDialect.SQLITE)
                .typedTargetConfirmation("sqlite")
                .build());
    }

    @Test
    void requestTracksFlagsAndResolvedConfirmationToken() {
        MigrationRequest request = MigrationRequest.builder()
                .sourceDialect(MigrationDialect.SQLITE)
                .targetDialect(MigrationDialect.MYSQL)
                .dryRun(true)
                .includeTempBlocks(true)
                .forcePendingInventory(true)
                .allowNonemptyTarget(true)
                .typedTargetConfirmation("localhost:3306/ktplus")
                .build();
        assertTrue(request.dryRun());
        assertTrue(request.includeTempBlocks());
        assertTrue(request.forcePendingInventory());
        assertTrue(request.allowNonemptyTarget());
        assertEquals("localhost:3306/ktplus", request.typedTargetConfirmation());
        assertTrue(com.monkey.ktplus.storage.migration.connection.MigrationEndpoint.mysql(
                        "LocalHost", 3306, "KTPlus")
                .matchesConfirmation(request.typedTargetConfirmation()));
        assertFalse(com.monkey.ktplus.storage.migration.connection.MigrationEndpoint.mysql(
                        "localhost", 3306, "ktplus")
                .matchesConfirmation("mysql"));
    }

    @Test
    void lockIsSingleFlight() {
        MigrationLock lock = new MigrationLock();
        assertTrue(lock.tryAcquire());
        assertFalse(lock.tryAcquire());
        assertTrue(lock.isHeld());
        lock.release();
        assertTrue(lock.tryAcquire());
        lock.release();
    }

    @Test
    void writeGateBlocksUntilUnfrozen() {
        StorageWriteGate gate = new StorageWriteGate();
        gate.requireWritable();
        gate.freezeWrites();
        assertTrue(gate.writesFrozen());
        assertThrows(StorageWriteFrozenException.class, gate::requireWritable);
        gate.unfreezeWrites();
        assertFalse(gate.writesFrozen());
        gate.requireWritable();
    }

    @Test
    void phaseResultStatuses() {
        assertTrue(PhaseResult.success("ok").succeeded());
        assertFalse(PhaseResult.skipped("skip").failed());
        assertTrue(PhaseResult.failed("boom").failed());
        assertEquals(PhaseStatus.SKIPPED, PhaseResult.skipped("skip").status());
    }

    @Test
    void contextRecordsPhaseAndCounts() {
        MigrationRequest request = MigrationRequest.builder()
                .sourceDialect(MigrationDialect.SQLITE)
                .targetDialect(MigrationDialect.MYSQL)
                .typedTargetConfirmation("mysql")
                .build();
        MigrationContext context = new MigrationContext(request, new StorageWriteGate(), Path.of("backups"));
        context.recordPhase("preflight", PhaseResult.success("ready"));
        context.putExportedRowCount("kt_killcoins", 3L);
        context.putImportedRowCount("kt_killcoins", 3L);
        context.putSkippedRowCount("kt_purchases", 1L);
        context.addWarning("note");
        context.markWritesFrozen();
        assertEquals(1, context.phaseResults().size());
        assertEquals(3L, context.exportedRowCounts().get("kt_killcoins").longValue());
        assertEquals(3L, context.importedRowCounts().get("kt_killcoins").longValue());
        assertEquals(1L, context.skippedRowCounts().get("kt_purchases").longValue());
        assertEquals(1, context.warnings().size());
        assertTrue(context.writesWereFrozen());
    }
}
