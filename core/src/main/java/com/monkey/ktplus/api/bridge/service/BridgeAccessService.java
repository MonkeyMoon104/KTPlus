package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.bridge.EffectMappings;
import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.model.PurchaseResult;
import com.monkey.ktplus.api.model.SelectionResult;
import com.monkey.ktplus.api.service.AccessService;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import java.util.Objects;
import org.bukkit.entity.Player;

public final class BridgeAccessService implements AccessService {
    private final com.monkey.ktplus.access.effect.EffectAccessService access;
    private final com.monkey.ktplus.access.effect.EffectSelectionService selection;
    private final EffectRegistry registry;

    public BridgeAccessService(
            com.monkey.ktplus.access.effect.EffectAccessService access,
            com.monkey.ktplus.access.effect.EffectSelectionService selection,
            EffectRegistry registry) {
        this.access = Objects.requireNonNull(access, "access");
        this.selection = Objects.requireNonNull(selection, "selection");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public boolean canActivate(Player player, Effect effect) {
        EffectDefinition definition = EffectMappings.requireDefinition(registry, effect);
        return definition != null && access.canActivate(player, definition);
    }

    @Override
    public PurchaseResult unlockForSelection(Player player, Effect effect) {
        EffectDefinition definition = EffectMappings.requireDefinition(registry, effect);
        if (definition == null) {
            return PurchaseResult.denied();
        }
        return BridgeEconomyService.toApi(access.unlockForSelection(player, definition));
    }

    @Override
    public SelectionResult select(Player player, Effect effect) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(effect, "effect");
        EffectDefinition definition = EffectMappings.requireDefinition(registry, effect);
        if (definition == null) {
            return SelectionResult.DENIED_PERMISSION;
        }
        return toApi(selection.select(player, definition));
    }

    private static SelectionResult toApi(
            com.monkey.ktplus.access.effect.EffectSelectionService.Outcome outcome) {
        return switch (outcome) {
            case SELECTED -> SelectionResult.SELECTED;
            case PURCHASED_AND_SELECTED -> SelectionResult.PURCHASED_AND_SELECTED;
            case DENIED_PERMISSION -> SelectionResult.DENIED_PERMISSION;
            case DENIED_FUNDS -> SelectionResult.DENIED_FUNDS;
        };
    }
}
