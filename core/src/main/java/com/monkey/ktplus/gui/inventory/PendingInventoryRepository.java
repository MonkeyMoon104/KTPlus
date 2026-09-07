package com.monkey.ktplus.gui.inventory;

import com.monkey.ktplus.storage.DatabaseService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PendingInventoryRepository {
    private final DatabaseService database;

    public PendingInventoryRepository(DatabaseService database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public void save(UUID playerId, PlayerInventorySnapshot snapshot) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(snapshot, "snapshot");
        String payload = PlayerInventorySerializer.serialize(snapshot);
        database.transaction(connection -> {
            try (var delete = connection.prepareStatement("DELETE FROM kt_pending_inventory WHERE uuid = ?")) {
                delete.setString(1, playerId.toString());
                delete.executeUpdate();
            }
            try (var insert = connection.prepareStatement(
                    "INSERT INTO kt_pending_inventory (uuid, payload, created_at) VALUES (?, ?, ?)")) {
                insert.setString(1, playerId.toString());
                insert.setString(2, payload);
                insert.setLong(3, System.currentTimeMillis());
                insert.executeUpdate();
            }
        });
    }

    public Optional<PlayerInventorySnapshot> load(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return database.queryOne(
                "SELECT payload FROM kt_pending_inventory WHERE uuid = ?",
                statement -> statement.setString(1, playerId.toString()),
                resultSet -> PlayerInventorySerializer.deserialize(resultSet.getString("payload")));
    }

    public void delete(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        database.update(
                "DELETE FROM kt_pending_inventory WHERE uuid = ?",
                statement -> statement.setString(1, playerId.toString()));
    }
}
