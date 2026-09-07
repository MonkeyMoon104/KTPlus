package com.monkey.ktplus.effects.condition;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.EffectDefinition;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class EffectConditionService {
    private ConfigSnapshot config;

    public EffectConditionService(ConfigSnapshot config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public void reload(ConfigSnapshot config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public boolean allowsWorld(Player player) {
        Objects.requireNonNull(player, "player");
        return !config.isWorldDisabled(player.getWorld().getName());
    }

    public boolean allowsGameMode(Player player) {
        Objects.requireNonNull(player, "player");
        GameMode mode = player.getGameMode();
        return mode != GameMode.SPECTATOR;
    }

    public boolean allowsTrigger(Player player, EffectDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return allowsWorld(player) && allowsGameMode(player);
    }
}
