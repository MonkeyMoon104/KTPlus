package com.monkey.ktplus.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServerLoadProbeTest {
    @Test
    void adaptiveDisabledKeepsConfiguredCap() {
        assertEquals(12, ServerLoadProbe.adaptedHeavyCapWithTps(12, false, 18.0D, 15.0D, 10.0D));
    }

    @Test
    void criticalTpsQuartersCap() {
        assertEquals(3, ServerLoadProbe.adaptedHeavyCapWithTps(12, true, 18.0D, 15.0D, 14.0D));
    }

    @Test
    void lowTpsHalvesCap() {
        assertEquals(6, ServerLoadProbe.adaptedHeavyCapWithTps(12, true, 18.0D, 15.0D, 17.0D));
    }

    @Test
    void healthyTpsKeepsCap() {
        assertEquals(12, ServerLoadProbe.adaptedHeavyCapWithTps(12, true, 18.0D, 15.0D, 19.5D));
    }
}
