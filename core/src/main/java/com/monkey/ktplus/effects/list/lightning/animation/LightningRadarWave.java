package com.monkey.ktplus.effects.list.lightning.animation;

import com.monkey.ktplus.effects.list.lightning.LightningSettings;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class LightningRadarWave {
    private static final int RING_POINTS = 36;
    private static final double PARTICLE_HEIGHT = 0.35;

    private LightningRadarWave() {}

    public static void start(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Location center,
            LightningSettings settings) {
        start(session, visuals, killer, center, settings, new HashSet<>(), 0);
    }

    private static void start(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Location center,
            LightningSettings settings,
            Set<UUID> chainOrigins,
            int depth) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(chainOrigins, "chainOrigins");

        World world = center.getWorld();
        if (world == null) {
            return;
        }

        Location origin = center.clone();
        Set<UUID> struckThisWave = new HashSet<>();
        double[] radius = {0.0};

        session.runTimer(0L, settings.radarPeriodTicks(), () -> {
            if (!session.active() || !killer.isOnline()) {
                return false;
            }

            radius[0] += settings.radarSpeed();
            spawnRingParticles(visuals, world, origin, radius[0]);
            strikePlayersInRange(
                    session,
                    visuals,
                    killer,
                    world,
                    origin,
                    radius[0],
                    settings,
                    struckThisWave,
                    chainOrigins,
                    depth);

            return radius[0] < settings.radarRange();
        });
    }

    private static void spawnRingParticles(
            VisualEffectService visuals, World world, Location center, double radius) {
        double centerX = center.getX();
        double centerZ = center.getZ();
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = (Math.PI * 2.0 * i) / RING_POINTS;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            double y = particleY(world, x, z, center.getY());
            Location point = new Location(world, x, y, z);
            visuals.particle("ELECTRIC_SPARK", point, 4, 0.12, 0.08, 0.12, 0.01, null);
            visuals.particle("ENCHANTED_HIT", point, 2, 0.08, 0.05, 0.08, 0.0, null);
            if (i % 3 == 0) {
                visuals.particle("CRIT", point, 1, 0.05, 0.1, 0.05, 0.02, null);
            }
        }
    }

    private static void strikePlayersInRange(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            World world,
            Location center,
            double radius,
            LightningSettings settings,
            Set<UUID> struckThisWave,
            Set<UUID> chainOrigins,
            int depth) {
        for (Player player : world.getPlayers()) {
            if (player.equals(killer) || !player.isValid() || player.isDead()) {
                continue;
            }
            UUID playerId = player.getUniqueId();
            if (struckThisWave.contains(playerId)) {
                continue;
            }
            if (horizontalDistance(player, center) > radius) {
                continue;
            }

            Location strikeLoc = player.getLocation();
            if (!session.allowsWorldMutation(killer, strikeLoc)) {
                continue;
            }

            struckThisWave.add(playerId);
            strikePlayer(visuals, killer, player, settings);

            if (!settings.chain() || depth >= settings.maxChainDepth()) {
                continue;
            }
            if (!chainOrigins.add(playerId)) {
                continue;
            }
            start(session, visuals, killer, strikeLoc, settings, chainOrigins, depth + 1);
        }
    }

    private static void strikePlayer(
            VisualEffectService visuals, Player killer, Player target, LightningSettings settings) {
        Location loc = target.getLocation();
        World world = loc.getWorld();
        if (world != null) {
            world.strikeLightningEffect(loc);
        }
        visuals.sound("ENTITY_LIGHTNING_BOLT_THUNDER", loc, 1.6f, 1.05f);
        visuals.particle("ELECTRIC_SPARK", loc.clone().add(0, 1.0, 0), 28, 0.35, 0.8, 0.35, 0.05, null);
        visuals.particle("CRIT", loc.clone().add(0, 1.0, 0), 12, 0.25, 0.6, 0.25, 0.08, null);
        if (settings.damageEnabled() && settings.damageValue() > 0.0) {
            target.damage(settings.damageValue(), killer);
        }
    }

    private static double particleY(World world, double x, double z, double fallbackY) {
        int surfaceY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
        if (surfaceY <= world.getMinHeight()) {
            return fallbackY + PARTICLE_HEIGHT;
        }
        return surfaceY + 1.0 + PARTICLE_HEIGHT;
    }

    private static double horizontalDistance(Player player, Location center) {
        double dx = player.getLocation().getX() - center.getX();
        double dz = player.getLocation().getZ() - center.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
