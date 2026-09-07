package com.monkey.ktplus.access.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EffectSelectionOutcomeTest {
    @Test
    void outcomesAreDistinct() {
        assertEquals(4, EffectSelectionService.Outcome.values().length);
        assertTrue(EffectSelectionService.Outcome.SELECTED != EffectSelectionService.Outcome.PURCHASED_AND_SELECTED);
    }
}
