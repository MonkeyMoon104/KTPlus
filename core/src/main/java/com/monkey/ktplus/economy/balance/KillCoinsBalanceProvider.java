package com.monkey.ktplus.economy.balance;

import com.monkey.ktplus.storage.repository.KillCoinsRepository;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class KillCoinsBalanceProvider implements BalanceProvider {
    private final KillCoinsRepository repository;

    public KillCoinsBalanceProvider(KillCoinsRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public String id() {
        return "KILLCOINS";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public long balance(Player player) {
        return repository.balance(player.getUniqueId());
    }

    @Override
    public long balance(UUID uuid) {
        return repository.balance(uuid);
    }

    @Override
    public void setBalance(UUID uuid, long balance) {
        repository.setBalance(uuid, balance);
    }

    @Override
    public void addBalance(UUID uuid, long amount) {
        repository.addBalance(uuid, amount);
    }

    @Override
    public boolean withdraw(Player player, long amount) {
        Objects.requireNonNull(player, "player");
        if (amount <= 0L) {
            return true;
        }
        long current = balance(player);
        if (current < amount) {
            return false;
        }
        repository.setBalance(player.getUniqueId(), current - amount);
        return true;
    }
}
