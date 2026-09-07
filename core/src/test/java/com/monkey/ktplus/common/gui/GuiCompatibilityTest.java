package com.monkey.ktplus.common.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuiCompatibilityTest {
    @Test
    void supportsIfModernFor121And26() {
        assertFalse(GuiCompatibility.supportsIfModern("1.20.4"));
        assertFalse(GuiCompatibility.supportsIfModern("1.17.1"));
        assertTrue(GuiCompatibility.supportsIfModern("1.21.11"));
        assertTrue(GuiCompatibility.supportsIfModern("26.2"));
    }

    @Test
    void resolveBackendKeepsBridgePreference() {
        assertEquals(
                GuiBackend.IF_MODERN,
                GuiCompatibility.resolveBackend("1.21.11", GuiBackend.IF_MODERN));
        assertEquals(
                GuiBackend.IF_MODERN,
                GuiCompatibility.resolveBackend("26.2", GuiBackend.IF_MODERN));
    }
}
