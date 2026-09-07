package com.monkey.ktplus.gui.inventory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlayerInventoryGuard {
    private final PendingInventoryRepository repository;
    private final Map<UUID, PlayerInventorySnapshot> active = new ConcurrentHashMap<>();

    public PlayerInventoryGuard(PendingInventoryRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public boolean begin(Player player) {
        Objects.requireNonNull(player, "player");
        UUID playerId = player.getUniqueId();
        if (active.containsKey(playerId)) {
            return false;
        }
        PlayerInventorySnapshot snapshot = PlayerInventorySnapshot.capture(player);
        repository.save(playerId, snapshot);
        active.put(playerId, snapshot);
        return true;
    }

    public boolean isActive(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public Optional<PlayerInventorySnapshot> activeSnapshot(Player player) {
        return Optional.ofNullable(active.get(player.getUniqueId()));
    }

    public void restore(Player player) {
        Objects.requireNonNull(player, "player");
        UUID playerId = player.getUniqueId();
        PlayerInventorySnapshot snapshot = active.remove(playerId);
        if (snapshot != null) {
            snapshot.apply(player);
            repository.delete(playerId);
            return;
        }
        repository.load(playerId).ifPresent(stored -> {
            stored.apply(player);
            repository.delete(playerId);
        });
    }

    public void restorePending(Player player) {
        Objects.requireNonNull(player, "player");
        UUID playerId = player.getUniqueId();
        repository.load(playerId).ifPresent(stored -> {
            stored.apply(player);
            repository.delete(playerId);
        });
    }

    public void restoreAllOnline() {
        for (UUID playerId : active.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                restore(player);
            }
        }
        active.clear();
    }
}
