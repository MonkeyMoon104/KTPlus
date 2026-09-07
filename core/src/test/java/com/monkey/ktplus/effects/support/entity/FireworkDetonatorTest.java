package com.monkey.ktplus.effects.support.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import com.monkey.ktplus.effects.support.entity.FireworkDetonator;

class FireworkDetonatorTest {
    @Test
    void detonateIgnoresNullAndInvalidFirework() {
        assertDoesNotThrow(() -> FireworkDetonator.detonate(null));
    }
}
