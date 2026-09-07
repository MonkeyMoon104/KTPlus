package com.monkey.ktplus.gui.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class PlayerInventorySerializerTest {
    @Test
    void roundTripsEmptySnapshot() {
        PlayerInventorySnapshot original =
                PlayerInventorySnapshot.fromStored(new ItemStack[36], new ItemStack[4], null);
        String payload = PlayerInventorySerializer.serialize(original);
        PlayerInventorySnapshot restored = PlayerInventorySerializer.deserialize(payload);
        assertEquals(original, restored);
    }
}
