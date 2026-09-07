package com.monkey.ktplus.storage.repository;

import com.monkey.ktplus.cache.BoundedCache;
import com.monkey.ktplus.storage.DatabaseService;
import java.sql.PreparedStatement;
import java.util.Objects;
import java.util.UUID;

public final class KillCoinsRepository {
    private final DatabaseService database;
    private final PurchaseRepository purchases;
    private volatile int startingBalance;
    private final BoundedCache<UUID, Long> balanceCache = BoundedCache.create(2048, 300);

    public KillCoinsRepository(DatabaseService database, PurchaseRepository purchases, int startingBalance) {
        this.database = Objects.requireNonNull(database, "database");
        this.purchases = Objects.requireNonNull(purchases, "purchases");
        this.startingBalance = Math.max(0, startingBalance);
    }

    public void updateStartingBalance(int startingBalance) {
        this.startingBalance = Math.max(0, startingBalance);
        balanceCache.clear();
    }

    public long balance(UUID uuid) {
        return balanceCache.computeIfAbsent(uuid, this::loadBalance);
    }

    public void setBalance(UUID uuid, long balance) {
        database.transaction(connection -> {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM kt_killcoins WHERE uuid = ?")) {
                delete.setString(1, uuid.toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert =
                    connection.prepareStatement("INSERT INTO kt_killcoins (uuid, balance) VALUES (?, ?)")) {
                insert.setString(1, uuid.toString());
                insert.setLong(2, Math.max(0L, balance));
                insert.executeUpdate();
            }
        });
        balanceCache.put(uuid, Math.max(0L, balance));
    }

    public void addBalance(UUID uuid, long amount) {
        Objects.requireNonNull(uuid, "uuid");
        if (amount == 0L) {
            return;
        }
        long[] remaining = new long[] {0L};
        database.transaction(connection -> {
            ensureBalanceRow(connection, uuid);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE kt_killcoins SET balance = CASE WHEN balance + ? < 0 THEN 0 ELSE balance + ? END WHERE uuid = ?")) {
                update.setLong(1, amount);
                update.setLong(2, amount);
                update.setString(3, uuid.toString());
                update.executeUpdate();
            }
            try (PreparedStatement select =
                    connection.prepareStatement("SELECT balance FROM kt_killcoins WHERE uuid = ?")) {
                select.setString(1, uuid.toString());
                try (java.sql.ResultSet resultSet = select.executeQuery()) {
                    if (resultSet.next()) {
                        remaining[0] = Math.max(0L, resultSet.getLong("balance"));
                    }
                }
            }
        });
        balanceCache.put(uuid, remaining[0]);
    }

    public boolean tryWithdraw(UUID uuid, long amount) {
        Objects.requireNonNull(uuid, "uuid");
        if (amount <= 0L) {
            return true;
        }
        boolean[] success = new boolean[] {false};
        long[] remaining = new long[] {0L};
        database.transaction(connection -> {
            ensureBalanceRow(connection, uuid);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE kt_killcoins SET balance = balance - ? WHERE uuid = ? AND balance >= ?")) {
                update.setLong(1, amount);
                update.setString(2, uuid.toString());
                update.setLong(3, amount);
                success[0] = update.executeUpdate() == 1;
            }
            if (success[0]) {
                try (PreparedStatement select =
                        connection.prepareStatement("SELECT balance FROM kt_killcoins WHERE uuid = ?")) {
                    select.setString(1, uuid.toString());
                    try (java.sql.ResultSet resultSet = select.executeQuery()) {
                        if (resultSet.next()) {
                            remaining[0] = Math.max(0L, resultSet.getLong("balance"));
                        }
                    }
                }
            }
        });
        if (success[0]) {
            balanceCache.put(uuid, remaining[0]);
        } else {
            balanceCache.invalidate(uuid);
        }
        return success[0];
    }

    public boolean tryPurchase(UUID uuid, String effectId, long price) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(effectId, "effectId");
        if (price <= 0L) {
            purchases.addPurchase(uuid, effectId);
            return true;
        }
        if (purchases.hasPurchase(uuid, effectId)) {
            return true;
        }
        boolean[] alreadyOwned = new boolean[] {false};
        boolean[] newlyClaimed = new boolean[] {false};
        boolean[] withdrawSucceeded = new boolean[] {price <= 0L};
        long[] remaining = new long[] {0L};
        database.transaction(connection -> {
            if (purchases.tryClaimOn(connection, uuid, effectId)) {
                newlyClaimed[0] = true;
            } else if (purchases.existsOn(connection, uuid, effectId)) {
                alreadyOwned[0] = true;
                return;
            } else {
                return;
            }
            if (price <= 0L) {
                return;
            }
            ensureBalanceRow(connection, uuid);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE kt_killcoins SET balance = balance - ? WHERE uuid = ? AND balance >= ?")) {
                update.setLong(1, price);
                update.setString(2, uuid.toString());
                update.setLong(3, price);
                withdrawSucceeded[0] = update.executeUpdate() == 1;
            }
            if (KillCoinsPurchaseCoordinator.mustRollbackClaimedPurchase(newlyClaimed[0], withdrawSucceeded[0], price)) {
                purchases.deleteOn(connection, uuid, effectId);
                newlyClaimed[0] = false;
                return;
            }
            try (PreparedStatement select =
                    connection.prepareStatement("SELECT balance FROM kt_killcoins WHERE uuid = ?")) {
                select.setString(1, uuid.toString());
                try (java.sql.ResultSet resultSet = select.executeQuery()) {
                    if (resultSet.next()) {
                        remaining[0] = Math.max(0L, resultSet.getLong("balance"));
                    }
                }
            }
        });
        boolean success = KillCoinsPurchaseCoordinator.purchaseSucceeded(
                alreadyOwned[0], newlyClaimed[0], withdrawSucceeded[0], price);
        if (success) {
            if (alreadyOwned[0]) {
                purchases.markOwned(uuid, effectId);
            } else if (newlyClaimed[0]) {
                purchases.markOwned(uuid, effectId);
                balanceCache.put(uuid, remaining[0]);
            }
        } else {
            balanceCache.invalidate(uuid);
            purchases.invalidate(uuid, effectId);
        }
        return success;
    }

    private void ensureBalanceRow(java.sql.Connection connection, UUID uuid) throws java.sql.SQLException {
        try (PreparedStatement select =
                connection.prepareStatement("SELECT balance FROM kt_killcoins WHERE uuid = ?")) {
            select.setString(1, uuid.toString());
            try (java.sql.ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement insert =
                connection.prepareStatement("INSERT INTO kt_killcoins (uuid, balance) VALUES (?, ?)")) {
            insert.setString(1, uuid.toString());
            insert.setLong(2, Math.max(0L, startingBalance));
            insert.executeUpdate();
        }
    }

    private long loadBalance(UUID uuid) {
        return database.queryOne(
                        "SELECT balance FROM kt_killcoins WHERE uuid = ?",
                        statement -> statement.setString(1, uuid.toString()),
                        resultSet -> Math.max(0L, resultSet.getLong("balance")))
                .orElse((long) startingBalance);
    }
}
