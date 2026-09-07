package com.monkey.ktplus.storage.migration.validate;

import com.monkey.ktplus.storage.migration.transfer.MigrationTableCatalog;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprint;
import com.monkey.ktplus.storage.migration.transfer.TableFingerprinter;
import com.monkey.ktplus.storage.migration.transfer.TableSpec;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

public final class MigrationValidators {
    private MigrationValidators() {}

    public static ValidationReport validate(
            DataSource source,
            DataSource target,
            Map<String, Long> exportedRowCounts,
            Map<String, Long> importedRowCounts,
            Map<String, Long> skippedRowCounts,
            List<TableSpec> tables)
            throws SQLException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        List<String> lines = new ArrayList<String>();
        boolean passed = true;
        boolean economicHardFail = false;

        try (Connection sourceConnection = source.getConnection();
                Connection targetConnection = target.getConnection()) {
            TableFingerprint sourceKill =
                    TableFingerprinter.fingerprint(sourceConnection, MigrationTableCatalog.KILLCOINS);
            TableFingerprint targetKill =
                    TableFingerprinter.fingerprint(targetConnection, MigrationTableCatalog.KILLCOINS);
            long sourceSum = sourceKill.sumBalance() == null ? 0L : sourceKill.sumBalance().longValue();
            long targetSum = targetKill.sumBalance() == null ? 0L : targetKill.sumBalance().longValue();
            if (sourceSum != targetSum) {
                passed = false;
                economicHardFail = true;
                lines.add(
                        "HARD FAIL kt_killcoins SUM(balance): source="
                                + sourceSum
                                + " target="
                                + targetSum
                                + " — ConfigSwap must not run");
            } else {
                lines.add("OK kt_killcoins SUM(balance)=" + sourceSum);
            }

            TableFingerprint sourcePurchases =
                    TableFingerprinter.fingerprint(sourceConnection, MigrationTableCatalog.PURCHASES);
            TableFingerprint targetPurchases =
                    TableFingerprinter.fingerprint(targetConnection, MigrationTableCatalog.PURCHASES);
            long purchaseSkips =
                    skippedRowCounts.getOrDefault("kt_purchases", Long.valueOf(0L)).longValue();
            if (targetPurchases.rowCount() < sourcePurchases.rowCount()) {
                passed = false;
                economicHardFail = true;
                lines.add(
                        "HARD FAIL kt_purchases targetRows < sourceRows ("
                                + targetPurchases.rowCount()
                                + " < "
                                + sourcePurchases.rowCount()
                                + ")");
            } else if (purchaseSkips == 0L
                    && !sourcePurchases.contentChecksum().equals(targetPurchases.contentChecksum())) {
                passed = false;
                economicHardFail = true;
                lines.add("HARD FAIL kt_purchases checksum mismatch with zero skips");
            } else {
                lines.add(
                        "OK kt_purchases sourceRows="
                                + sourcePurchases.rowCount()
                                + " targetRows="
                                + targetPurchases.rowCount()
                                + " skipped="
                                + purchaseSkips);
            }

            for (TableSpec spec : tables) {
                long exported = exportedRowCounts.getOrDefault(spec.name(), Long.valueOf(0L)).longValue();
                long imported = importedRowCounts.getOrDefault(spec.name(), Long.valueOf(0L)).longValue();
                long skipped = skippedRowCounts.getOrDefault(spec.name(), Long.valueOf(0L)).longValue();
                TableFingerprint targetFp = TableFingerprinter.fingerprint(targetConnection, spec);
                if (imported + skipped != exported) {
                    passed = false;
                    lines.add(
                            "FAIL "
                                    + spec.name()
                                    + " exported="
                                    + exported
                                    + " imported="
                                    + imported
                                    + " skipped="
                                    + skipped);
                } else {
                    lines.add(
                            "OK "
                                    + spec.name()
                                    + " exported="
                                    + exported
                                    + " imported="
                                    + imported
                                    + " skipped="
                                    + skipped
                                    + " targetRows="
                                    + targetFp.rowCount());
                }
            }
        }
        return new ValidationReport(passed, economicHardFail, lines);
    }
}
