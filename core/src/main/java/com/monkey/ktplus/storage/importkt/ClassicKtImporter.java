package com.monkey.ktplus.storage.importkt;

import com.monkey.ktplus.storage.DatabaseService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public final class ClassicKtImporter {
    private final DatabaseService database;
    private final Map<String, String> knownEffectIdsByLower;
    private final boolean vaultEconomy;

    public ClassicKtImporter(DatabaseService database, Set<String> knownEffectIds, boolean vaultEconomy) {
        this.database = Objects.requireNonNull(database, "database");
        this.knownEffectIdsByLower = normalizeIds(knownEffectIds);
        this.vaultEconomy = vaultEconomy;
    }

    public ClassicKtImportResult importData(ClassicKtImportRequest request) {
        Objects.requireNonNull(request, "request");
        String token = ClassicKtImportRequest.confirmationTokenFor(request.ktFolder());
        ClassicKtImportResult.Builder result = ClassicKtImportResult.builder().confirmationToken(token);

        Path ktFolder = request.ktFolder();
        if (!Files.isDirectory(ktFolder)) {
            return result.success(false)
                    .message("Classic KT folder not found: " + token)
                    .addMessage("Expected folder: " + token)
                    .addMessage("Place the old KT data under plugins/KT/ then retry.")
                    .build();
        }

        Path ktDb = ktFolder.resolve("kt.db");
        Path killcoinsDb = ktFolder.resolve("killcoins.db");
        if (!Files.isRegularFile(ktDb) && !Files.isRegularFile(killcoinsDb)) {
            return result.success(false)
                    .message("No classic KT SQLite files found under " + token)
                    .addMessage("Looked for kt.db and killcoins.db")
                    .build();
        }

        if (!request.dryRun()) {
            String typed = request.typedConfirmation();
            if (typed == null || typed.isBlank()) {
                return result.success(false)
                        .message("Confirmation token required")
                        .addMessage("Run with --dry-run first, then repeat with token: " + token)
                        .build();
            }
            if (!token.equalsIgnoreCase(typed.replace('\\', '/'))) {
                return result.success(false)
                        .message("Confirmation token mismatch")
                        .addMessage("Expected: " + token)
                        .addMessage("Got: " + typed.replace('\\', '/'))
                        .build();
            }
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("ktplus-import-kt-");
            Path snapKt = snapshotIfPresent(ktDb, workDir.resolve("kt.db"));
            Path snapCoins = snapshotIfPresent(killcoinsDb, workDir.resolve("killcoins.db"));

            SourceLayout layout = resolveLayout(snapKt, snapCoins);
            if (layout == null) {
                return result.success(false)
                        .message("Unrecognized classic KT schema")
                        .addMessage("Need killeffects and/or killcoins_balance + killcoins_purchases tables")
                        .build();
            }

            List<BalanceRow> balances = readBalances(layout.coinsDb());
            List<PurchaseRow> purchases = readPurchases(layout.coinsDb());
            List<SelectionRow> selections = readSelections(layout.selectionDb());

            Set<String> unknown = new LinkedHashSet<String>();
            long purchasesUnknown = 0L;
            long selectionsUnknown = 0L;
            List<PurchaseRow> validPurchases = new ArrayList<PurchaseRow>();
            for (PurchaseRow row : purchases) {
                String canonical = knownEffectIdsByLower.get(row.effectId().toLowerCase(Locale.ROOT));
                if (canonical == null) {
                    purchasesUnknown++;
                    unknown.add(row.effectId());
                    continue;
                }
                validPurchases.add(new PurchaseRow(row.uuid(), canonical));
            }
            List<SelectionRow> validSelections = new ArrayList<SelectionRow>();
            for (SelectionRow row : selections) {
                String canonical = knownEffectIdsByLower.get(row.effectId().toLowerCase(Locale.ROOT));
                if (canonical == null) {
                    selectionsUnknown++;
                    unknown.add(row.effectId());
                    continue;
                }
                validSelections.add(new SelectionRow(row.uuid(), canonical));
            }

            result.balancesRead(balances.size())
                    .purchasesRead(purchases.size())
                    .selectionsRead(selections.size())
                    .purchasesUnknown(purchasesUnknown)
                    .selectionsUnknown(selectionsUnknown)
                    .unknownEffectIds(new ArrayList<String>(unknown));

            result.addMessage("Source folder: " + token);
            result.addMessage(
                    "Layout: selection="
                            + fileName(layout.selectionDb())
                            + " | coins="
                            + fileName(layout.coinsDb()));
            if (vaultEconomy) {
                result.addMessage(
                        "Warning: economy.provider is VAULT — imported KillCoins balances will not be used in-game.");
            }

            if (request.dryRun()) {
                long wouldImportBalances = balances.size();
                long wouldSkipBalances = 0L;
                if (!request.overwriteBalances()) {
                    for (BalanceRow row : balances) {
                        if (balanceExists(row.uuid())) {
                            wouldSkipBalances++;
                            wouldImportBalances--;
                        }
                    }
                }
                long wouldImportPurchases = 0L;
                long wouldSkipPurchases = 0L;
                for (PurchaseRow row : validPurchases) {
                    if (purchaseExists(row.uuid(), row.effectId())) {
                        wouldSkipPurchases++;
                    } else {
                        wouldImportPurchases++;
                    }
                }
                long wouldImportSelections = 0L;
                long wouldSkipSelections = 0L;
                for (SelectionRow row : validSelections) {
                    if (selectionExists(row.uuid())) {
                        wouldSkipSelections++;
                    } else {
                        wouldImportSelections++;
                    }
                }
                result.balancesImported(wouldImportBalances)
                        .balancesSkipped(wouldSkipBalances)
                        .purchasesImported(wouldImportPurchases)
                        .purchasesSkipped(wouldSkipPurchases)
                        .selectionsImported(wouldImportSelections)
                        .selectionsSkipped(wouldSkipSelections)
                        .success(true)
                        .message("Dry-run OK")
                        .addMessage(
                                "Balances: read="
                                        + balances.size()
                                        + " import="
                                        + wouldImportBalances
                                        + " skip="
                                        + wouldSkipBalances)
                        .addMessage(
                                "Purchases: read="
                                        + purchases.size()
                                        + " import="
                                        + wouldImportPurchases
                                        + " skip="
                                        + wouldSkipPurchases
                                        + " unknown="
                                        + purchasesUnknown)
                        .addMessage(
                                "Selections: read="
                                        + selections.size()
                                        + " import="
                                        + wouldImportSelections
                                        + " skip="
                                        + wouldSkipSelections
                                        + " unknown="
                                        + selectionsUnknown)
                        .addMessage("To apply, run: /kt import-kt " + (request.overwriteBalances()
                                ? "--overwrite-balances "
                                : "")
                                + token);
                return result.build();
            }

            WriteCounts counts = writeAll(balances, validPurchases, validSelections, request.overwriteBalances());
            result.balancesImported(counts.balancesImported)
                    .balancesSkipped(counts.balancesSkipped)
                    .purchasesImported(counts.purchasesImported)
                    .purchasesSkipped(counts.purchasesSkipped)
                    .selectionsImported(counts.selectionsImported)
                    .selectionsSkipped(counts.selectionsSkipped)
                    .success(true)
                    .message("Import OK")
                    .addMessage(
                            "Balances: imported="
                                    + counts.balancesImported
                                    + " skipped="
                                    + counts.balancesSkipped)
                    .addMessage(
                            "Purchases: imported="
                                    + counts.purchasesImported
                                    + " skipped="
                                    + counts.purchasesSkipped
                                    + " unknown="
                                    + purchasesUnknown)
                    .addMessage(
                            "Selections: imported="
                                    + counts.selectionsImported
                                    + " skipped="
                                    + counts.selectionsSkipped
                                    + " unknown="
                                    + selectionsUnknown);
            if (!unknown.isEmpty()) {
                result.addMessage("Unknown effect ids skipped (sample): "
                        + String.join(", ", unknown.stream().limit(12).toList()));
            }
            return result.build();
        } catch (Exception error) {
            return result.success(false)
                    .message("Import failed: " + error.getMessage())
                    .addMessage(String.valueOf(error.getMessage()))
                    .build();
        } finally {
            deleteRecursive(workDir);
        }
    }

    private WriteCounts writeAll(
            List<BalanceRow> balances,
            List<PurchaseRow> purchases,
            List<SelectionRow> selections,
            boolean overwriteBalances) {
        WriteCounts counts = new WriteCounts();
        String dialect = database.dialect();
        database.transaction(connection -> {
            for (BalanceRow row : balances) {
                if (!overwriteBalances && existsOn(connection, "SELECT 1 FROM kt_killcoins WHERE uuid = ?", row.uuid())) {
                    counts.balancesSkipped++;
                    continue;
                }
                if (overwriteBalances) {
                    try (PreparedStatement delete =
                            connection.prepareStatement("DELETE FROM kt_killcoins WHERE uuid = ?")) {
                        delete.setString(1, row.uuid().toString());
                        delete.executeUpdate();
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement(insertBalanceSql(dialect))) {
                    insert.setString(1, row.uuid().toString());
                    insert.setLong(2, row.balance());
                    int updated = insert.executeUpdate();
                    if (updated > 0) {
                        counts.balancesImported++;
                    } else {
                        counts.balancesSkipped++;
                    }
                }
            }
            for (PurchaseRow row : purchases) {
                try (PreparedStatement insert = connection.prepareStatement(insertPurchaseSql(dialect))) {
                    insert.setString(1, row.uuid().toString());
                    insert.setString(2, row.effectId());
                    int updated = insert.executeUpdate();
                    if (updated > 0) {
                        counts.purchasesImported++;
                    } else {
                        counts.purchasesSkipped++;
                    }
                }
            }
            for (SelectionRow row : selections) {
                if (existsOn(connection, "SELECT 1 FROM kt_player_effects WHERE uuid = ?", row.uuid())) {
                    counts.selectionsSkipped++;
                    continue;
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO kt_player_effects (uuid, effect_id) VALUES (?, ?)")) {
                    insert.setString(1, row.uuid().toString());
                    insert.setString(2, row.effectId());
                    insert.executeUpdate();
                    counts.selectionsImported++;
                }
            }
        });
        return counts;
    }

    private boolean balanceExists(UUID uuid) {
        return database.queryOne(
                        "SELECT 1 FROM kt_killcoins WHERE uuid = ?",
                        statement -> statement.setString(1, uuid.toString()),
                        resultSet -> Boolean.TRUE)
                .isPresent();
    }

    private boolean purchaseExists(UUID uuid, String effectId) {
        return database.queryOne(
                        "SELECT 1 FROM kt_purchases WHERE uuid = ? AND effect_id = ?",
                        statement -> {
                            statement.setString(1, uuid.toString());
                            statement.setString(2, effectId);
                        },
                        resultSet -> Boolean.TRUE)
                .isPresent();
    }

    private boolean selectionExists(UUID uuid) {
        return database.queryOne(
                        "SELECT 1 FROM kt_player_effects WHERE uuid = ?",
                        statement -> statement.setString(1, uuid.toString()),
                        resultSet -> Boolean.TRUE)
                .isPresent();
    }

    private static boolean existsOn(Connection connection, String sql, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static String insertBalanceSql(String dialect) {
        if ("sqlite".equals(dialect)) {
            return "INSERT OR IGNORE INTO kt_killcoins (uuid, balance) VALUES (?, ?)";
        }
        if ("postgresql".equals(dialect) || "postgres".equals(dialect)) {
            return "INSERT INTO kt_killcoins (uuid, balance) VALUES (?, ?) ON CONFLICT (uuid) DO NOTHING";
        }
        return "INSERT IGNORE INTO kt_killcoins (uuid, balance) VALUES (?, ?)";
    }

    private static String insertPurchaseSql(String dialect) {
        if ("sqlite".equals(dialect)) {
            return "INSERT OR IGNORE INTO kt_purchases (uuid, effect_id) VALUES (?, ?)";
        }
        if ("postgresql".equals(dialect) || "postgres".equals(dialect)) {
            return "INSERT INTO kt_purchases (uuid, effect_id) VALUES (?, ?) ON CONFLICT (uuid, effect_id) DO NOTHING";
        }
        return "INSERT IGNORE INTO kt_purchases (uuid, effect_id) VALUES (?, ?)";
    }

    private static @Nullable Path snapshotIfPresent(Path source, Path target) throws IOException {
        if (!Files.isRegularFile(source)) {
            return null;
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static @Nullable SourceLayout resolveLayout(@Nullable Path ktDb, @Nullable Path killcoinsDb)
            throws SQLException {
        boolean ktHasEffects = ktDb != null && tableExists(ktDb, "killeffects");
        boolean ktHasCoins = ktDb != null
                && tableExists(ktDb, "killcoins_balance")
                && tableExists(ktDb, "killcoins_purchases");
        boolean separateCoins = killcoinsDb != null
                && tableExists(killcoinsDb, "killcoins_balance")
                && tableExists(killcoinsDb, "killcoins_purchases");

        if (ktHasEffects && separateCoins) {
            return new SourceLayout(ktDb, killcoinsDb);
        }
        if (ktHasEffects && ktHasCoins) {
            return new SourceLayout(ktDb, ktDb);
        }
        if (separateCoins && !ktHasEffects) {
            return new SourceLayout(killcoinsDb, killcoinsDb);
        }
        if (ktHasCoins && !ktHasEffects) {
            return new SourceLayout(ktDb, ktDb);
        }
        if (ktHasEffects) {
            return new SourceLayout(ktDb, ktDb);
        }
        return null;
    }

    private static boolean tableExists(Path dbFile, String table) throws SQLException {
        try (Connection connection = openSqlite(dbFile);
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static List<BalanceRow> readBalances(@Nullable Path dbFile) throws SQLException {
        List<BalanceRow> rows = new ArrayList<BalanceRow>();
        if (dbFile == null || !tableExists(dbFile, "killcoins_balance")) {
            return rows;
        }
        try (Connection connection = openSqlite(dbFile);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT uuid, balance FROM killcoins_balance")) {
            while (resultSet.next()) {
                UUID uuid = parseUuid(resultSet.getString("uuid"));
                if (uuid == null) {
                    continue;
                }
                long balance = Math.max(0L, Math.round(resultSet.getDouble("balance")));
                rows.add(new BalanceRow(uuid, balance));
            }
        }
        return rows;
    }

    private static List<PurchaseRow> readPurchases(@Nullable Path dbFile) throws SQLException {
        List<PurchaseRow> rows = new ArrayList<PurchaseRow>();
        if (dbFile == null || !tableExists(dbFile, "killcoins_purchases")) {
            return rows;
        }
        try (Connection connection = openSqlite(dbFile);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT uuid, effect FROM killcoins_purchases")) {
            while (resultSet.next()) {
                UUID uuid = parseUuid(resultSet.getString("uuid"));
                String effect = resultSet.getString("effect");
                if (uuid == null || effect == null || effect.isBlank()) {
                    continue;
                }
                rows.add(new PurchaseRow(uuid, effect.trim()));
            }
        }
        return rows;
    }

    private static List<SelectionRow> readSelections(@Nullable Path dbFile) throws SQLException {
        List<SelectionRow> rows = new ArrayList<SelectionRow>();
        if (dbFile == null || !tableExists(dbFile, "killeffects")) {
            return rows;
        }
        try (Connection connection = openSqlite(dbFile);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT uuid, effect FROM killeffects")) {
            while (resultSet.next()) {
                UUID uuid = parseUuid(resultSet.getString("uuid"));
                String effect = resultSet.getString("effect");
                if (uuid == null || effect == null || effect.isBlank()) {
                    continue;
                }
                rows.add(new SelectionRow(uuid, effect.trim()));
            }
        }
        return rows;
    }

    private static Connection openSqlite(Path dbFile) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            // Driver may already be loaded by Paper/Hikari.
        }
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
    }

    private static @Nullable UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Map<String, String> normalizeIds(Set<String> knownEffectIds) {
        Map<String, String> normalized = new HashMap<String, String>();
        for (String id : Objects.requireNonNull(knownEffectIds, "knownEffectIds")) {
            if (id != null && !id.isBlank()) {
                normalized.putIfAbsent(id.toLowerCase(Locale.ROOT), id);
            }
        }
        return Map.copyOf(normalized);
    }

    private static String fileName(@Nullable Path path) {
        return path == null ? "none" : String.valueOf(path.getFileName());
    }

    private static void deleteRecursive(@Nullable Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            });
        } catch (IOException ignored) {
            // best effort cleanup
        }
    }

    private record SourceLayout(Path selectionDb, Path coinsDb) {}

    private record BalanceRow(UUID uuid, long balance) {}

    private record PurchaseRow(UUID uuid, String effectId) {}

    private record SelectionRow(UUID uuid, String effectId) {}

    private static final class WriteCounts {
        private long balancesImported;
        private long balancesSkipped;
        private long purchasesImported;
        private long purchasesSkipped;
        private long selectionsImported;
        private long selectionsSkipped;
    }
}
