package com.monkey.ktplus.gui.session;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class GuiSessionRegistry {
    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();

    public Set<UUID> activePlayerIds() {
        return sessions.keySet().stream().collect(Collectors.toSet());
    }

    public void put(GuiSession session) {
        Objects.requireNonNull(session, "session");
        sessions.put(session.playerId(), session);
    }

    public Optional<GuiSession> get(Player player) {
        Objects.requireNonNull(player, "player");
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    public void remove(Player player) {
        Objects.requireNonNull(player, "player");
        sessions.remove(player.getUniqueId());
    }

    public boolean removeIfCurrent(Player player, UUID sessionId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sessionId, "sessionId");
        GuiSession current = sessions.get(player.getUniqueId());
        if (current != null && current.id().equals(sessionId)) {
            sessions.remove(player.getUniqueId());
            return true;
        }
        return false;
    }

    public void clear() {
        sessions.clear();
    }

    public void closeAll() {
        for (UUID playerId : sessions.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        sessions.clear();
    }
}
