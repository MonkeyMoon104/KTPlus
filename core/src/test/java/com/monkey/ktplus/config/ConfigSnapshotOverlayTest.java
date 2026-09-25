package com.monkey.ktplus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ConfigSnapshotOverlayTest {
    @Test
    void messageOverlayReturnsOnlyExplicitKeys() {
        YamlConfiguration empty = new YamlConfiguration();
        YamlConfiguration messages = new YamlConfiguration();
        messages.set("no-permission", "denied");
        ConfigSnapshot snapshot = new ConfigSnapshot(empty, messages, empty, empty, empty, empty, empty, empty, empty);
        assertEquals("denied", snapshot.messageOverlay("no-permission"));
        assertNull(snapshot.messageOverlay("missing-key"));
    }

    @Test
    void defaultLanguageFallsBackToEn() {
        YamlConfiguration main = new YamlConfiguration();
        YamlConfiguration empty = new YamlConfiguration();
        ConfigSnapshot snapshot = new ConfigSnapshot(main, empty, empty, empty, empty, empty, empty, empty, empty);
        assertEquals("EN", snapshot.defaultLanguage());
        main.set("default-language", "it");
        snapshot = new ConfigSnapshot(main, empty, empty, empty, empty, empty, empty, empty, empty);
        assertEquals("it", snapshot.defaultLanguage());
    }
}
