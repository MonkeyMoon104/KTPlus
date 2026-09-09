package com.monkey.ktplus.storage.repository;

import com.monkey.ktplus.cache.BoundedCache;
import com.monkey.ktplus.storage.DatabaseService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class PurchaseRepository {
    private final DatabaseService database;
    private final BoundedCache<String, Boolean> purchaseCache = BoundedCache.create(4096, 300);

    public PurchaseRepository(DatabaseService database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public boolean hasPurchase(UUID uuid, String effectId) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(effectId, "effectId");
        return purchaseCache.computeIfAbsent(purchaseKey(uuid, effectId), ignored -> loadPurchase(uuid, effectId));
    }

    public void addPurchase(UUID uuid, String effectId) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(effectId, "effectId");
        if (hasPurchase(uuid, effectId)) {
            return;
        }
        database.update("INSERT INTO kt_purchases (uuid, effect_id) VALUES (?, ?)", statement -> {
            statement.setString(1, uuid.toString());
            statement.setString(2, effectId);
        });
        markOwned(uuid, effectId);
    }

    public boolean claimPurchaseIfAbsent(UUID uuid, String effectId) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(effectId, "effectId");
        if (hasPurchase(uuid, effectId)) {
            return false;
        }
        boolean[] claimed = new boolean[] {false};
        database.transaction(connection -> {
            claimed[0] = tryClaimOn(connection, uuid, effectId);
        });
        if (claimed[0]) {
            markOwned(uuid, effectId);
            return true;
        }
        invalidate(uuid, effectId);
        return false;
    }

    public void removePurchase(UUID uuid, String effectId) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(effectId, "effectId");
        database.update(
                "DELETE FROM kt_purchases WHERE uuid = ? AND effect_id = ?",
                statement -> {
                    statement.setString(1, uuid.toString());
                    statement.setString(2, effectId);
                });
        invalidate(uuid, effectId);
    }

    public boolean existsOn(Connection connection, UUID uuid, String effectId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(effectId, "effectId");
        try (PreparedStatement owned = connection.prepareStatement(
                "SELECT effect_id FROM kt_purchases WHERE uuid = ? AND effect_id = ?")) {
            owned.setString(1, uuid.toString());
            owned.setString(2, effectId);
            try (java.sql.ResultSet resultSet = owned.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean tryClaimOn(Connection connection, UUID uuid, String effectId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(effectId, "effectId");
        if (existsOn(connection, uuid, effectId)) {
            return false;
        }
        try (PreparedStatement insert = connection.prepareStatement(PurchaseClaimSql.forDialect(database.dialect()))) {
            insert.setString(1, uuid.toString());
            insert.setString(2, effectId);
            return insert.executeUpdate() == 1;
        }
    }

    public void insertOn(Connection connection, UUID uuid, String effectId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(effectId, "effectId");
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO kt_purchases (uuid, effect_id) VALUES (?, ?)")) {
            insert.setString(1, uuid.toString());
            insert.setString(2, effectId);
            insert.executeUpdate();
        }
    }

    public void deleteOn(Connection connection, UUID uuid, String effectId) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(effectId, "effectId");
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM kt_purchases WHERE uuid = ? AND effect_id = ?")) {
            delete.setString(1, uuid.toString());
            delete.setString(2, effectId);
            delete.executeUpdate();
        }
    }

    public void markOwned(UUID uuid, String effectId) {
        purchaseCache.put(purchaseKey(uuid, effectId), true);
    }

    public void invalidate(UUID uuid, String effectId) {
        purchaseCache.invalidate(purchaseKey(uuid, effectId));
    }

    public void clearCache() {
        purchaseCache.clear();
    }

    private boolean loadPurchase(UUID uuid, String effectId) {
        return database.queryOne(
                        "SELECT effect_id FROM kt_purchases WHERE uuid = ? AND effect_id = ?",
                        statement -> {
                            statement.setString(1, uuid.toString());
                            statement.setString(2, effectId);
                        },
                        resultSet -> resultSet.getString("effect_id"))
                .isPresent();
    }

    private String purchaseKey(UUID uuid, String effectId) {
        return uuid + ":" + effectId.toLowerCase(Locale.ROOT);
    }
}
