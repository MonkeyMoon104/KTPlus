package com.monkey.ktplus.access.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.monkey.ktplus.economy.PurchaseResult;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.permission.PermissionService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class EffectSelectionServiceTest {
    private final EffectDefinition paidEffect =
            new EffectDefinition("cloud", "Cloud", Material.STONE, 100, false, 10L);
    private final EffectDefinition freeEffect =
            new EffectDefinition("lightning", "Lightning", Material.STONE, 0, true, 10L);

    @Test
    @EnabledIf("stonePresent")
    void selectWhenOwnedAndPermitted() {
        RecordingSelections selections = new RecordingSelections();
        EffectSelectionService service = service(true, PurchaseResult.ok(), true, selections);
        Player player = TestPlayers.withPermissions("ktplus.cloud.use");

        assertEquals(EffectSelectionService.Outcome.SELECTED, service.select(player, paidEffect));
        assertEquals("cloud", selections.lastEffectId);
    }

    @Test
    @EnabledIf("stonePresent")
    void selectDeniedWithoutPermission() {
        RecordingSelections selections = new RecordingSelections();
        EffectSelectionService service = service(false, PurchaseResult.denied(), true, selections);

        assertEquals(
                EffectSelectionService.Outcome.DENIED_PERMISSION,
                service.select(TestPlayers.withPermissions(), paidEffect));
        assertNull(selections.lastEffectId);
    }

    @Test
    @EnabledIf("stonePresent")
    void selectDeniedWhenFundsInsufficient() {
        RecordingSelections selections = new RecordingSelections();
        EffectSelectionService service =
                service(false, PurchaseResult.notEnough(10L, 100), true, selections);
        Player player = TestPlayers.withPermissions("ktplus.cloud.use");

        assertEquals(EffectSelectionService.Outcome.DENIED_FUNDS, service.select(player, paidEffect));
        assertNull(selections.lastEffectId);
    }

    @Test
    @EnabledIf("stonePresent")
    void selectPurchasesWhenPermittedAndNotOwned() {
        RecordingSelections selections = new RecordingSelections();
        EffectSelectionService service = service(false, PurchaseResult.ok(), true, selections);
        Player player = TestPlayers.withPermissions("ktplus.cloud.use");

        assertEquals(
                EffectSelectionService.Outcome.PURCHASED_AND_SELECTED, service.select(player, paidEffect));
        assertEquals("cloud", selections.lastEffectId);
    }

    @Test
    @EnabledIf("stonePresent")
    void selectUnlocksFreeEffectWithoutPurchaseOutcome() {
        RecordingSelections selections = new RecordingSelections();
        EffectSelectionService service = service(false, PurchaseResult.ok(), true, selections);
        Player player = TestPlayers.withPermissions("ktplus.lightning.use");

        assertEquals(EffectSelectionService.Outcome.SELECTED, service.select(player, freeEffect));
        assertEquals("lightning", selections.lastEffectId);
    }

    static boolean stonePresent() {
        return Material.matchMaterial("STONE") != null;
    }

    private static EffectSelectionService service(
            boolean owned,
            PurchaseResult purchaseResult,
            boolean economyEnabled,
            RecordingSelections selections) {
        EffectEconomyGate economyGate = new EffectEconomyGate() {
            @Override
            public boolean ownsOrFree(Player player, EffectDefinition effectDefinition) {
                return owned;
            }

            @Override
            public PurchaseResult purchase(Player player, EffectDefinition effectDefinition) {
                return purchaseResult;
            }
        };
        EffectAccessService access = new EffectAccessService(new PermissionService(), economyGate);
        EconomyEnabledGate economy = () -> economyEnabled;
        UserSelectionWriter users = (player, effectId) -> selections.lastEffectId = effectId;
        return new EffectSelectionService(access, economy, users);
    }

    private static final class RecordingSelections {
        private String lastEffectId;
    }
}
