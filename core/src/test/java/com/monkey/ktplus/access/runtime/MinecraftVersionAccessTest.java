package com.monkey.ktplus.access.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class MinecraftVersionAccessTest {
    @Test
    void returnsNonBlankVersionWithoutServer() {
        String version = MinecraftVersionAccess.minecraftVersion();
        assertNotNull(version);
        assertFalse(version.trim().isEmpty());
    }
}
