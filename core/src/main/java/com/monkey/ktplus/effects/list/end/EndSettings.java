package com.monkey.ktplus.effects.list.end;

import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;

public final class EndSettings {
    public static final int DEFAULT_ENDERMAN_COUNT = 5;
    public static final double DEFAULT_TARGET_RANGE = 24.0;
    public static final double DEFAULT_PORTAL_RADIUS = 2.5;
    public static final double DEFAULT_PORTAL_DURATION_SECONDS = 5.0;
    public static final long EFFECT_DURATION_TICKS = 160L;

    public static final String DEFAULT_TRAP_BLOCK = "END_PORTAL_FRAME";
    public static final double DEFAULT_TRAP_DURATION_SECONDS = 2.0;
    public static final double DEFAULT_SEARCH_DURATION_SECONDS = 3.0;
    public static final double DEFAULT_BLOCK_PLACE_INTERVAL_SECONDS = 0.55;
    public static final double DEFAULT_DISMISS_DELAY_SECONDS = 1.0;
    private static final long TICKS_PER_SECOND = 20L;
    private static final long DEFAULT_BLOCK_PLACE_WINDUP_TICKS = 8L;

    private final int endermanCount;
    private final long portalDurationTicks;
    private final double targetRange;
    private final double portalRadius;
    private final Material trapBlock;
    private final long trapDurationTicks;
    private final long searchDurationTicks;
    private final long blockPlaceIntervalTicks;
    private final long blockPlaceWindupTicks;
    private final long dismissDelayTicks;

    public EndSettings(
            int endermanCount,
            long portalDurationTicks,
            double targetRange,
            double portalRadius,
            Material trapBlock,
            long trapDurationTicks,
            long searchDurationTicks,
            long blockPlaceIntervalTicks,
            long blockPlaceWindupTicks,
            long dismissDelayTicks) {
        this.endermanCount = Math.max(1, endermanCount);
        this.portalDurationTicks = Math.max(1L, portalDurationTicks);
        this.targetRange = Math.max(1.0, targetRange);
        this.portalRadius = Math.max(0.5, portalRadius);
        this.trapBlock = Objects.requireNonNull(trapBlock, "trapBlock");
        this.trapDurationTicks = Math.max(1L, trapDurationTicks);
        this.searchDurationTicks = Math.max(0L, searchDurationTicks);
        this.blockPlaceIntervalTicks = Math.max(1L, blockPlaceIntervalTicks);
        this.blockPlaceWindupTicks = Math.max(1L, blockPlaceWindupTicks);
        this.dismissDelayTicks = Math.max(1L, dismissDelayTicks);
    }

    public static EndSettings defaults() {
        return new EndSettings(
                DEFAULT_ENDERMAN_COUNT,
                secondsToTicks(DEFAULT_PORTAL_DURATION_SECONDS),
                DEFAULT_TARGET_RANGE,
                DEFAULT_PORTAL_RADIUS,
                resolveTrapBlock(DEFAULT_TRAP_BLOCK),
                secondsToTicks(DEFAULT_TRAP_DURATION_SECONDS),
                secondsToTicks(DEFAULT_SEARCH_DURATION_SECONDS),
                secondsToTicks(DEFAULT_BLOCK_PLACE_INTERVAL_SECONDS),
                DEFAULT_BLOCK_PLACE_WINDUP_TICKS,
                secondsToTicks(DEFAULT_DISMISS_DELAY_SECONDS));
    }

    public static EndSettings from(@Nullable ConfigurationSection effectSection) {
        if (effectSection == null) {
            return defaults();
        }
        ConfigurationSection perks = effectSection.getConfigurationSection("perks");
        ConfigurationSection portal = perks != null ? perks : effectSection.getConfigurationSection("end-portal");
        if (portal == null) {
            portal = effectSection.getConfigurationSection("end-trap");
        }
        ConfigurationSection trap = effectSection.getConfigurationSection("end-trap");
        if (trap == null) {
            trap = portal;
        }
        if (portal == null && trap == null) {
            return defaults();
        }

        ConfigurationSection portalSource = portal != null ? portal : trap;
        ConfigurationSection trapSource = trap != null ? trap : portalSource;

        int count = portalSource.getInt("enderman-count", DEFAULT_ENDERMAN_COUNT);
        double range = portalSource.getDouble("target-range", DEFAULT_TARGET_RANGE);
        double radius = portalSource.getDouble("portal-radius", DEFAULT_PORTAL_RADIUS);
        double portalSeconds = portalSource.getDouble("portal-duration-seconds", DEFAULT_PORTAL_DURATION_SECONDS);

        String blockKey = trapSource.getString("block", DEFAULT_TRAP_BLOCK);
        Material block = MaterialResolver.find(blockKey);
        if (block == null || block.isAir()) {
            block = resolveTrapBlock(DEFAULT_TRAP_BLOCK);
        }
        double trapSeconds = trapSource.getDouble("duration-seconds", DEFAULT_TRAP_DURATION_SECONDS);
        double searchSeconds = trapSource.getDouble("search-duration-seconds", DEFAULT_SEARCH_DURATION_SECONDS);
        double placeIntervalSeconds =
                trapSource.getDouble("block-place-interval-seconds", DEFAULT_BLOCK_PLACE_INTERVAL_SECONDS);
        double dismissSeconds = trapSource.getDouble("dismiss-delay-seconds", DEFAULT_DISMISS_DELAY_SECONDS);

        return new EndSettings(
                count,
                secondsToTicks(portalSeconds),
                range,
                radius,
                block,
                secondsToTicks(trapSeconds),
                secondsToTicks(searchSeconds),
                secondsToTicks(placeIntervalSeconds),
                DEFAULT_BLOCK_PLACE_WINDUP_TICKS,
                secondsToTicks(dismissSeconds));
    }

    public int endermanCount() {
        return endermanCount;
    }

    public long portalDurationTicks() {
        return portalDurationTicks;
    }

    public double targetRange() {
        return targetRange;
    }

    public double portalRadius() {
        return portalRadius;
    }

    public long spawnIntervalTicks() {
        return Math.max(1L, portalDurationTicks / endermanCount);
    }

    public Material trapBlock() {
        return trapBlock;
    }

    public long trapDurationTicks() {
        return trapDurationTicks;
    }

    public long searchDurationTicks() {
        return searchDurationTicks;
    }

    public long blockPlaceIntervalTicks() {
        return blockPlaceIntervalTicks;
    }

    public long blockPlaceWindupTicks() {
        return blockPlaceWindupTicks;
    }

    public long dismissDelayTicks() {
        return dismissDelayTicks;
    }

    private static Material resolveTrapBlock(String key) {
        return MaterialResolver.resolve(key, key);
    }

    private static long secondsToTicks(double seconds) {
        return Math.max(1L, Math.round(Math.max(0.05, seconds) * TICKS_PER_SECOND));
    }
}
