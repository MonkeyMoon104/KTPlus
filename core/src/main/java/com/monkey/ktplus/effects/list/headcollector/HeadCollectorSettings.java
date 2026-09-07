package com.monkey.ktplus.effects.list.headcollector;

import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;

public final class HeadCollectorSettings {
    public static final int DEFAULT_MAX_HEADS = 5;
    public static final boolean DEFAULT_INHERIT_HEADS = true;
    public static final double DEFAULT_ORBIT_RADIUS = 1.25;
    public static final double DEFAULT_ORBIT_HEIGHT = 0.95;
    public static final double DEFAULT_ORBIT_SPEED = 0.085;
    public static final double DEFAULT_HEAD_SPIN_SPEED = 0.026;
    public static final int DEFAULT_ORBIT_TRAIL_INTERVAL = 2;
    public static final String DEFAULT_READY_LABEL = "READY";
    public static final int DEFAULT_MISSILE_RISE_TICKS = 5;
    public static final int DEFAULT_MISSILE_STRIKE_TICKS = 10;
    public static final long DEFAULT_MISSILE_GAP_TICKS = 2L;
    public static final int DEFAULT_FORMATION_HOLD_TICKS = 10;
    public static final double DEFAULT_FORMATION_SPACING = 0.82;
    public static final double DEFAULT_FORMATION_DISTANCE = 1.85;
    public static final double DEFAULT_FORMATION_HEIGHT = 1.35;
    public static final double DEFAULT_ATTACK_DAMAGE = 4.0;
    public static final double DEFAULT_ATTACK_DAMAGE_PER_HEAD = 1.5;
    public static final double DEFAULT_HEALTH_PER_HEAD = 2.0;
    public static final long INTRO_DURATION_TICKS = 22L;

    private final int maxHeads;
    private final boolean inheritHeads;
    private final double orbitRadius;
    private final double orbitHeight;
    private final double orbitSpeed;
    private final double headSpinSpeed;
    private final int orbitTrailInterval;
    private final String readyLabel;
    private final int missileRiseTicks;
    private final int missileStrikeTicks;
    private final long missileGapTicks;
    private final int formationHoldTicks;
    private final double formationSpacing;
    private final double formationDistance;
    private final double formationHeight;
    private final double attackDamage;
    private final double attackDamagePerHead;
    private final double healthPerHead;

    public HeadCollectorSettings(
            int maxHeads,
            boolean inheritHeads,
            double orbitRadius,
            double orbitHeight,
            double orbitSpeed,
            double headSpinSpeed,
            int orbitTrailInterval,
            String readyLabel,
            int missileRiseTicks,
            int missileStrikeTicks,
            long missileGapTicks,
            int formationHoldTicks,
            double formationSpacing,
            double formationDistance,
            double formationHeight,
            double attackDamage,
            double attackDamagePerHead,
            double healthPerHead) {
        this.maxHeads = Math.max(1, maxHeads);
        this.inheritHeads = inheritHeads;
        this.orbitRadius = Math.max(0.5, orbitRadius);
        this.orbitHeight = Math.max(0.5, orbitHeight);
        this.orbitSpeed = Math.max(0.01, orbitSpeed);
        this.headSpinSpeed = Math.max(0.005, headSpinSpeed);
        this.orbitTrailInterval = Math.max(1, orbitTrailInterval);
        this.readyLabel = readyLabel == null || readyLabel.isBlank() ? DEFAULT_READY_LABEL : readyLabel;
        this.missileRiseTicks = Math.max(3, missileRiseTicks);
        this.missileStrikeTicks = Math.max(4, missileStrikeTicks);
        this.missileGapTicks = Math.max(1L, missileGapTicks);
        this.formationHoldTicks = Math.max(0, formationHoldTicks);
        this.formationSpacing = Math.max(0.2, formationSpacing);
        this.formationDistance = Math.max(0.5, formationDistance);
        this.formationHeight = Math.max(0.5, formationHeight);
        this.attackDamage = Math.max(0.0, attackDamage);
        this.attackDamagePerHead = Math.max(0.0, attackDamagePerHead);
        this.healthPerHead = Math.max(0.0, healthPerHead);
    }

