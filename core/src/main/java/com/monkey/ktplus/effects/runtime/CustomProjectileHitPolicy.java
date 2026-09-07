package com.monkey.ktplus.effects.runtime;

import java.util.Objects;
import java.util.UUID;

public final class CustomProjectileHitPolicy {
    private final UUID ownerId;
    private final double damage;
    private final boolean removeOnHit;

    public CustomProjectileHitPolicy(UUID ownerId, double damage, boolean removeOnHit) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.damage = Math.max(0.0D, damage);
        this.removeOnHit = removeOnHit;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public double damage() {
        return damage;
    }

    public boolean removeOnHit() {
        return removeOnHit;
    }
}
