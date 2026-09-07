package com.monkey.ktplus.access.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.permission.PermissionService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class PermissionServiceTest {
    @Test
    @EnabledIf("stonePresent")
    void canUseEffectRequiresEffectPermission() {
        PermissionService permission = new PermissionService();
        EffectDefinition definition = new EffectDefinition("cloud", "Cloud", Material.STONE, 0, false, 10L);
        Player denied = TestPlayers.withPermissions();
        Player allowed = TestPlayers.withPermissions("ktplus.cloud.use");
        assertFalse(permission.canUseEffect(denied, definition));
        assertTrue(permission.canUseEffect(allowed, definition));
    }

    @Test
    @EnabledIf("stonePresent")
    void adminBypassSkipsEffectPermission() {
        PermissionService permission = new PermissionService();
        EffectDefinition definition = new EffectDefinition("cloud", "Cloud", Material.STONE, 0, false, 10L);
        Player admin = TestPlayers.withPermissions("ktplus.admin.bypass");
        assertTrue(permission.canUseEffect(admin, definition));
    }

    @Test
    void permissionNodeUsesLowercaseEffectId() {
        PermissionService permission = new PermissionService();
        assertEquals("ktplus.lightning.use", permission.permissionNode("Lightning"));
    }

    static boolean stonePresent() {
        return Material.matchMaterial("STONE") != null;
    }
}