    public static HeadCollectorSettings defaults() {
        return new HeadCollectorSettings(
                DEFAULT_MAX_HEADS,
                DEFAULT_INHERIT_HEADS,
                DEFAULT_ORBIT_RADIUS,
                DEFAULT_ORBIT_HEIGHT,
                DEFAULT_ORBIT_SPEED,
                DEFAULT_HEAD_SPIN_SPEED,
                DEFAULT_ORBIT_TRAIL_INTERVAL,
                DEFAULT_READY_LABEL,
                DEFAULT_MISSILE_RISE_TICKS,
                DEFAULT_MISSILE_STRIKE_TICKS,
                DEFAULT_MISSILE_GAP_TICKS,
                DEFAULT_FORMATION_HOLD_TICKS,
                DEFAULT_FORMATION_SPACING,
                DEFAULT_FORMATION_DISTANCE,
                DEFAULT_FORMATION_HEIGHT,
                DEFAULT_ATTACK_DAMAGE,
                DEFAULT_ATTACK_DAMAGE_PER_HEAD,
                DEFAULT_HEALTH_PER_HEAD);
    }

    public static HeadCollectorSettings from(@Nullable ConfigurationSection effectSection) {
        if (effectSection == null) {
            return defaults();
        }
        ConfigurationSection root = effectSection.getConfigurationSection("head-collector-settings");
        if (root == null) {
            return defaults();
        }
        ConfigurationSection collection = root.getConfigurationSection("collection");
        ConfigurationSection orbit = root.getConfigurationSection("orbit");
        ConfigurationSection actionBar = root.getConfigurationSection("action-bar");
        ConfigurationSection missile = root.getConfigurationSection("missile");
        ConfigurationSection formation = root.getConfigurationSection("formation");
        ConfigurationSection perks = effectSection.getConfigurationSection("perks");
        if (perks == null) {
            perks = root.getConfigurationSection("perks");
        }

        return new HeadCollectorSettings(
                readInt(root, collection, "max-heads", "max-heads", DEFAULT_MAX_HEADS),
                readBoolean(root, collection, "inherit-heads", "inherit-heads", DEFAULT_INHERIT_HEADS),
                readDouble(root, orbit, "radius", "orbit-radius", DEFAULT_ORBIT_RADIUS),
                readDouble(root, orbit, "height", "orbit-height", DEFAULT_ORBIT_HEIGHT),
                readDouble(root, orbit, "speed", "orbit-speed", DEFAULT_ORBIT_SPEED),
                readDouble(root, orbit, "head-spin-speed", "head-spin-speed", DEFAULT_HEAD_SPIN_SPEED),
                readInt(root, orbit, "trail-interval", "orbit-trail-interval", DEFAULT_ORBIT_TRAIL_INTERVAL),
                readString(root, actionBar, "ready-label", "ready-label", DEFAULT_READY_LABEL),
                readInt(root, missile, "rise-ticks", "missile-rise-ticks", DEFAULT_MISSILE_RISE_TICKS),
                readInt(root, missile, "strike-ticks", "missile-strike-ticks", DEFAULT_MISSILE_STRIKE_TICKS),
                readLong(root, missile, "gap-ticks", "missile-gap-ticks", DEFAULT_MISSILE_GAP_TICKS),
                readInt(root, formation, "hold-ticks", "formation-hold-ticks", DEFAULT_FORMATION_HOLD_TICKS),
                readDouble(root, formation, "spacing", "formation-spacing", DEFAULT_FORMATION_SPACING),
                readDouble(root, formation, "distance", "formation-distance", DEFAULT_FORMATION_DISTANCE),
                readDouble(root, formation, "height", "formation-height", DEFAULT_FORMATION_HEIGHT),
                readDouble(root, perks, "attack-damage", "attack-damage", DEFAULT_ATTACK_DAMAGE),
                readDouble(root, perks, "attack-damage-per-head", "attack-damage-per-head", DEFAULT_ATTACK_DAMAGE_PER_HEAD),
                readDouble(root, perks, "health-per-head", "health-per-head", DEFAULT_HEALTH_PER_HEAD));
    }

