package com.monkey.ktplus.hook.worldguard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorldGuardBlockChangeModeTest {
    @Test
    void fromConfigDefaultsToRespectWhenFlagTrue() {
        assertEquals(WorldGuardHook.BlockChangeMode.RESPECT, WorldGuardHook.BlockChangeMode.fromConfig(null, true));
        assertEquals(WorldGuardHook.BlockChangeMode.RESPECT, WorldGuardHook.BlockChangeMode.fromConfig("", true));
        assertEquals(WorldGuardHook.BlockChangeMode.RESPECT, WorldGuardHook.BlockChangeMode.fromConfig("RESPECT", true));
        assertEquals(WorldGuardHook.BlockChangeMode.RESPECT, WorldGuardHook.BlockChangeMode.fromConfig("respect", true));
    }

    @Test
    void fromConfigBypassAliases() {
        assertEquals(WorldGuardHook.BlockChangeMode.BYPASS, WorldGuardHook.BlockChangeMode.fromConfig("BYPASS", true));
        assertEquals(WorldGuardHook.BlockChangeMode.BYPASS, WorldGuardHook.BlockChangeMode.fromConfig("IGNORE", true));
    }

    @Test
    void fromConfigUsesBuildFlagWhenRawMissing() {
        assertEquals(WorldGuardHook.BlockChangeMode.BYPASS, WorldGuardHook.BlockChangeMode.fromConfig(null, false));
    }
}
