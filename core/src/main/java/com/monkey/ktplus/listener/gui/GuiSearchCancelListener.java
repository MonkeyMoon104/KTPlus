package com.monkey.ktplus.listener.gui;

import com.monkey.ktplus.gui.EffectGuiService;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public final class GuiSearchCancelListener implements Listener {
    private final EffectGuiService gui;

    public GuiSearchCancelListener(EffectGuiService gui) {
        this.gui = Objects.requireNonNull(gui, "gui");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            tryCancel(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (tryCancel(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (tryCancel(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private boolean tryCancel(Player player) {
        return gui.isPendingSearch(player) && gui.cancelPendingSearch(player);
    }
}