    private static int readInt(
            ConfigurationSection root,
            @Nullable ConfigurationSection group,
            String key,
            String legacyKey,
            int defaultValue) {
        if (group != null && group.contains(key)) {
            return group.getInt(key);
        }
        if (root.contains(legacyKey)) {
            return root.getInt(legacyKey);
        }
        return defaultValue;
    }

    private static long readLong(
            ConfigurationSection root,
            @Nullable ConfigurationSection group,
            String key,
            String legacyKey,
            long defaultValue) {
        if (group != null && group.contains(key)) {
            return group.getLong(key);
        }
        if (root.contains(legacyKey)) {
            return root.getLong(legacyKey);
        }
        return defaultValue;
    }

    private static double readDouble(
            ConfigurationSection root,
            @Nullable ConfigurationSection group,
            String key,
            String legacyKey,
            double defaultValue) {
        if (group != null && group.contains(key)) {
            return group.getDouble(key);
        }
        if (root.contains(legacyKey)) {
            return root.getDouble(legacyKey);
        }
        return defaultValue;
    }

    private static String readString(
            ConfigurationSection root,
            @Nullable ConfigurationSection group,
            String key,
            String legacyKey,
            String defaultValue) {
        if (group != null && group.contains(key)) {
            return Objects.requireNonNullElse(group.getString(key), defaultValue);
        }
        if (root.contains(legacyKey)) {
            return Objects.requireNonNullElse(root.getString(legacyKey), defaultValue);
        }
        return defaultValue;
    }

    private static boolean readBoolean(
            ConfigurationSection root,
            @Nullable ConfigurationSection group,
            String key,
            String legacyKey,
            boolean defaultValue) {
        if (group != null && group.contains(key)) {
            return group.getBoolean(key);
        }
        if (root.contains(legacyKey)) {
            return root.getBoolean(legacyKey);
        }
        return defaultValue;
    }

    public int maxHeads() {
        return maxHeads;
    }

    public boolean inheritHeads() {
        return inheritHeads;
    }

    public double orbitRadius() {
        return orbitRadius;
    }

    public double orbitHeight() {
        return orbitHeight;
    }

    public double orbitSpeed() {
        return orbitSpeed;
    }

    public double headSpinSpeed() {
        return headSpinSpeed;
    }

    public int orbitTrailInterval() {
        return orbitTrailInterval;
    }

    public String readyLabel() {
        return readyLabel;
    }

    public int missileRiseTicks() {
        return missileRiseTicks;
    }

    public int missileStrikeTicks() {
        return missileStrikeTicks;
    }

    public long missileGapTicks() {
        return missileGapTicks;
    }

    public int formationHoldTicks() {
        return formationHoldTicks;
    }

    public double formationSpacing() {
        return formationSpacing;
    }

    public double formationDistance() {
        return formationDistance;
    }

    public double formationHeight() {
        return formationHeight;
    }

    public double attackDamage() {
        return attackDamage;
    }

    public double attackDamagePerHead() {
        return attackDamagePerHead;
    }

    public double healthPerHead() {
        return healthPerHead;
    }

    public double launchDamageForHeads(int headCount) {
        int count = Math.max(1, headCount);
        return attackDamage + (attackDamagePerHead * (count - 1));
    }

    public float absorptionForHeads(int headCount) {
        return (float) (healthPerHead * Math.max(0, headCount));
    }

    public long introDurationTicks() {
        return INTRO_DURATION_TICKS;
    }
}
