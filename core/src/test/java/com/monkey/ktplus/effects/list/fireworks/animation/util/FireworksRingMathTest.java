package com.monkey.ktplus.effects.list.fireworks.animation.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FireworksRingMathTest {
    @Test
    void horizontalDistanceUsesBlockCenter() {
        double distance = FireworksRingMath.horizontalDistance(10.5, 20.5, 12, 22);
        assertTrue(distance > 2.8 && distance < 2.9);
    }

    @Test
    void expandingRingMatchesFrontBand() {
        assertTrue(FireworksRingMath.isInExpandingRing(5.0, 5.0, 0.5));
        assertFalse(FireworksRingMath.isInExpandingRing(2.0, 5.0, 0.5));
    }

    @Test
    void restoreTriggersBehindWaveFront() {
        assertTrue(FireworksRingMath.shouldRestoreWave(2.0, 5.0));
        assertFalse(FireworksRingMath.shouldRestoreWave(4.5, 5.0));
    }

    @Test
    void radarRangeUsesHorizontalDistance() {
        assertTrue(FireworksRingMath.isWithinRadar(5.0, 6.0));
        assertFalse(FireworksRingMath.isWithinRadar(7.0, 6.0));
    }

    @Test
    void boundingRadiusIncludesRingWidth() {
        assertTrue(FireworksRingMath.boundingRadius(5.0) >= 6);
    }
}
