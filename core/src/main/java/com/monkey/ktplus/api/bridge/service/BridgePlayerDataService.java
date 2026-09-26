package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.service.PlayerDataService;
import com.monkey.ktplus.user.UserService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class BridgePlayerDataService implements PlayerDataService {
    private final UserService users;

    public BridgePlayerDataService(UserService users) {
        this.users = Objects.requireNonNull(users, "users");
    }

    @Override
    public Optional<String> selectedEffect(Player player) {
        return users.selectedEffect(player);
    }

    @Override
    public Optional<String> selectedEffect(UUID playerId) {
        return users.selectedEffect(playerId);
    }

    @Override
    public void setSelectedEffect(Player player, String effectId) {
        users.selectEffect(player, effectId);
    }

    @Override
    public void setSelectedEffect(UUID playerId, String effectId) {
        users.selectEffect(playerId, effectId);
    }

    @Override
    public void clearSelectedEffect(Player player) {
        users.clearEffect(player);
    }

    @Override
    public void clearSelectedEffect(UUID playerId) {
        users.clearEffect(playerId);
    }
}
