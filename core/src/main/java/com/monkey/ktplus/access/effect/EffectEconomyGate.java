package com.monkey.ktplus.access.effect;

import com.monkey.ktplus.economy.PurchaseResult;
import com.monkey.ktplus.effects.api.EffectDefinition;
import org.bukkit.entity.Player;

public interface EffectEconomyGate {
    boolean ownsOrFree(Player player, EffectDefinition definition);

    PurchaseResult purchase(Player player, EffectDefinition definition);
}
