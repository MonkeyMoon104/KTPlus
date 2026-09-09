package com.monkey.ktplus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.storage.importkt.ClassicKtImportRequest;
import com.monkey.ktplus.storage.importkt.ClassicKtImportResult;
import com.monkey.ktplus.storage.importkt.ClassicKtImporter;
import com.monkey.ktplus.storage.migration.writegate.StorageWriteGate;
import com.monkey.ktplus.storage.schema.FlywaySchemaMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClassicKtImporterTest {
    private Path workDir;
    private Path ktFolder;
    private Path targetDb;
    private HikariDataSource dataSource;
    private StorageWriteGate writeGate;
    private DatabaseService database;

    private final UUID playerA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID playerB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() throws Exception {
        workDir = Files.createTempDirectory("ktplus-import-kt-test-");
        ktFolder = workDir.resolve("KT");
        Files.createDirectories(ktFolder);
        targetDb = workDir.resolve("ktplus.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + targetDb.toAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setPoolName("KTImportTest");
        dataSource = new HikariDataSource(config);
        new FlywaySchemaMigrator(dataSource).migrate();
        writeGate = new StorageWriteGate();
        database = new DatabaseService(writeGate);
        database.attachRunningStateForTests("sqlite", dataSource);

        seedClassicKt();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dataSource != null) {
            dataSource.close();
        }
        deleteRecursive(workDir);
    }

    @Test
    void dryRunDoesNotWrite() throws Exception {
        ClassicKtImporter importer = new ClassicKtImporter(database, Set.of("fire", "lightning"), false);
        ClassicKtImportRequest request =
                ClassicKtImportRequest.parse(ktFolder, new String[] {"--dry-run"});
        ClassicKtImportResult result = importer.importData(request);

        assertTrue(result.success());
        assertEquals(2, result.balancesRead());
        assertEquals(3, result.purchasesRead());
        assertEquals(2, result.selectionsRead());
        assertEquals(1, result.purchasesUnknown());
        assertEquals(0, result.selectionsUnknown());
        assertEquals(0, countRows("kt_killcoins"));
        assertEquals(0, countRows("kt_purchases"));
        assertEquals(0, countRows("kt_player_effects"));
    }

    @Test
    void importWritesAndSkipsDuplicates() throws Exception {
        seedExistingTargetRows();

        ClassicKtImporter importer = new ClassicKtImporter(database, Set.of("fire", "lightning"), false);
        String token = ClassicKtImportRequest.confirmationTokenFor(ktFolder);
        ClassicKtImportResult result =
                importer.importData(ClassicKtImportRequest.parse(ktFolder, new String[] {token}));

        assertTrue(result.success());
        assertEquals(1, result.balancesImported());
        assertEquals(1, result.balancesSkipped());
        assertEquals(1, result.purchasesImported());
        assertEquals(1, result.purchasesSkipped());
        assertEquals(1, result.selectionsImported());
        assertEquals(1, result.selectionsSkipped());
        assertEquals(1, result.purchasesUnknown());
        assertEquals(0, result.selectionsUnknown());

        assertEquals(50L, balanceOf(playerA));
        assertEquals(200L, balanceOf(playerB));
        assertTrue(purchaseExists(playerA, "fire"));
        assertTrue(purchaseExists(playerB, "lightning"));
        assertFalse(purchaseExists(playerB, "legacy_gone"));
        assertEquals("fire", selectionOf(playerA));
        assertEquals("lightning", selectionOf(playerB));
    }

    @Test
    void overwriteBalancesReplacesExisting() throws Exception {
        seedExistingTargetRows();

        ClassicKtImporter importer = new ClassicKtImporter(database, Set.of("fire", "lightning"), false);
        String token = ClassicKtImportRequest.confirmationTokenFor(ktFolder);
        ClassicKtImportResult result = importer.importData(
                ClassicKtImportRequest.parse(ktFolder, new String[] {"--overwrite-balances", token}));

        assertTrue(result.success());
        assertEquals(2, result.balancesImported());
        assertEquals(0, result.balancesSkipped());
        assertEquals(100L, balanceOf(playerA));
        assertEquals(200L, balanceOf(playerB));
    }

    @Test
    void requiresConfirmationToken() {
        ClassicKtImporter importer = new ClassicKtImporter(database, Set.of("fire"), false);
        ClassicKtImportResult result =
                importer.importData(ClassicKtImportRequest.parse(ktFolder, new String[] {}));
        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("confirmation"));
        assertEquals(0, countRows("kt_killcoins"));
    }

    private void seedClassicKt() throws Exception {
        Path ktDb = ktFolder.resolve("kt.db");
        Path coinsDb = ktFolder.resolve("killcoins.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + ktDb.toAbsolutePath());
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE killeffects (uuid VARCHAR(36) PRIMARY KEY, effect TEXT NOT NULL)");
            statement.executeUpdate(
                    "INSERT INTO killeffects (uuid, effect) VALUES ('"
                            + playerA
                            + "', 'fire')");
            statement.executeUpdate(
                    "INSERT INTO killeffects (uuid, effect) VALUES ('"
                            + playerB
                            + "', 'lightning')");
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + coinsDb.toAbsolutePath());
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE killcoins_balance (uuid VARCHAR(36) PRIMARY KEY, balance REAL NOT NULL)");
            statement.executeUpdate(
                    "CREATE TABLE killcoins_purchases (uuid VARCHAR(36) NOT NULL, effect TEXT NOT NULL, "
                            + "PRIMARY KEY (uuid, effect))");
            statement.executeUpdate(
                    "INSERT INTO killcoins_balance (uuid, balance) VALUES ('" + playerA + "', 100.4)");
            statement.executeUpdate(
                    "INSERT INTO killcoins_balance (uuid, balance) VALUES ('" + playerB + "', 200.0)");
            statement.executeUpdate(
                    "INSERT INTO killcoins_purchases (uuid, effect) VALUES ('" + playerA + "', 'fire')");
            statement.executeUpdate(
                    "INSERT INTO killcoins_purchases (uuid, effect) VALUES ('"
                            + playerB
                            + "', 'legacy_gone')");
            statement.executeUpdate(
                    "INSERT INTO killcoins_purchases (uuid, effect) VALUES ('"
                            + playerB
                            + "', 'lightning')");
        }
    }

    private void seedExistingTargetRows() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO kt_killcoins (uuid, balance) VALUES ('" + playerA + "', 50)");
            statement.executeUpdate(
                    "INSERT INTO kt_purchases (uuid, effect_id) VALUES ('" + playerA + "', 'fire')");
            statement.executeUpdate(
                    "INSERT INTO kt_player_effects (uuid, effect_id) VALUES ('" + playerA + "', 'fire')");
        }
    }

    private long countRows(String table) {
        return database.queryOne("SELECT COUNT(*) AS c FROM " + table, null, rs -> rs.getLong("c"))
                .orElse(0L);
    }

    private long balanceOf(UUID uuid) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("SELECT balance FROM kt_killcoins WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getLong("balance");
            }
        }
    }

    private boolean purchaseExists(UUID uuid, String effectId) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM kt_purchases WHERE uuid = ? AND effect_id = ?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, effectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String selectionOf(UUID uuid) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("SELECT effect_id FROM kt_player_effects WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getString("effect_id");
            }
        }
    }

    private static void deleteRecursive(Path root) throws Exception {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // best effort
                }
            });
        }
    }
}
