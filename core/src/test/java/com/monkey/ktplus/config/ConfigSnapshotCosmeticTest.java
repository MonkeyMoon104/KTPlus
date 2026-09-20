package com.monkey.ktplus.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ConfigSnapshotCosmeticTest {
    @Test
    void cosmeticModeDefaultsOff() {
        ConfigSnapshot config = snapshot(false);
        assertFalse(config.cosmeticMode());
    }

    @Test
    void cosmeticModeForcesDamageAndStructureOff() {
        YamlConfiguration main = new YamlConfiguration();
        main.set("cosmetic-mode", true);
        YamlConfiguration effects = new YamlConfiguration();
        effects.set("effects.fire.damage.enabled", true);
        effects.set("effects.fire.damage.value", 5.0D);
        effects.set("effects.fire.damage.radius", 3.0D);
        effects.set("effects.fire.structure", true);
        ConfigSnapshot config = snapshot(main, effects);

        assertTrue(config.cosmeticMode());
        assertFalse(config.effectDamage("fire").enabled());
        assertFalse(config.effectStructure("fire", true));
    }

    @Test
    void withoutCosmeticModeDamageConfigIsHonored() {
        YamlConfiguration main = new YamlConfiguration();
        main.set("cosmetic-mode", false);
        YamlConfiguration effects = new YamlConfiguration();
        effects.set("effects.fire.damage.enabled", true);
        effects.set("effects.fire.structure", true);
        ConfigSnapshot config = snapshot(main, effects);

        assertTrue(config.effectDamage("fire").enabled());
        assertTrue(config.effectStructure("fire", false));
    }

    private static ConfigSnapshot snapshot(boolean cosmetic) {
        YamlConfiguration main = new YamlConfiguration();
        main.set("cosmetic-mode", cosmetic);
        return snapshot(main, new YamlConfiguration());
    }

    private static ConfigSnapshot snapshot(YamlConfiguration main, YamlConfiguration effects) {
        YamlConfiguration empty = new YamlConfiguration();
        return new ConfigSnapshot(main, empty, effects, empty, empty, empty, empty, empty, empty);
    }
}
