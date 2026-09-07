package com.monkey.ktplus.resourcepack;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.util.net.ResourcePackSender;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourcePackService {
    private final JavaPlugin plugin;
    private final Logger logger;
    private ConfigSnapshot config;

    public ResourcePackService(JavaPlugin plugin, ConfigSnapshot config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
        this.config = Objects.requireNonNull(config, "config");
    }

    public void reload(ConfigSnapshot config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public void send(Player player) {
        Objects.requireNonNull(player, "player");
        ResourcePackSettings settings = settings();
        if (!settings.isValidForSend()) {
            return;
        }
        ResourcePackSender.send(
                player,
                settings.url(),
                settings.sha1(),
                settings.uuid(),
                settings.prompt(),
                settings.required(),
                logger);
    }

    public void remove(Player player) {
        Objects.requireNonNull(player, "player");
        ResourcePackSettings settings = settings();
        if (settings.uuid() == null) {
            return;
        }
        ResourcePackSender.remove(player, settings.uuid(), logger);
    }

    public void applyOnline() {
        ResourcePackSettings settings = settings();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (settings.enabled()) {
                send(player);
            } else {
                remove(player);
            }
        }
    }

    public ResourcePackSettings settings() {
        return ResourcePackSettings.from(config.resourcePack());
    }
}
