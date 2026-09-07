package com.monkey.ktplus.effects.list.fireworks.animation;

import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;

public final class FireworksSettings {
    public static final double DEFAULT_RADAR_RANGE = 12.0;
    public static final double DEFAULT_RADAR_SPEED = 0.5;
    public static final long DEFAULT_RADAR_PERIOD_TICKS = 2L;
    public static final int DEFAULT_FIREWORKS_PER_PLAYER = 3;
    public static final boolean DEFAULT_DAMAGE_ENABLED = true;
    public static final double DEFAULT_DAMAGE_PER_FIREWORK = 2.0;
    public static final long DEFAULT_FINALE_INTERVAL_TICKS = 8L;
    public static final long DEFAULT_FOOT_UPDATE_TICKS = 2L;
    public static final long DEFAULT_MAX_DURATION_TICKS = 600L;
    private static final double HOMING_SPEED_BLOCKS_PER_TICK = 1.35;

    private final double radarRange;
    private final double radarSpeed;
    private final long radarPeriodTicks;
    private final int fireworksPerPlayer;
    private final boolean damageEnabled;
    private final double damagePerFirework;
    private final long finaleIntervalTicks;
    private final long footUpdateTicks;

    public FireworksSettings(
            double radarRange,
            double radarSpeed,
            long radarPeriodTicks,
            int fireworksPerPlayer,
            boolean damageEnabled,
            double damagePerFirework,
            long finaleIntervalTicks,
            long footUpdateTicks) {
        this.radarRange = Math.max(1.0, radarRange);
        this.radarSpeed = Math.max(0.1, radarSpeed);
        this.radarPeriodTicks = Math.max(1L, radarPeriodTicks);
        this.fireworksPerPlayer = Math.max(1, fireworksPerPlayer);
        this.damageEnabled = damageEnabled;
        this.damagePerFirework = Math.max(0.0, damagePerFirework);
        this.finaleIntervalTicks = Math.max(1L, finaleIntervalTicks);
        this.footUpdateTicks = Math.max(1L, footUpdateTicks);
    }

    public static FireworksSettings defaults() {
        return new FireworksSettings(
                DEFAULT_RADAR_RANGE,
                DEFAULT_RADAR_SPEED,
                DEFAULT_RADAR_PERIOD_TICKS,
                DEFAULT_FIREWORKS_PER_PLAYER,
                DEFAULT_DAMAGE_ENABLED,
                DEFAULT_DAMAGE_PER_FIREWORK,
                DEFAULT_FINALE_INTERVAL_TICKS,
                DEFAULT_FOOT_UPDATE_TICKS);
    }

