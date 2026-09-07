package com.monkey.ktplus.event.random;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.economy.EconomyService;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import java.util.Random;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class RandomEventService {
    private final EconomyService economy;
    private final VisualEffectService visuals;
    private final Random random = new Random();
    private ConfigSnapshot config;

    public RandomEventService(ConfigSnapshot config, EconomyService economy, VisualEffectService visuals) {
        this.config = Objects.requireNonNull(config, "config");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
    }

    public void reload(ConfigSnapshot config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public void tryTrigger(Player killer, Location location) {
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(location, "location");
        if (!config.events().getBoolean("enabled", true)) {
            return;
        }
        if (economy.enabled()
                && config.events().getBoolean("money-fountain.enabled", true)
                && random.nextDouble() < config.events().getDouble("money-fountain.chance", 0.02)) {
            moneyFountain(killer, location);
            return;
        }
        if (config.events().getBoolean("meteor-shower.enabled", true)
                && random.nextDouble() < config.events().getDouble("meteor-shower.chance", 0.02)) {
            meteor(location);
        }
    }

    private void moneyFountain(Player killer, Location location) {
        long amount = Math.max(1L, config.events().getLong("money-fountain.amount", 25L));
        economy.addBalance(killer.getUniqueId(), amount);
        visuals.particle("HAPPY_VILLAGER", location.clone().add(0, 1.0, 0), 40, 0.8, 0.8, 0.8, 0.05, Color.LIME);
        visuals.sound("ENTITY_PLAYER_LEVELUP", location, 1.0f, 1.4f);
    }

    private void meteor(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        location.getWorld().strikeLightningEffect(location);
        visuals.particle("EXPLOSION", location.clone().add(0, 0.5, 0), 8, 0.8, 0.4, 0.8, 0.0, Color.ORANGE);
        visuals.sound("ENTITY_GENERIC_EXPLODE", location, 1.0f, 0.8f);
    }
}
