package com.monkey.ktplus.effects.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class EffectEntityRegistryTest {
    @Test
    void registersAndForgetsProjectileAndFirework() {
        EffectEntityRegistry registry = new EffectEntityRegistry();
        UUID projectileId = UUID.randomUUID();
        UUID fireworkId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        CustomProjectileHitPolicy policy = new CustomProjectileHitPolicy(ownerId, 4.0D, true);

        registry.registerProjectile(projectileId, policy);
        registry.registerFirework(fireworkId, ownerId);

        assertEquals(policy, registry.projectile(projectileId));
        assertEquals(ownerId, registry.fireworkOwner(fireworkId));

        registry.forget(projectileId);
        registry.forget(fireworkId);

        assertNull(registry.projectile(projectileId));
        assertNull(registry.fireworkOwner(fireworkId));
    }

    @Test
    void clearRemovesAll() {
        EffectEntityRegistry registry = new EffectEntityRegistry();
        UUID id = UUID.randomUUID();
        registry.registerFirework(id, UUID.randomUUID());
        registry.clear();
        assertNull(registry.fireworkOwner(id));
    }
}
