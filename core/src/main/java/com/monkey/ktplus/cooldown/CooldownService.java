package com.monkey.ktplus.cooldown;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownService {
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public boolean ready(UUID uuid, String key) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(key, "key");
        String cacheKey = cacheKey(uuid, key);
        Long until = cooldowns.get(cacheKey);
        if (until == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now >= until) {
            cooldowns.remove(cacheKey, until);
            return true;
        }
        return false;
    }

    public void set(UUID uuid, String key, Duration duration) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(duration, "duration");
        cooldowns.put(cacheKey(uuid, key), System.currentTimeMillis() + duration.toMillis());
    }

    public void clear(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        String prefix = uuid + ":";
        cooldowns.keySet().removeIf(entry -> entry.startsWith(prefix));
    }

    public void clearKey(UUID uuid, String key) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(key, "key");
        cooldowns.remove(cacheKey(uuid, key));
    }

    public void pruneExpired() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private String cacheKey(UUID uuid, String key) {
        return uuid + ":" + key;
    }
}
