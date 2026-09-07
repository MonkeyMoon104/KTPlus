package com.monkey.ktplus.listener.gui;

import com.monkey.ktplus.gui.EffectGuiService;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerInventoryGuardListener implements Listener {
    private final EffectGuiService gui;

    public PlayerInventoryGuardListener(EffectGuiService gui) {
        this.gui = Objects.requireNonNull(gui, "gui");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        gui.inventoryGuard().restorePending(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (gui.sessions().get(player).isPresent()) {
            gui.finishSession(player, null);
        }
    }
}
