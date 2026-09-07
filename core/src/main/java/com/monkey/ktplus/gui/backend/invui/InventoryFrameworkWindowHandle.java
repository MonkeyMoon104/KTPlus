package com.monkey.ktplus.gui.backend.invui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import com.monkey.ktplus.gui.window.GuiChestSlotWriter;
import com.monkey.ktplus.gui.window.GuiWindowHandle;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

final class InventoryFrameworkWindowHandle implements GuiWindowHandle {
    private final ChestGui gui;
    private final StaticPane borderPane;
    private final StaticPane contentPane;
    private final Plugin plugin;
    private final int rows;

    InventoryFrameworkWindowHandle(
            ChestGui gui, StaticPane borderPane, StaticPane contentPane, Plugin plugin, int rows) {
        this.gui = Objects.requireNonNull(gui, "gui");
        this.borderPane = Objects.requireNonNull(borderPane, "borderPane");
        this.contentPane = Objects.requireNonNull(contentPane, "contentPane");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.rows = rows;
    }

    @Override
    public void refreshTop(Player player, Consumer<GuiChestSlotWriter> populate) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(populate, "populate");
        borderPane.clear();
        contentPane.clear();
        populate.accept(new Writer());
        gui.update();
    }

    @Override
    public void updateTitle(Player player, String title) {
        gui.setTitle(title);
        gui.update();
    }

    private final class Writer implements GuiChestSlotWriter {
        @Override
        public void set(int slot, ItemStack item) {
            placeItem(contentPane, slot, item);
        }

        @Override
        public void setBorder(int slot, ItemStack item) {
            placeItem(borderPane, slot, item);
        }

        private void placeItem(StaticPane pane, int slot, ItemStack item) {
            if (slot < 0 || slot >= rows * 9) {
                return;
            }
            pane.addItem(new GuiItem(item.clone(), plugin), Slot.fromIndex(slot));
        }
    }
}
