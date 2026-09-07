package com.monkey.ktplus.gui.backend.vanilla;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class VanillaInventoryHolder implements InventoryHolder {
    private final UUID sessionId;

    public VanillaInventoryHolder(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID sessionId() {
        return sessionId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
