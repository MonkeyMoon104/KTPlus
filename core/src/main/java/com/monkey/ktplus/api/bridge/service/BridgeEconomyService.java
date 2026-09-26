package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.bridge.ApiEvents;
import com.monkey.ktplus.api.bridge.EffectMappings;
import com.monkey.ktplus.api.event.EffectPurchaseEvent;
import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.model.PurchaseResult;
import com.monkey.ktplus.api.service.EconomyService;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class BridgeEconomyService implements EconomyService {
    private final com.monkey.ktplus.economy.EconomyService economy;
    private final EffectRegistry registry;

    public BridgeEconomyService(com.monkey.ktplus.economy.EconomyService economy, EffectRegistry registry) {
        this.economy = Objects.requireNonNull(economy, "economy");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public boolean enabled() {
        return economy.enabled();
    }

    @Override
    public String providerId() {
        return economy.providerId();
    }

    @Override
    public long balance(Player player) {
        return economy.balance(player);
    }

    @Override
    public long balance(UUID playerId) {
        return economy.balance(playerId);
    }

    @Override
    public void setBalance(UUID playerId, long balance) {
        economy.setBalance(playerId, balance);
    }

    @Override
    public void addBalance(UUID playerId, long amount) {
        economy.addBalance(playerId, amount);
    }

    @Override
    public boolean owns(Player player, Effect effect) {
        EffectDefinition definition = EffectMappings.requireDefinition(registry, effect);
        return definition != null && economy.hasPurchase(player, definition);
    }

    @Override
    public boolean ownsOrFree(Player player, Effect effect) {
        EffectDefinition definition = EffectMappings.requireDefinition(registry, effect);
        return definition != null && economy.ownsOrFree(player, definition);
    }

    @Override
    public PurchaseResult purchase(Player player, Effect effect) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(effect, "effect");
        EffectDefinition definition = EffectMappings.requireDefinition(registry, effect);
        if (definition == null) {
            return PurchaseResult.denied();
        }
        EffectPurchaseEvent pre = new EffectPurchaseEvent(player, effect, true);
        ApiEvents.call(pre);
        if (pre.isCancelled()) {
            return PurchaseResult.denied();
        }
        com.monkey.ktplus.economy.PurchaseResult internal = economy.purchase(player, definition);
        PurchaseResult result = toApi(internal);
        EffectPurchaseEvent post = new EffectPurchaseEvent(player, effect, false);
        post.setResult(result);
        ApiEvents.call(post);
        return result;
    }

    static PurchaseResult toApi(com.monkey.ktplus.economy.PurchaseResult result) {
        if (result.successful()) {
            return PurchaseResult.ok();
        }
        if (result.price() > 0L) {
            return PurchaseResult.notEnough(result.balance(), result.price());
        }
        return PurchaseResult.denied();
    }
}
