package com.monkey.ktplus.effects.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

public final class EffectEntityRegistry {
    private static final long DEFAULT_FIRE_CREDIT_MS = 10_000L;

    private final Map<UUID, CustomProjectileHitPolicy> projectiles = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> fireworks = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> endermen = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> ownedDamagers = new ConcurrentHashMap<>();
    private final Map<UUID, FireCredit> fireCredits = new ConcurrentHashMap<>();

    public void registerProjectile(UUID projectileId, CustomProjectileHitPolicy policy) {
        Objects.requireNonNull(projectileId, "projectileId");
        Objects.requireNonNull(policy, "policy");
        projectiles.put(projectileId, policy);
    }

    public @Nullable CustomProjectileHitPolicy projectile(UUID projectileId) {
        Objects.requireNonNull(projectileId, "projectileId");
        return projectiles.get(projectileId);
    }

    public void registerFirework(UUID fireworkId, UUID ownerId) {
        Objects.requireNonNull(fireworkId, "fireworkId");
        Objects.requireNonNull(ownerId, "ownerId");
        fireworks.put(fireworkId, ownerId);
    }

    public @Nullable UUID fireworkOwner(UUID fireworkId) {
        Objects.requireNonNull(fireworkId, "fireworkId");
        return fireworks.get(fireworkId);
    }

    public void registerEnderman(UUID endermanId, UUID ownerId) {
        Objects.requireNonNull(endermanId, "endermanId");
        Objects.requireNonNull(ownerId, "ownerId");
        endermen.put(endermanId, ownerId);
    }

    public @Nullable UUID endermanOwner(UUID endermanId) {
        Objects.requireNonNull(endermanId, "endermanId");
        return endermen.get(endermanId);
    }

    public void registerOwnedDamager(UUID entityId, UUID ownerId) {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(ownerId, "ownerId");
        ownedDamagers.put(entityId, ownerId);
    }

    public @Nullable UUID ownedDamagerOwner(UUID entityId) {
        Objects.requireNonNull(entityId, "entityId");
        return ownedDamagers.get(entityId);
    }

    public void registerFireCredit(UUID victimId, UUID ownerId) {
        registerFireCredit(victimId, ownerId, DEFAULT_FIRE_CREDIT_MS);
    }

    public void registerFireCredit(UUID victimId, UUID ownerId, long durationMs) {
        Objects.requireNonNull(victimId, "victimId");
        Objects.requireNonNull(ownerId, "ownerId");
        long expiresAt = System.currentTimeMillis() + Math.max(1L, durationMs);
        fireCredits.put(victimId, new FireCredit(ownerId, expiresAt));
    }

    public @Nullable UUID fireCreditOwner(UUID victimId) {
        Objects.requireNonNull(victimId, "victimId");
        FireCredit credit = fireCredits.get(victimId);
        if (credit == null) {
            return null;
        }
        if (System.currentTimeMillis() > credit.expiresAtMillis()) {
            fireCredits.remove(victimId, credit);
            return null;
        }
        return credit.ownerId();
    }

    public void clearFireCredit(UUID victimId) {
        Objects.requireNonNull(victimId, "victimId");
        fireCredits.remove(victimId);
    }

    public void forget(UUID entityId) {
        Objects.requireNonNull(entityId, "entityId");
        projectiles.remove(entityId);
        fireworks.remove(entityId);
        endermen.remove(entityId);
        ownedDamagers.remove(entityId);
    }

    public void clear() {
        projectiles.clear();
        fireworks.clear();
        endermen.clear();
        ownedDamagers.clear();
        fireCredits.clear();
    }

    private record FireCredit(UUID ownerId, long expiresAtMillis) {}
}