    public static FireworksSettings from(@Nullable ConfigurationSection effectSection) {
        if (effectSection == null) {
            return defaults();
        }
        ConfigurationSection settings = effectSection.getConfigurationSection("fireworks-settings");
        ConfigurationSection perks = effectSection.getConfigurationSection("perks");
        ConfigurationSection radar = settings == null ? null : settings.getConfigurationSection("radar");
        ConfigurationSection marking = settings == null ? null : settings.getConfigurationSection("marking");
        ConfigurationSection legacyFinale = settings == null ? null : settings.getConfigurationSection("finale");
        ConfigurationSection damage = perks == null ? null : perks.getConfigurationSection("damage");
        if (damage == null && legacyFinale != null) {
            damage = legacyFinale.getConfigurationSection("damage");
        }
        double legacyDamage = legacyFinale == null
                ? DEFAULT_DAMAGE_PER_FIREWORK
                : legacyFinale.getDouble("damage-per-firework", -1.0);
        double damageValue = damage == null
                ? (legacyDamage >= 0.0 ? legacyDamage : DEFAULT_DAMAGE_PER_FIREWORK)
                : damage.getDouble("value", DEFAULT_DAMAGE_PER_FIREWORK);
        boolean damageEnabled = damage == null
                ? DEFAULT_DAMAGE_ENABLED
                : damage.getBoolean("enabled", DEFAULT_DAMAGE_ENABLED);
        int fireworksPerPlayer = perks != null
                ? perks.getInt("fireworks-per-player", DEFAULT_FIREWORKS_PER_PLAYER)
                : (legacyFinale == null
                        ? DEFAULT_FIREWORKS_PER_PLAYER
                        : legacyFinale.getInt("fireworks-per-player", DEFAULT_FIREWORKS_PER_PLAYER));
        long finaleIntervalTicks = perks != null
                ? perks.getLong("interval-ticks", DEFAULT_FINALE_INTERVAL_TICKS)
                : (legacyFinale == null
                        ? DEFAULT_FINALE_INTERVAL_TICKS
                        : legacyFinale.getLong("interval-ticks", DEFAULT_FINALE_INTERVAL_TICKS));
        return new FireworksSettings(
                radar == null ? DEFAULT_RADAR_RANGE : radar.getDouble("range", DEFAULT_RADAR_RANGE),
                radar == null ? DEFAULT_RADAR_SPEED : radar.getDouble("speed", DEFAULT_RADAR_SPEED),
                radar == null
                        ? DEFAULT_RADAR_PERIOD_TICKS
                        : radar.getLong("period-ticks", DEFAULT_RADAR_PERIOD_TICKS),
                fireworksPerPlayer,
                damageEnabled,
                damageValue,
                finaleIntervalTicks,
                marking == null
                        ? DEFAULT_FOOT_UPDATE_TICKS
                        : marking.getLong("foot-update-ticks", DEFAULT_FOOT_UPDATE_TICKS));
    }

    public long estimatedRadarDurationTicks() {
        int radarSteps = (int) Math.ceil(radarRange / radarSpeed);
        return (long) radarSteps * radarPeriodTicks + 20L;
    }

    public long estimatedMaxDurationTicks(int assumedMarkedPlayers) {
        int radarSteps = (int) Math.ceil(radarRange / radarSpeed);
        long radarTicks = (long) radarSteps * radarPeriodTicks;
        int marked = Math.max(1, assumedMarkedPlayers);
        int totalShots = marked * fireworksPerPlayer;
        long lastLaunchDelay = (long) Math.max(0, totalShots - 1) * finaleIntervalTicks;
        long chaseTicks = (long) Math.ceil((radarRange * 4.0) / HOMING_SPEED_BLOCKS_PER_TICK) + 80L;
        return radarTicks + lastLaunchDelay + chaseTicks + 60L;
    }

    public double radarRange() {
        return radarRange;
    }

    public double radarSpeed() {
        return radarSpeed;
    }

    public long radarPeriodTicks() {
        return radarPeriodTicks;
    }

    public int fireworksPerPlayer() {
        return fireworksPerPlayer;
    }

    public boolean damageEnabled() {
        return damageEnabled;
    }

    public double damagePerFirework() {
        return damagePerFirework;
    }

    public long finaleIntervalTicks() {
        return finaleIntervalTicks;
    }

    public long footUpdateTicks() {
        return footUpdateTicks;
    }

    public static long resolveMaxDurationTicks(@Nullable ConfigurationSection effectSection) {
        FireworksSettings settings = from(effectSection);
        return Math.max(DEFAULT_MAX_DURATION_TICKS, settings.estimatedMaxDurationTicks(10));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FireworksSettings)) {
            return false;
        }
        FireworksSettings that = (FireworksSettings) other;
        return Double.compare(that.radarRange, radarRange) == 0
                && Double.compare(that.radarSpeed, radarSpeed) == 0
                && radarPeriodTicks == that.radarPeriodTicks
                && fireworksPerPlayer == that.fireworksPerPlayer
                && damageEnabled == that.damageEnabled
                && Double.compare(that.damagePerFirework, damagePerFirework) == 0
                && finaleIntervalTicks == that.finaleIntervalTicks
                && footUpdateTicks == that.footUpdateTicks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                radarRange,
                radarSpeed,
                radarPeriodTicks,
                fireworksPerPlayer,
                damageEnabled,
                damagePerFirework,
                finaleIntervalTicks,
                footUpdateTicks);
    }
}
