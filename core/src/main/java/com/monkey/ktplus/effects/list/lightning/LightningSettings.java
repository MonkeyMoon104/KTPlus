package com.monkey.ktplus.effects.list.lightning;

import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;

public final class LightningSettings {
    public static final boolean DEFAULT_CHAIN = true;
    public static final int DEFAULT_MAX_CHAIN_DEPTH = 3;
    public static final double DEFAULT_RADAR_RANGE = 12.0;
    public static final double DEFAULT_RADAR_SPEED = 0.55;
    public static final long DEFAULT_RADAR_PERIOD_TICKS = 2L;
    public static final boolean DEFAULT_DAMAGE_ENABLED = true;
    public static final double DEFAULT_DAMAGE_VALUE = 5.0;
    public static final long DEFAULT_MAX_DURATION_TICKS = 200L;

    private final boolean chain;
    private final int maxChainDepth;
    private final double radarRange;
    private final double radarSpeed;
    private final long radarPeriodTicks;
    private final boolean damageEnabled;
    private final double damageValue;

    public LightningSettings(
            boolean chain,
            int maxChainDepth,
            double radarRange,
            double radarSpeed,
            long radarPeriodTicks,
            boolean damageEnabled,
            double damageValue) {
        this.chain = chain;
        this.maxChainDepth = Math.max(0, maxChainDepth);
        this.radarRange = Math.max(1.0, radarRange);
        this.radarSpeed = Math.max(0.1, radarSpeed);
        this.radarPeriodTicks = Math.max(1L, radarPeriodTicks);
        this.damageEnabled = damageEnabled;
        this.damageValue = Math.max(0.0, damageValue);
    }

    public static LightningSettings defaults() {
        return new LightningSettings(
                DEFAULT_CHAIN,
                DEFAULT_MAX_CHAIN_DEPTH,
                DEFAULT_RADAR_RANGE,
                DEFAULT_RADAR_SPEED,
                DEFAULT_RADAR_PERIOD_TICKS,
                DEFAULT_DAMAGE_ENABLED,
                DEFAULT_DAMAGE_VALUE);
    }

    public static LightningSettings from(@Nullable ConfigurationSection effectSection) {
        if (effectSection == null) {
            return defaults();
        }
        ConfigurationSection perks = effectSection.getConfigurationSection("perks");
        ConfigurationSection radar = perks == null ? null : perks.getConfigurationSection("radar");
        ConfigurationSection damage = effectSection.getConfigurationSection("damage");
        return new LightningSettings(
                perks == null ? DEFAULT_CHAIN : perks.getBoolean("chain", DEFAULT_CHAIN),
                perks == null
                        ? DEFAULT_MAX_CHAIN_DEPTH
                        : perks.getInt("max-chain-depth", DEFAULT_MAX_CHAIN_DEPTH),
                radar == null ? DEFAULT_RADAR_RANGE : radar.getDouble("range", DEFAULT_RADAR_RANGE),
                radar == null ? DEFAULT_RADAR_SPEED : radar.getDouble("speed", DEFAULT_RADAR_SPEED),
                radar == null
                        ? DEFAULT_RADAR_PERIOD_TICKS
                        : radar.getLong("period-ticks", DEFAULT_RADAR_PERIOD_TICKS),
                damage == null
                        ? DEFAULT_DAMAGE_ENABLED
                        : damage.getBoolean("enabled", DEFAULT_DAMAGE_ENABLED),
                damage == null
                        ? DEFAULT_DAMAGE_VALUE
                        : damage.getDouble("value", DEFAULT_DAMAGE_VALUE));
    }

    public long estimatedRadarDurationTicks() {
        int radarSteps = (int) Math.ceil(radarRange / radarSpeed);
        return (long) radarSteps * radarPeriodTicks + 20L;
    }

    public long estimatedMaxDurationTicks() {
        int depthLayers = chain ? maxChainDepth + 1 : 1;
        return Math.max(DEFAULT_MAX_DURATION_TICKS, estimatedRadarDurationTicks() * depthLayers + 40L);
    }

    public static long resolveMaxDurationTicks(@Nullable ConfigurationSection effectSection) {
        return from(effectSection).estimatedMaxDurationTicks();
    }

    public boolean chain() {
        return chain;
    }

    public int maxChainDepth() {
        return maxChainDepth;
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

    public boolean damageEnabled() {
        return damageEnabled;
    }

    public double damageValue() {
        return damageValue;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LightningSettings)) {
            return false;
        }
        LightningSettings that = (LightningSettings) other;
        return chain == that.chain
                && maxChainDepth == that.maxChainDepth
                && Double.compare(that.radarRange, radarRange) == 0
                && Double.compare(that.radarSpeed, radarSpeed) == 0
                && radarPeriodTicks == that.radarPeriodTicks
                && damageEnabled == that.damageEnabled
                && Double.compare(that.damageValue, damageValue) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                chain,
                maxChainDepth,
                radarRange,
                radarSpeed,
                radarPeriodTicks,
                damageEnabled,
                damageValue);
    }
}
