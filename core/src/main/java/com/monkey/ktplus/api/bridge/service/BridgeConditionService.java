package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.bridge.EffectMappings;
import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.service.ConditionService;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.condition.EffectConditionService;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import java.util.Objects;
import org.bukkit.entity.Player;

public final class BridgeConditionService implements ConditionService {
    private final EffectConditionService conditions;
    private final EffectRegistry registry;

    public BridgeConditionService(EffectConditionService conditions, EffectRegistry registry) {
        this.conditions = Objects.requireNonNull(conditions, "conditions");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public boolean allowsWorld(Player player) {
        return conditions.allowsWorld(player);
    }

    @Override
    public boolean allowsGameMode(Player player) {
        return conditions.allowsGameMode(player);
    }

    @Override
    public boolean allowsTrigger(Player player, Effect effect) {
        EffectDefinition definition = EffectMappings.requireDefinition(registry, effect);
        return definition != null && conditions.allowsTrigger(player, definition);
    }
}
