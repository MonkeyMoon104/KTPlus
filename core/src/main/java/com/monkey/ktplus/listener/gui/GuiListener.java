package com.monkey.ktplus.listener.gui;

import com.monkey.ktplus.gui.EffectGuiService;
import com.monkey.ktplus.gui.action.GuiClickFeedback;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GuiListener implements Listener {
    private final EffectGuiService gui;

    public GuiListener(EffectGuiService gui) {
        this.gui = Objects.requireNonNull(gui, "gui");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!gui.sessions().get(player).isPresent()) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < 90) {
            event.setCancelled(true);
        }
        if (event.getClick().isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof InventoryCreativeEvent) {
            return;
        }
        if (rawSlot < 0 || rawSlot >= 90) {
            return;
        }
        gui.sessions()
                .get(player)
                .flatMap(session -> session.action(rawSlot))
                .ifPresent(action -> {
                    GuiClickFeedback.play(player);
                    gui.handle(player, action, event.getClick());
                });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!gui.sessions().get((Player) event.getWhoClicked()).isPresent()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!gui.sessions().get((Player) event.getWhoClicked()).isPresent()) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < 90) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        gui.sessions().get(player).ifPresent(session -> gui.finishSession(player, session.id()));
    }
}
