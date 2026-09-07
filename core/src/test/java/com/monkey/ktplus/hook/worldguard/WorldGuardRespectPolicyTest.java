package com.monkey.ktplus.hook.worldguard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldGuardRespectPolicyTest {
    @Test
    void brokenHookFailClosedEvenWithBypass() {
        assertFalse(WorldGuardHook.evaluateRespect(false, true, true));
    }

    @Test
    void bypassAllowsWhenHookAvailable() {
        assertTrue(WorldGuardHook.evaluateRespect(true, true, false));
    }

    @Test
    void regionDenyBlocksWithoutBypass() {
        assertFalse(WorldGuardHook.evaluateRespect(true, false, false));
    }

    @Test
    void regionAllowPermitsWithoutBypass() {
        assertTrue(WorldGuardHook.evaluateRespect(true, false, true));
    }
}
