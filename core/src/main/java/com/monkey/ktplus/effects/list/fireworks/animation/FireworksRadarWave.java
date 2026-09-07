package com.monkey.ktplus.effects.list.fireworks.animation;

import com.monkey.ktplus.effects.list.fireworks.animation.util.FireworksGround;
import com.monkey.ktplus.effects.list.fireworks.animation.util.FireworksRingMath;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.world.SensitiveBlocks;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class FireworksRadarWave {
    private static final long BLOCK_RESTORE_DELAY = 1200L;
    private static final double RADAR_PARTICLE_HEIGHT = 0.45;

    private FireworksRadarWave() {}

    public static void start(
            EffectSession session,
            FireworksScheduler scheduler,
            VisualEffectService visuals,
            Player killer,
            Location groundCenter,
            FireworksSettings settings,
            boolean allowStructure,
            FireworksMarkedTracker tracker,
            Runnable onRadarComplete) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(groundCenter, "groundCenter");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(tracker, "tracker");
        Objects.requireNonNull(onRadarComplete, "onRadarComplete");

        World world = groundCenter.getWorld();
        if (world == null) {
            return;
        }

        Material glowstone = MaterialResolver.resolve(visuals.material("GLOWSTONE"), "GLOWSTONE");
        double centerX = groundCenter.getX();
        double centerZ = groundCenter.getZ();
        Set<Location> waveBlocks = new HashSet<>();
        double[] radius = {0.0};

        tracker.startFootTracking(scheduler, killer, glowstone, allowStructure, settings.footUpdateTicks());
        visuals.sound("ENTITY_FIREWORK_ROCKET_LAUNCH", groundCenter, 1.0f, 1.2f);

        session.runTimer(0L, settings.radarPeriodTicks(), () -> {
            radius[0] += settings.radarSpeed();
            spawnRingParticles(visuals, world, groundCenter, radius[0]);

            if (allowStructure) {
                updateWaveBlocks(
                        session,
                        killer,
                        world,
                        centerX,
                        centerZ,
                        radius[0],
                        settings.radarSpeed(),
                        glowstone,
                        waveBlocks,
                        tracker);
            }

            markPlayersInRange(
                    scheduler, killer, world, groundCenter, radius[0], glowstone, allowStructure, tracker);

            if (radius[0] >= settings.radarRange()) {
                restoreRemainingWave(session, waveBlocks, tracker);
                onRadarComplete.run();
                return false;
            }
            return true;
        });
    }

    private static void spawnRingParticles(
            VisualEffectService visuals, World world, Location center, double radius) {
        double centerX = center.getX();
        double centerZ = center.getZ();
        for (int i = 0; i < 32; i++) {
            double angle = (Math.PI * 2.0 * i) / 32.0;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            double y = FireworksGround.surfaceParticleY(world, x, z, RADAR_PARTICLE_HEIGHT);
            Location point = new Location(world, x, y, z);
            visuals.particle("FIREWORK", point, 8, 0.15, 0.05, 0.15, 0.001, null);
        }
    }

    private static void updateWaveBlocks(
            EffectSession session,
            Player killer,
            World world,
            double centerX,
            double centerZ,
            double radius,
            double speed,
            Material glowstone,
            Set<Location> waveBlocks,
            FireworksMarkedTracker tracker) {
        int bound = FireworksRingMath.boundingRadius(radius);
        int centerBlockX = (int) Math.floor(centerX);
        int centerBlockZ = (int) Math.floor(centerZ);

        for (int dx = -bound; dx <= bound; dx++) {
            for (int dz = -bound; dz <= bound; dz++) {
                int blockX = centerBlockX + dx;
                int blockZ = centerBlockZ + dz;
                double distance = FireworksRingMath.horizontalDistance(centerX, centerZ, blockX, blockZ);

                if (FireworksRingMath.shouldRestoreWave(distance, radius)) {
                    restoreWaveColumn(session, blockX, blockZ, waveBlocks, tracker);
                    continue;
                }

                if (!FireworksRingMath.isInExpandingRing(distance, radius, speed)) {
                    continue;
                }

                Block surface = FireworksGround.surfaceBlock(world, blockX, blockZ);
                if (surface == null) {
                    continue;
                }
                Location blockLoc = surface.getLocation();
                if (tracker.isPinned(blockLoc) || waveBlocks.contains(blockLoc)) {
                    continue;
                }
                if (!session.allowsWorldMutation(killer, blockLoc) || SensitiveBlocks.isSensitive(surface)) {
                    continue;
                }
                session.temporaryBlock(killer, surface, glowstone, BLOCK_RESTORE_DELAY);
                waveBlocks.add(blockLoc.clone());
            }
        }
    }

    private static void restoreWaveColumn(
            EffectSession session,
            int blockX,
            int blockZ,
            Set<Location> waveBlocks,
            FireworksMarkedTracker tracker) {
        for (Location loc : new HashSet<>(waveBlocks)) {
            if (loc.getBlockX() != blockX || loc.getBlockZ() != blockZ) {
                continue;
            }
            if (tracker.isPinned(loc)) {
                continue;
            }
            session.restoreTemporaryBlock(loc.getBlock());
            waveBlocks.remove(loc);
        }
    }

    private static void restoreRemainingWave(
            EffectSession session, Set<Location> waveBlocks, FireworksMarkedTracker tracker) {
        for (Location loc : new HashSet<>(waveBlocks)) {
            if (tracker.isPinned(loc)) {
                continue;
            }
            session.restoreTemporaryBlock(loc.getBlock());
            waveBlocks.remove(loc);
        }
    }

    private static void markPlayersInRange(
            FireworksScheduler scheduler,
            Player killer,
            World world,
            Location center,
            double radius,
            Material glowstone,
            boolean allowStructure,
            FireworksMarkedTracker tracker) {
        for (Player player : world.getPlayers()) {
            if (player.equals(killer)) {
                continue;
            }
            if (tracker.isMarked(player.getUniqueId())) {
                continue;
            }
            if (horizontalDistance(player, center) > radius) {
                continue;
            }
            tracker.mark(scheduler, killer, player, glowstone, allowStructure);
        }
    }

    private static double horizontalDistance(Player player, Location center) {
        double dx = player.getLocation().getX() - center.getX();
        double dz = player.getLocation().getZ() - center.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
