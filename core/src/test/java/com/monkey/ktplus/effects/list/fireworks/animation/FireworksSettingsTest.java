package com.monkey.ktplus.effects.list.fireworks.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class FireworksSettingsTest {
    @Test
    void defaultsMatchConstants() {
        FireworksSettings settings = FireworksSettings.defaults();
        assertEquals(FireworksSettings.DEFAULT_RADAR_RANGE, settings.radarRange());
        assertEquals(FireworksSettings.DEFAULT_RADAR_SPEED, settings.radarSpeed());
        assertEquals(FireworksSettings.DEFAULT_RADAR_PERIOD_TICKS, settings.radarPeriodTicks());
        assertEquals(FireworksSettings.DEFAULT_FIREWORKS_PER_PLAYER, settings.fireworksPerPlayer());
        assertTrue(settings.damageEnabled());
        assertEquals(FireworksSettings.DEFAULT_DAMAGE_PER_FIREWORK, settings.damagePerFirework());
        assertEquals(FireworksSettings.DEFAULT_FINALE_INTERVAL_TICKS, settings.finaleIntervalTicks());
        assertEquals(FireworksSettings.DEFAULT_FOOT_UPDATE_TICKS, settings.footUpdateTicks());
    }

    @Test
    void parsesCustomSection() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("fireworks-settings.radar.range", 20.0);
        yaml.set("fireworks-settings.radar.speed", 1.0);
        yaml.set("fireworks-settings.radar.period-ticks", 3L);
        yaml.set("fireworks-settings.marking.foot-update-ticks", 4L);
        yaml.set("perks.fireworks-per-player", 5);
        yaml.set("perks.interval-ticks", 10L);
        yaml.set("perks.damage.enabled", false);
        yaml.set("perks.damage.value", 4.5);

        FireworksSettings settings = FireworksSettings.from(yaml);
        assertEquals(20.0, settings.radarRange());
        assertEquals(1.0, settings.radarSpeed());
        assertEquals(3L, settings.radarPeriodTicks());
        assertEquals(5, settings.fireworksPerPlayer());
        assertFalse(settings.damageEnabled());
        assertEquals(4.5, settings.damagePerFirework());
        assertEquals(10L, settings.finaleIntervalTicks());
        assertEquals(4L, settings.footUpdateTicks());
    }

    @Test
    void parsesLegacyFinaleSection() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("fireworks-settings.radar.range", 12.0);
        yaml.set("fireworks-settings.finale.fireworks-per-player", 4);
        yaml.set("fireworks-settings.finale.interval-ticks", 7L);
        yaml.set("fireworks-settings.finale.damage.enabled", true);
        yaml.set("fireworks-settings.finale.damage.value", 3.0);

        FireworksSettings settings = FireworksSettings.from(yaml);
        assertEquals(4, settings.fireworksPerPlayer());
        assertEquals(7L, settings.finaleIntervalTicks());
        assertTrue(settings.damageEnabled());
        assertEquals(3.0, settings.damagePerFirework());
    }

    @Test
    void estimatedDurationUsesConservativeFormula() {
        FireworksSettings settings = FireworksSettings.defaults();
        long duration = settings.estimatedMaxDurationTicks(10);
        assertTrue(duration > 400L);
        assertTrue(duration < 900L);
    }

    @Test
    void resolveMaxDurationTicksNeverBelowDefault() {
        long duration = FireworksSettings.resolveMaxDurationTicks(null);
        assertEquals(FireworksSettings.DEFAULT_MAX_DURATION_TICKS, duration);
    }
}
