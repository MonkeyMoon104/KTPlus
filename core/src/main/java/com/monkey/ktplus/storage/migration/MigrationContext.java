package com.monkey.ktplus.storage.migration;

import com.monkey.ktplus.storage.migration.connection.MigrationEndpoint;
import com.monkey.ktplus.storage.migration.snapshot.SnapshotManifest;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprint;
import com.monkey.ktplus.storage.migration.writegate.StorageWriteGate;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;

public final class MigrationContext {
    private final MigrationRequest request;
    private final StorageWriteGate writeGate;
    private final Path backupsDirectory;
    private final Map<String, PhaseResult> phaseResults = new LinkedHashMap<String, PhaseResult>();
    private final Map<String, Long> exportedRowCounts = new LinkedHashMap<String, Long>();
    private final Map<String, Long> importedRowCounts = new LinkedHashMap<String, Long>();
    private final Map<String, Long> skippedRowCounts = new LinkedHashMap<String, Long>();
    private final List<String> warnings = new ArrayList<String>();
    private final List<String> operatorMessages = new ArrayList<String>();

    private @Nullable DataSource sourceDataSource;
    private @Nullable DataSource targetDataSource;
    private @Nullable MigrationEndpoint sourceEndpoint;
    private @Nullable MigrationEndpoint targetEndpoint;
    private @Nullable Path snapshotDirectory;
    private @Nullable SnapshotManifest snapshotManifest;
    private @Nullable Path exportDirectory;
    private @Nullable Map<String, TableFingerprint> exportFingerprints;
    private @Nullable Path reportPath;
    private boolean writesWereFrozen;
    private boolean ownsSourceDataSource;
    private boolean ownsTargetDataSource;
    private boolean ownsMigrationLock;
    private boolean validationPassed;
    private boolean economicValidationHardFailed;
    private boolean configSwapped;
    private boolean targetWritesAttempted;

    public MigrationContext(MigrationRequest request, StorageWriteGate writeGate, Path backupsDirectory) {
        this.request = Objects.requireNonNull(request, "request");
        this.writeGate = Objects.requireNonNull(writeGate, "writeGate");
        this.backupsDirectory = Objects.requireNonNull(backupsDirectory, "backupsDirectory");
    }

    public MigrationRequest request() {
        return request;
    }

    public StorageWriteGate writeGate() {
        return writeGate;
    }

    public Path backupsDirectory() {
        return backupsDirectory;
    }

    public @Nullable DataSource sourceDataSource() {
        return sourceDataSource;
    }

    public void setSourceDataSource(@Nullable DataSource sourceDataSource, boolean owned) {
        this.sourceDataSource = sourceDataSource;
        this.ownsSourceDataSource = owned;
    }

    public boolean ownsSourceDataSource() {
        return ownsSourceDataSource;
    }

    public @Nullable DataSource targetDataSource() {
        return targetDataSource;
    }

    public void setTargetDataSource(@Nullable DataSource targetDataSource, boolean owned) {
        this.targetDataSource = targetDataSource;
        this.ownsTargetDataSource = owned;
    }

    public boolean ownsTargetDataSource() {
        return ownsTargetDataSource;
    }

    public @Nullable MigrationEndpoint sourceEndpoint() {
        return sourceEndpoint;
    }

    public void setSourceEndpoint(@Nullable MigrationEndpoint sourceEndpoint) {
        this.sourceEndpoint = sourceEndpoint;
    }

    public @Nullable MigrationEndpoint targetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(@Nullable MigrationEndpoint targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public @Nullable Path snapshotDirectory() {
        return snapshotDirectory;
    }

    public void setSnapshotDirectory(@Nullable Path snapshotDirectory) {
        this.snapshotDirectory = snapshotDirectory;
    }

    public @Nullable SnapshotManifest snapshotManifest() {
        return snapshotManifest;
    }

    public void setSnapshotManifest(@Nullable SnapshotManifest snapshotManifest) {
        this.snapshotManifest = snapshotManifest;
    }

    public @Nullable Path exportDirectory() {
        return exportDirectory;
    }

    public void setExportDirectory(@Nullable Path exportDirectory) {
        this.exportDirectory = exportDirectory;
    }

    public @Nullable Map<String, TableFingerprint> exportFingerprints() {
        return exportFingerprints == null ? null : Collections.unmodifiableMap(exportFingerprints);
    }

    public void setExportFingerprints(@Nullable Map<String, TableFingerprint> exportFingerprints) {
        this.exportFingerprints = exportFingerprints == null
                ? null
                : new LinkedHashMap<String, TableFingerprint>(exportFingerprints);
    }

    public @Nullable Path reportPath() {
        return reportPath;
    }

    public void setReportPath(@Nullable Path reportPath) {
        this.reportPath = reportPath;
    }

    public boolean writesWereFrozen() {
        return writesWereFrozen;
    }

    public void markWritesFrozen() {
        this.writesWereFrozen = true;
    }

    public boolean ownsMigrationLock() {
        return ownsMigrationLock;
    }

    public void markOwnsMigrationLock() {
        this.ownsMigrationLock = true;
    }

    public void clearOwnsMigrationLock() {
        this.ownsMigrationLock = false;
    }

    public void markValidationResult(boolean passed, boolean economicHardFail) {
        this.validationPassed = passed;
        this.economicValidationHardFailed = economicHardFail;
    }

    public boolean validationPassed() {
        return validationPassed;
    }

    public boolean economicValidationHardFailed() {
        return economicValidationHardFailed;
    }

    public void markConfigSwapped(boolean swapped) {
        this.configSwapped = swapped;
    }

    public boolean configSwapped() {
        return configSwapped;
    }

    public void markTargetWritesAttempted() {
        this.targetWritesAttempted = true;
    }

    public boolean targetWritesAttempted() {
        return targetWritesAttempted;
    }

    public void recordPhase(String phaseName, PhaseResult result) {
        Objects.requireNonNull(phaseName, "phaseName");
        Objects.requireNonNull(result, "result");
        phaseResults.put(phaseName, result);
    }

    public Map<String, PhaseResult> phaseResults() {
        return Collections.unmodifiableMap(phaseResults);
    }

    public void putExportedRowCount(String table, long count) {
        exportedRowCounts.put(table, Long.valueOf(count));
    }

    public void putImportedRowCount(String table, long count) {
        importedRowCounts.put(table, Long.valueOf(count));
    }

    public void putSkippedRowCount(String table, long count) {
        skippedRowCounts.put(table, Long.valueOf(count));
    }

    public Map<String, Long> exportedRowCounts() {
        return Collections.unmodifiableMap(exportedRowCounts);
    }

    public Map<String, Long> importedRowCounts() {
        return Collections.unmodifiableMap(importedRowCounts);
    }

    public Map<String, Long> skippedRowCounts() {
        return Collections.unmodifiableMap(skippedRowCounts);
    }

    public void addWarning(String warning) {
        warnings.add(Objects.requireNonNull(warning, "warning"));
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    public void addOperatorMessage(String message) {
        operatorMessages.add(Objects.requireNonNull(message, "message"));
    }

    public List<String> operatorMessages() {
        return Collections.unmodifiableList(operatorMessages);
    }
}
