package com.monkey.ktplus.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GuiLayoutFusedTest {
    @Test
    void mapsRawSlots() {
        GuiLayout layout = GuiLayout.of(6, Arrays.asList(64, 65, 66, 67, 68, 69, 70));
        assertTrue(layout.isTopSlot(10));
        assertFalse(layout.isBottomSlot(10));
        assertTrue(layout.isBottomSlot(64));
        assertEquals(19, layout.playerInventoryIndex(64));
        assertEquals(0, layout.playerInventoryIndex(81));
        assertTrue(layout.isCategorySlot(65));
    }
}
