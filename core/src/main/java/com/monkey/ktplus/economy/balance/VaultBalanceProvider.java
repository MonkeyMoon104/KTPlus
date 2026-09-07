package com.monkey.ktplus.economy.balance;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jspecify.annotations.Nullable;

public final class VaultBalanceProvider implements BalanceProvider {
    private final @Nullable Object economy;
    private final @Nullable Method getBalance;
    private final @Nullable Method depositPlayer;
    private final @Nullable Method withdrawPlayer;
    private final boolean available;

    private VaultBalanceProvider(
            @Nullable Object economy,
            @Nullable Method getBalance,
            @Nullable Method depositPlayer,
            @Nullable Method withdrawPlayer,
            boolean available) {
        this.economy = economy;
        this.getBalance = getBalance;
        this.depositPlayer = depositPlayer;
        this.withdrawPlayer = withdrawPlayer;
        this.available = available;
    }

    public static VaultBalanceProvider tryCreate(Logger logger) {
        Objects.requireNonNull(logger, "logger");
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) {
            return unavailable();
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration =
                    Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration == null || registration.getProvider() == null) {
                logger.warning("[Economy] Vault present but no Economy provider registered.");
                return unavailable();
            }
            Object economy = registration.getProvider();
            Method getBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
            Method depositPlayer = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            Method withdrawPlayer = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            return new VaultBalanceProvider(economy, getBalance, depositPlayer, withdrawPlayer, true);
        } catch (ReflectiveOperationException error) {
            logger.warning("[Economy] Unable to hook Vault Economy: " + error.getMessage());
            return unavailable();
        }
    }

    private static VaultBalanceProvider unavailable() {
        return new VaultBalanceProvider(null, null, null, null, false);
    }

    @Override
    public String id() {
        return "VAULT";
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public long balance(Player player) {
        return balance(player.getUniqueId());
    }

    @Override
    public long balance(UUID uuid) {
        if (!available) {
            return 0L;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        try {
            Object value = getBalance.invoke(economy, offline);
            if (value instanceof Number) {
                return Math.max(0L, (long) Math.floor(((Number) value).doubleValue()));
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0L;
    }

    @Override
    public void setBalance(UUID uuid, long balance) {
        if (!available) {
            return;
        }
        long current = balance(uuid);
        long target = Math.max(0L, balance);
        if (target > current) {
            addBalance(uuid, target - current);
        } else if (target < current) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            try {
                withdrawPlayer.invoke(economy, offline, (double) (current - target));
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    @Override
    public void addBalance(UUID uuid, long amount) {
        if (!available || amount == 0L) {
            return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        try {
            if (amount > 0L) {
                depositPlayer.invoke(economy, offline, (double) amount);
            } else {
                withdrawPlayer.invoke(economy, offline, (double) (-amount));
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Override
    public boolean withdraw(Player player, long amount) {
        Objects.requireNonNull(player, "player");
        if (!available) {
            return false;
        }
        if (amount <= 0L) {
            return true;
        }
        if (balance(player) < amount) {
            return false;
        }
        try {
            Object result = withdrawPlayer.invoke(economy, player, (double) amount);
            Method success = result.getClass().getMethod("transactionSuccess");
            Object ok = success.invoke(result);
            return ok instanceof Boolean && (Boolean) ok;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
