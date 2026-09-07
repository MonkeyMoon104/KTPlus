package com.monkey.ktplus.listener.resourcepack;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.resourcepack.ResourcePackService;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class ResourcePackJoinListener implements Listener {
    private final ResourcePackService resourcePackService;

    public ResourcePackJoinListener(ResourcePackService resourcePackService) {
        this.resourcePackService = Objects.requireNonNull(resourcePackService, "resourcePackService");
    }

    public void reload(ConfigSnapshot config) {
        resourcePackService.reload(config);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Objects.requireNonNull(event, "event");
        resourcePackService.send(event.getPlayer());
    }
}
