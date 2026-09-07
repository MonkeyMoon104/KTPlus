package com.monkey.ktplus.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KillCoinsPurchaseFreezeTest {
    private Path dbFile;
    private HikariDataSource dataSource;
    private StorageWriteGate writeGate;
    private DatabaseService database;
    private KillCoinsRepository killCoins;
    private PurchaseRepository purchases;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = Files.createTempFile("ktplus-freeze-", ".db");
        Files.deleteIfExists(dbFile);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
        config.setMaximumPoolSize(2);
        config.setPoolName("KTFreezeTest");
        dataSource = new HikariDataSource(config);
        new FlywaySchemaMigrator(dataSource).migrate();
        writeGate = new StorageWriteGate();
        database = new DatabaseService(writeGate);
        database.attachRunningStateForTests("sqlite", dataSource);
        purchases = new PurchaseRepository(database);
        killCoins = new KillCoinsRepository(database, purchases, 0);
    }

    @AfterEach
    void tearDown() throws Exception {
        writeGate.unfreezeWrites();
        if (dataSource != null) {
            dataSource.close();
        }
        Files.deleteIfExists(dbFile);
    }

    @Test
    void freezeDuringOpenPurchaseTransactionDoesNotAbortInFlightWork_andNewWritesAreBlocked()
            throws Exception {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 500L);

        CountDownLatch enteredTransaction = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        AtomicBoolean purchaseFinished = new AtomicBoolean(false);
        AtomicReference<Throwable> purchaseError = new AtomicReference<Throwable>();

        Thread purchaseThread = new Thread(() -> {
            try {
                database.transaction(connection -> {
                    boolean claimed = purchases.tryClaimOn(connection, player, "fire");
                    assertTrue(claimed);
                    enteredTransaction.countDown();
                    try {
                        assertTrue(allowCommit.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("interrupted while waiting to commit purchase", interrupted);
                    }
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE kt_killcoins SET balance = balance - ? WHERE uuid = ? AND balance >= ?")) {
                        update.setLong(1, 100L);
                        update.setString(2, player.toString());
                        update.setLong(3, 100L);
                        assertEquals(1, update.executeUpdate());
                    }
                });
                purchaseFinished.set(true);
            } catch (Throwable error) {
                purchaseError.set(error);
            }
        });
        purchaseThread.start();

        assertTrue(enteredTransaction.await(5, TimeUnit.SECONDS));
        writeGate.freezeWrites();
        assertTrue(writeGate.writesFrozen());

        assertThrows(StorageWriteFrozenException.class, () -> database.transaction(connection -> {
        }));
        assertThrows(StorageWriteFrozenException.class, () -> killCoins.tryPurchase(UUID.randomUUID(), "smoke", 10L));

        allowCommit.countDown();
        purchaseThread.join(5_000L);
        assertEquals(null, purchaseError.get());
        assertTrue(purchaseFinished.get());

        writeGate.unfreezeWrites();
        assertTrue(purchases.hasPurchase(player, "fire"));
        long persistedBalance = database
                .queryOne(
                        "SELECT balance FROM kt_killcoins WHERE uuid = ?",
                        statement -> statement.setString(1, player.toString()),
                        resultSet -> resultSet.getLong("balance"))
                .orElse(-1L);
        assertEquals(400L, persistedBalance);
    }

    @Test
    void failedWithdrawInsideOpenTransactionRollsBackClaimedPurchase() {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 50L);
        assertFalse(killCoins.tryPurchase(player, "wither", 100L));
        assertFalse(purchases.hasPurchase(player, "wither"));
        assertEquals(50L, killCoins.balance(player));
    }

    @Test
    void successfulTryPurchaseIsAtomicWhenGateIsOpen() {
        UUID player = UUID.randomUUID();
        killCoins.setBalance(player, 250L);
        assertTrue(killCoins.tryPurchase(player, "hearts", 100L));
        assertTrue(purchases.hasPurchase(player, "hearts"));
        assertEquals(150L, killCoins.balance(player));
    }
}
