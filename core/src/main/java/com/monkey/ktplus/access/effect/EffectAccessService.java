package com.monkey.ktplus.access.effect;

import com.monkey.ktplus.economy.PurchaseResult;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.permission.PermissionService;
import java.util.Objects;
import org.bukkit.entity.Player;

public final class EffectAccessService {
    private final PermissionService permission;
    private final EffectEconomyGate economy;

    public EffectAccessService(PermissionService permission, EffectEconomyGate economy) {
        this.permission = Objects.requireNonNull(permission, "permission");
        this.economy = Objects.requireNonNull(economy, "economy");
    }

    public boolean canActivate(Player player, EffectDefinition definition) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(definition, "definition");
        if (permission.isAdminBypass(player)) {
            return true;
        }
        if (!permission.canUseEffect(player, definition)) {
            return false;
        }
        return economy.ownsOrFree(player, definition);
    }

    public PurchaseResult unlockForSelection(Player player, EffectDefinition definition) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(definition, "definition");
        if (permission.isAdminBypass(player)) {
            return PurchaseResult.ok();
        }
        if (!permission.canUseEffect(player, definition)) {
            return PurchaseResult.denied();
        }
        if (economy.ownsOrFree(player, definition)) {
            return PurchaseResult.ok();
        }
        return economy.purchase(player, definition);
    }
}
