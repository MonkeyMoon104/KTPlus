package com.monkey.ktplus.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;

class EffectPermissionDefaultsTest {
    @Test
    void freeEffectsDefaultTrue() {
        assertEquals(PermissionDefault.TRUE, EffectPermissionDefaults.forPrice(0));
    }

    @Test
    void paidEffectsDefaultFalse() {
        assertEquals(PermissionDefault.FALSE, EffectPermissionDefaults.forPrice(100));
    }
}
