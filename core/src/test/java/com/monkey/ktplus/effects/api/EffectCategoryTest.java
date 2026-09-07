package com.monkey.ktplus.effects.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EffectCategoryTest {
    @Test
    void parsesConfigIds() {
        assertEquals(EffectCategory.NON_COMMON, EffectCategory.fromConfigId("non-common", EffectCategory.COMMON));
        assertEquals(EffectCategory.VERY_RARE, EffectCategory.fromConfigId("very-rare", EffectCategory.COMMON));
        assertEquals(EffectCategory.COMMON, EffectCategory.fromConfigId("unknown", EffectCategory.COMMON));
    }
}
