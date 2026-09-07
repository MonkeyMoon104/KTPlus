package com.monkey.ktplus.economy;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.economy.balance.BalanceProvider;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.hook.luckperms.LuckPermsHook;
import com.monkey.ktplus.storage.repository.KillCoinsRepository;
import com.monkey.ktplus.storage.repository.PurchaseRepository;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class EconomyService implements com.monkey.ktplus.access.effect.EffectEconomyGate,
        com.monkey.ktplus.access.effect.EconomyEnabledGate {
    private ConfigSnapshot config;
    private final KillCoinsRepository killCoins;
    private final PurchaseRepository purchases;
    private BalanceProvider balanceProvider;
    private @Nullable LuckPermsHook luckPermsHook;

    public EconomyService(
            ConfigSnapshot config,
            KillCoinsRepository killCoins,
            PurchaseRepository purchases,
            BalanceProvider balanceProvider) {
        this.config = Objects.requireNonNull(config, "config");
        this.killCoins = Objects.requireNonNull(killCoins, "killCoins");
        this.purchases = Objects.requireNonNull(purchases, "purchases");
        this.balanceProvider = Objects.requireNonNull(balanceProvider, "balanceProvider");
    }

    public void reload(ConfigSnapshot config, BalanceProvider balanceProvider) {
        this.config = Objects.requireNonNull(config, "config");
        this.balanceProvider = Objects.requireNonNull(balanceProvider, "balanceProvider");
    }

    public void setLuckPermsHook(@Nullable LuckPermsHook luckPermsHook) {
        this.luckPermsHook = luckPermsHook;
    }

    public boolean enabled() {
        return config.economyEnabled();
    }

    public String providerId() {
        return balanceProvider.id();
    }

    public long balance(Player player) {
        return balanceProvider.balance(player);
    }

    public long balance(UUID uuid) {
        return balanceProvider.balance(uuid);
    }

    public void setBalance(UUID uuid, long balance) {
        balanceProvider.setBalance(uuid, balance);
    }

    public void addBalance(UUID uuid, long amount) {
        balanceProvider.addBalance(uuid, amount);
    }

    public boolean hasPurchase(Player player, EffectDefinition definition) {
        return purchases.hasPurchase(player.getUniqueId(), definition.id());
    }

    public boolean ownsOrFree(Player player, EffectDefinition definition) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(definition, "definition");
        if (!enabled()) {
            return true;
        }
        if (definition.price() <= 0) {
            return true;
        }
        return hasPurchase(player, definition);
    }

    public PurchaseResult purchase(Player player, EffectDefinition definition) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(definition, "definition");
        if (!enabled() || definition.price() <= 0 || hasPurchase(player, definition)) {
            return PurchaseResult.ok();
        }
        if ("KILLCOINS".equalsIgnoreCase(balanceProvider.id())) {
            if (!killCoins.tryPurchase(player.getUniqueId(), definition.id(), definition.price())) {
                return PurchaseResult.notEnough(balance(player), definition.price());
            }
            if (luckPermsHook != null) {
                luckPermsHook.grantEffectPermission(player.getUniqueId(), definition.permissionNode());
            }
            return PurchaseResult.ok();
        }
        if (!purchases.claimPurchaseIfAbsent(player.getUniqueId(), definition.id())) {
            if (purchases.hasPurchase(player.getUniqueId(), definition.id())) {
                return PurchaseResult.ok();
            }
            return PurchaseResult.notEnough(balance(player), definition.price());
        }
        if (!balanceProvider.withdraw(player, definition.price())) {
            purchases.removePurchase(player.getUniqueId(), definition.id());
            return PurchaseResult.notEnough(balance(player), definition.price());
        }
        if (luckPermsHook != null) {
            luckPermsHook.grantEffectPermission(player.getUniqueId(), definition.permissionNode());
        }
        return PurchaseResult.ok();
    }

    public void reward(Player killer, boolean playerKill) {
        int reward = config.killReward(playerKill);
        if (enabled() && reward > 0) {
            addBalance(killer.getUniqueId(), reward);
        }
    }
}
