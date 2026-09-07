package com.monkey.ktplus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ConfigSnapshotEconomyTest {
    @Test
    void defaultsToKillcoinsWhenUseInternalTrue() {
        YamlConfiguration economy = new YamlConfiguration();
        economy.set("use-internal", true);
        ConfigSnapshot config = snapshot(economy);
        assertEquals("KILLCOINS", config.economyProviderRaw());
    }

    @Test
    void explicitProviderWins() {
        YamlConfiguration economy = new YamlConfiguration();
        economy.set("provider", "VAULT");
        economy.set("use-internal", true);
        ConfigSnapshot config = snapshot(economy);
        assertEquals("VAULT", config.economyProviderRaw());
    }

    @Test
    void economyEnabledByDefault() {
        ConfigSnapshot config = snapshot(new YamlConfiguration());
        assertTrue(config.economyEnabled());
    }

    private static ConfigSnapshot snapshot(YamlConfiguration economy) {
        YamlConfiguration empty = new YamlConfiguration();
        return new ConfigSnapshot(empty, empty, empty, economy, empty, empty, empty, empty, empty);
    }
}
