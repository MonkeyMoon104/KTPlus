package com.monkey.ktplus.effects.list.tornado.animation;

import org.bukkit.configuration.ConfigurationSection;

final class TornadoStats {
    static final int MAX_LEVEL = 5;

    final int level;
    final double baseRadius;
    final double height;
    final double speed;
    final double hitRadius;
    final double ripRadius;
    final double spinSpeed;
    final int maxBlocks;
    final double damageMultiplier;
    final double knockback;

    TornadoStats(
            int level,
            double baseRadius,
            double height,
            double speed,
            double hitRadius,
            double ripRadius,
            double spinSpeed,
            int maxBlocks,
            double damageMultiplier,
            double knockback) {
        this.level = level;
        this.baseRadius = baseRadius;
        this.height = height;
        this.speed = speed;
        this.hitRadius = hitRadius;
        this.ripRadius = ripRadius;
        this.spinSpeed = spinSpeed;
        this.maxBlocks = maxBlocks;
        this.damageMultiplier = damageMultiplier;
        this.knockback = knockback;
    }

    static TornadoStats forLevel(int level, ConfigurationSection levelsSection) {
        int lvl = Math.max(1, Math.min(MAX_LEVEL, level));
        TornadoStats defaults = defaultsFor(lvl);
        if (levelsSection == null) {
            return defaults;
        }
        ConfigurationSection section = levelsSection.getConfigurationSection("f" + lvl);
        if (section == null) {
            return defaults;
        }
        return new TornadoStats(
                lvl,
                section.getDouble("base-radius", defaults.baseRadius),
                section.getDouble("height", defaults.height),
                section.getDouble("speed", defaults.speed),
                section.getDouble("hit-radius", defaults.hitRadius),
                section.getDouble("rip-radius", defaults.ripRadius),
                section.getDouble("spin-speed", defaults.spinSpeed),
                Math.max(1, section.getInt("max-blocks", defaults.maxBlocks)),
                Math.max(0.1, section.getDouble("damage-multiplier", defaults.damageMultiplier)),
                Math.max(0.2, section.getDouble("knockback", defaults.knockback)));
    }

    private static TornadoStats defaultsFor(int lvl) {
        return switch (lvl) {
            case 1 -> new TornadoStats(1, 1.15, 6.0, 0.11, 1.35, 2.2, 0.18, 5, 1.0, 0.75);
            case 2 -> new TornadoStats(2, 1.55, 7.5, 0.14, 1.7, 2.8, 0.22, 9, 1.35, 0.95);
            case 3 -> new TornadoStats(3, 2.05, 9.0, 0.17, 2.15, 3.5, 0.26, 14, 1.8, 1.15);
            case 4 -> new TornadoStats(4, 2.65, 11.0, 0.20, 2.7, 4.3, 0.30, 19, 2.3, 1.4);
            default -> new TornadoStats(5, 3.35, 13.0, 0.24, 3.4, 5.2, 0.35, 26, 2.9, 1.7);
        };
    }
}
