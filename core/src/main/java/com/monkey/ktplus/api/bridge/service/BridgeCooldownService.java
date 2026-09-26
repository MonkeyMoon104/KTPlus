package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.service.CooldownService;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class BridgeCooldownService implements CooldownService {
    private final com.monkey.ktplus.cooldown.CooldownService cooldowns;

    public BridgeCooldownService(com.monkey.ktplus.cooldown.CooldownService cooldowns) {
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
    }

    @Override
    public boolean ready(Player player, String key) {
        return ready(player.getUniqueId(), key);
    }

    @Override
    public boolean ready(UUID playerId, String key) {
        return cooldowns.ready(playerId, key);
    }

    @Override
    public void set(Player player, String key, Duration duration) {
        set(player.getUniqueId(), key, duration);
    }

    @Override
    public void set(UUID playerId, String key, Duration duration) {
        cooldowns.set(playerId, key, duration);
    }

    @Override
    public long remainingMillis(Player player, String key) {
        return remainingMillis(player.getUniqueId(), key);
    }

    @Override
    public long remainingMillis(UUID playerId, String key) {
        return cooldowns.remainingMillis(playerId, key);
    }

    @Override
    public void clear(Player player) {
        clear(player.getUniqueId());
    }

    @Override
    public void clear(UUID playerId) {
        cooldowns.clear(playerId);
    }

    @Override
    public void clearKey(Player player, String key) {
        clearKey(player.getUniqueId(), key);
    }

    @Override
    public void clearKey(UUID playerId, String key) {
        cooldowns.clearKey(playerId, key);
    }
}
