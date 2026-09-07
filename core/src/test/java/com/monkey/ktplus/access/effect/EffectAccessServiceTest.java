package com.monkey.ktplus.access.effect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.economy.PurchaseResult;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.permission.PermissionService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class EffectAccessServiceTest {
    private final EffectDefinition definition =
            new EffectDefinition("cloud", "Cloud", Material.STONE, 100, false, 10L);

    @Test
    @EnabledIf("stonePresent")
    void canActivateRequiresEffectPermission() {
        EffectAccessService access = service(true, PurchaseResult.ok());
        assertFalse(access.canActivate(TestPlayers.withPermissions(), definition));
        assertTrue(access.canActivate(TestPlayers.withPermissions("ktplus.cloud.use"), definition));
    }

    @Test
    @EnabledIf("stonePresent")
    void adminBypassAllowsActivateWithoutEffectPermission() {
        EffectAccessService access = service(false, PurchaseResult.ok());
        assertTrue(access.canActivate(TestPlayers.withPermissions("ktplus.admin.bypass"), definition));
    }

    @Test
    @EnabledIf("stonePresent")
    void unlockDeniedWithoutEffectPermission() {
        EffectAccessService access = service(true, PurchaseResult.ok());
        assertFalse(access.unlockForSelection(TestPlayers.withPermissions(), definition).successful());
    }

    @Test
    @EnabledIf("stonePresent")
    void unlockOkForAdminBypassWithoutEffectPermission() {
        EffectAccessService access = service(false, PurchaseResult.denied());
        assertTrue(access.unlockForSelection(TestPlayers.withPermissions("ktplus.admin.bypass"), definition).successful());
    }

    @Test
    @EnabledIf("stonePresent")
    void canActivateDeniedWhenPermittedButNotOwned() {
        EffectAccessService access = service(false, PurchaseResult.ok());
        assertFalse(access.canActivate(TestPlayers.withPermissions("ktplus.cloud.use"), definition));
    }

    @Test
    @EnabledIf("stonePresent")
    void unlockPurchasesWhenPermittedAndNotOwned() {
        EffectAccessService access = service(false, PurchaseResult.ok());
        assertTrue(access.unlockForSelection(TestPlayers.withPermissions("ktplus.cloud.use"), definition).successful());
    }

    static boolean stonePresent() {
        return Material.matchMaterial("STONE") != null;
    }

    private static EffectAccessService service(boolean owned, PurchaseResult purchaseResult) {
        EffectEconomyGate economy = new EffectEconomyGate() {
            @Override
            public boolean ownsOrFree(Player player, EffectDefinition effectDefinition) {
                return owned;
            }

            @Override
            public PurchaseResult purchase(Player player, EffectDefinition effectDefinition) {
                return purchaseResult;
            }
        };
        return new EffectAccessService(new PermissionService(), economy);
    }
}
