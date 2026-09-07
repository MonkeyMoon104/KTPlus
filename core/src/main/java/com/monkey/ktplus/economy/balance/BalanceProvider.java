package com.monkey.ktplus.economy.balance;

import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface BalanceProvider {
    String id();

    boolean available();

    long balance(Player player);

    long balance(UUID uuid);

    void setBalance(UUID uuid, long balance);

    void addBalance(UUID uuid, long amount);

    boolean withdraw(Player player, long amount);
}
