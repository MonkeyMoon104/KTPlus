package com.monkey.ktplus.effects.custom;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.cooldown.CooldownService;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomEffectLoader {
    public CustomEffectLoader(
            JavaPlugin plugin,
            ConfigSnapshot config,
            VisualEffectService visuals,
            CooldownService cooldowns) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(cooldowns, "cooldowns");
    }

    public void registerAll(EffectRegistry registry) {
        Objects.requireNonNull(registry, "registry");
    }
}
