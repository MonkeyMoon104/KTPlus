package com.monkey.ktplus.command.lamp;

import com.monkey.ktplus.command.KtCommandActions;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.BukkitLampConfig;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class LampCommandBootstrap {
    private LampCommandBootstrap() {}

    public static boolean register(JavaPlugin plugin, KtCommandActions actions) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(actions, "actions");
        try {
            EffectIdSuggestions.bind(actions::effectIds);
            BukkitLampConfig<BukkitCommandActor> config = BukkitLampConfig.createDefault(plugin);
            BukkitLamp.builder(config).build().register(new KtLampCommands(actions));
            plugin.getLogger().info("[Commands] Lamp command tree registered");
            return true;
        } catch (Throwable error) {
            EffectIdSuggestions.clear();
            plugin.getLogger().warning("[Commands] Lamp unavailable, using Bukkit executor: " + error.getMessage());
            return false;
        }
    }
}
