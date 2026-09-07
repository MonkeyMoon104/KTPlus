package com.monkey.ktplus.effects.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EffectPriceCalculatorTest {
    @Test
    void addsPremiumToCategoryFloor() {
        CategoryDefinition category = new CategoryDefinition(EffectCategory.RARE, "Rare", "GOLD_INGOT", 25000);
        assertEquals(26500, EffectPriceCalculator.totalPrice(category, 1500));
    }
}
