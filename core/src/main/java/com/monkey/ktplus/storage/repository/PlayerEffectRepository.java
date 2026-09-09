package com.monkey.ktplus.storage.repository;

import com.monkey.ktplus.cache.BoundedCache;
import com.monkey.ktplus.storage.DatabaseService;
import java.sql.PreparedStatement;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerEffectRepository {
    private final DatabaseService database;
    private final BoundedCache<UUID, Optional<String>> cache = BoundedCache.create(2048, 300);

    public PlayerEffectRepository(DatabaseService database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public Optional<String> selectedEffect(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadSelected);
    }

    public void setSelectedEffect(UUID uuid, String effectId) {
        database.transaction(connection -> {
            try (PreparedStatement delete =
                    connection.prepareStatement("DELETE FROM kt_player_effects WHERE uuid = ?")) {
                delete.setString(1, uuid.toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO kt_player_effects (uuid, effect_id) VALUES (?, ?)")) {
                insert.setString(1, uuid.toString());
                insert.setString(2, effectId);
                insert.executeUpdate();
            }
        });
        cache.put(uuid, Optional.of(effectId));
    }

    public void clearSelectedEffect(UUID uuid) {
        database.update(
                "DELETE FROM kt_player_effects WHERE uuid = ?",
                statement -> statement.setString(1, uuid.toString()));
        cache.put(uuid, Optional.empty());
    }

    public void clearCache() {
        cache.clear();
    }

    private Optional<String> loadSelected(UUID uuid) {
        return database.queryOne(
                "SELECT effect_id FROM kt_player_effects WHERE uuid = ?",
                statement -> statement.setString(1, uuid.toString()),
                resultSet -> resultSet.getString("effect_id"));
    }
}
