package com.monkey.ktplus.effects.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

public final class CustomProjectileTracker {
    private static final Map<UUID, CustomProjectileHitPolicy> POLICIES = new ConcurrentHashMap<UUID, CustomProjectileHitPolicy>();

    private CustomProjectileTracker() {}

    public static void register(UUID projectileId, CustomProjectileHitPolicy policy) {
        Objects.requireNonNull(projectileId, "projectileId");
        Objects.requireNonNull(policy, "policy");
        POLICIES.put(projectileId, policy);
    }

    public static @Nullable CustomProjectileHitPolicy get(UUID projectileId) {
        Objects.requireNonNull(projectileId, "projectileId");
        return POLICIES.get(projectileId);
    }

    public static void forget(UUID projectileId) {
        Objects.requireNonNull(projectileId, "projectileId");
        POLICIES.remove(projectileId);
    }

    public static void clear() {
        POLICIES.clear();
    }
}
