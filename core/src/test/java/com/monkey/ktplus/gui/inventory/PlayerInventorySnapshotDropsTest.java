package com.monkey.ktplus.gui.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class PlayerInventorySnapshotDropsTest {
    @Test
    void addToDropsSkipsEmptySlots() {
        PlayerInventorySnapshot snapshot =
                PlayerInventorySnapshot.fromStored(new ItemStack[36], new ItemStack[4], null);
        List<ItemStack> drops = new ArrayList<>();
        snapshot.addToDrops(drops);
        assertTrue(drops.isEmpty());
    }

    @Test
    void addToDropsDoesNotMutateSnapshotStorage() {
        ItemStack[] storage = new ItemStack[36];
        PlayerInventorySnapshot snapshot = PlayerInventorySnapshot.fromStored(storage, new ItemStack[4], null);
        List<ItemStack> drops = new ArrayList<>();
        snapshot.addToDrops(drops);
        assertEquals(0, drops.size());
        assertEquals(36, snapshot.storage().length);
    }
}
