package com.monkey.ktplus.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PluginVersionTest {
    @Test
    void newerNumericVersionsWin() {
        assertTrue(PluginVersion.isNewer("4.0.1", "4.0.0"));
        assertTrue(PluginVersion.isNewer("5.0.0", "4.0.0"));
        assertFalse(PluginVersion.isNewer("3.0.3", "4.0.0"));
        assertFalse(PluginVersion.isNewer("4.0.0", "4.0.0"));
    }

    @Test
    void normalizesPrefixesAndIgnoresBuildMetadata() {
        assertEquals("4.0.0", PluginVersion.normalize("v4.0.0"));
        assertEquals("4.0.0", PluginVersion.normalize("4.0.0+build.12"));
        assertTrue(PluginVersion.isNewer("v4.1.0", "4.0.0"));
        assertFalse(PluginVersion.isNewer("4.0.0+old", "4.0.0"));
    }

    @Test
    void comparesUnevenComponentCounts() {
        assertTrue(PluginVersion.isNewer("4.0.0.1", "4.0.0"));
        assertFalse(PluginVersion.isNewer("4.0", "4.0.1"));
        assertEquals(0, PluginVersion.compare("4.0.0", "4.0"));
    }
}
