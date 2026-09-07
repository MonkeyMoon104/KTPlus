package com.monkey.ktplus.gui.window;

import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface GuiChestSlotWriter {
    void set(int slot, ItemStack item);

    default void setBorder(int slot, ItemStack item) {
        set(slot, item);
    }
}
