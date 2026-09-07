package com.monkey.ktplus.gui.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EffectGuiSortModeTest {
    @Test
    void cyclesThroughAllModes() {
        EffectGuiSortMode mode = EffectGuiSortMode.NONE;
        int steps = 0;
        do {
            mode = mode.next();
            steps++;
        } while (mode != EffectGuiSortMode.NONE && steps < 32);
        assertEquals(EffectGuiSortMode.values().length, steps);
    }

    @Test
    void previousIsInverseOfNext() {
        for (EffectGuiSortMode mode : EffectGuiSortMode.values()) {
            assertEquals(mode, mode.next().previous());
            assertEquals(mode, mode.previous().next());
        }
    }

    @Test
    void noneIsDefaultMode() {
        assertEquals(EffectGuiSortMode.NONE, EffectGuiSortMode.values()[0]);
        assertEquals("None", EffectGuiSortMode.NONE.shortLabel());
    }

    @Test
    void priceModesExist() {
        assertTrue(EffectGuiSortMode.PRICE_ASC.shortLabel().toLowerCase().contains("price"));
        assertTrue(EffectGuiSortMode.PRICE_DESC.shortLabel().toLowerCase().contains("price"));
    }
}
