package com.monkey.ktplus.effects.list.warden;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ExpandingRing;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.entity.SupportedEntities;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class WardenKillEffect implements KillEffect {
    private final EffectDefinition definition;
    private final VisualEffectService visuals;

    public WardenKillEffect(EffectDefinition definition, VisualEffectService visuals) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
    }

    @Override
    public EffectDefinition definition() {
        return definition;
    }

    @Override
    public void execute(EffectContext context, EffectSession session) {
        Location loc = context.location();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        Player killer = context.killer();
        Location particleLoc = loc.clone().add(0, 3, 0);
        Location shriekLoc = loc.clone();
        Location soundLoc = loc.clone().add(0, 1, 0);

        visuals.sound("ENTITY_WARDEN_EMERGE", soundLoc, 2.0f, 1.0f);
        BuiltInDamageService.apply(session, killer, loc, context.config().effectDamage("warden"));

        final double radius = 1.5;
        final int points = 36;

        int[] finishedCount = {0};
        List<Location> impactLocations = new ArrayList<>();

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = Math.cos(angle);
            double z = Math.sin(angle);

            Location start = loc.clone().add(x * radius, 0.5, z * radius);
            Vector velocity = new Vector(x * 0.05, 0.10, z * 0.05);

            Location current = start.clone();
            Vector vel = velocity.clone();
            int[] age = {0};
            boolean[] taskCompleted = {false};

            session.runTimer(i % 4, 1L, () -> {
                if (taskCompleted[0]) {
                    return false;
                }
                if (age[0] > 40 || current.getY() <= loc.getY()) {
                    taskCompleted[0] = true;
                    finishedCount[0]++;
                    impactLocations.add(current.clone());

                    if (finishedCount[0] >= points) {
                        Location center = averageLocation(impactLocations);
                        if (center != null) {
                            spawnStaticSplashRing(visuals, center);
                            spawnExpandingRing(session, visuals, center, killer);
                        }
                    }
                    return false;
                }

                visuals.particle("SCULK_SOUL", current, 1, 0, 0, 0, 0, null);

                current.add(vel);
                vel.setY(vel.getY() - 0.015);
                age[0]++;
                return true;
            });
        }

        int[] ticks = {0};
        boolean[] shriekTaskCompleted = {false};

        session.runTimer(0L, 1L, () -> {
            if (shriekTaskCompleted[0]) {
                return false;
            }
            if (ticks[0] > 85) {
                shriekTaskCompleted[0] = true;
                return false;
            }

            if (ticks[0] % 2 == 0) {
                visuals.particle(
                        "SHRIEK",
                        shriekLoc,
                        ParticleScale.scale(150),
                        0,
                        0,
                        0,
                        1.5,
                        Color.NAVY);
            }

            if (ticks[0] >= 15 && (ticks[0] - 15) % 8 == 0) {
                visuals.particle("SONIC_BOOM", particleLoc, 1, 0, 0, 0, 0.5, Color.NAVY);
            }

            ticks[0]++;
            return true;
        });
    }

    private static Location averageLocation(List<Location> locations) {
        if (locations.isEmpty()) {
            return null;
        }

        double x = 0;
        double y = 0;
        double z = 0;
        World world = locations.get(0).getWorld();

        for (Location location : locations) {
            x += location.getX();
            y += location.getY();
            z += location.getZ();
        }

        return new Location(world, x / locations.size(), y / locations.size(), z / locations.size());
    }

    private static void spawnStaticSplashRing(VisualEffectService visuals, Location center) {
        int ringPoints = ParticleScale.scale(48);
        double splashRadius = 2.2;

        for (int j = 0; j < ringPoints; j++) {
            double angle = 2 * Math.PI * j / ringPoints;
            double rx = Math.cos(angle) * splashRadius;
            double rz = Math.sin(angle) * splashRadius;

            Location ringLoc = center.clone().add(rx, 0.2, rz);
            visuals.particle("FLAME", ringLoc, 1, 0, 0.01, 0, 0, Color.NAVY);
        }
    }

    private static void spawnExpandingRing(
            EffectSession session, VisualEffectService visuals, Location center, Player killer) {
        String entityType = visuals.entity("WARDEN");
        if (!SupportedEntities.isPresent(entityType)) {
            ExpandingRing.play(session, visuals, "SOUL", "ELECTRIC_SPARK", center, Color.NAVY, 35, 0.12, 0.02, 2L);
            return;
        }

        int ringPoints = ParticleScale.scale(64);
        double splashRadius = 2.2;
        double maxRadius = 6.0;
        int steps = 35;
        double radiusIncrement = (maxRadius - splashRadius) / steps;

        List<Vector> directions = Arrays.asList(
                new Vector(-1, 0, -1).normalize(),
                new Vector(1, 0, -1).normalize(),
                new Vector(-1, 0, 1).normalize(),
                new Vector(1, 0, 1).normalize());

        List<Entity> wardens = new ArrayList<>();
        Map<Entity, Vector> wardenDirections = new HashMap<>();

        for (Vector dir : directions) {
            Entity warden = session.spawnEntity(
                    entityType,
                    center.clone(),
                    spawned -> {
                        EntityCompat.trySetAware(spawned, false);
                        EntityCompat.trySetPersistent(spawned, false);
                        if (spawned instanceof LivingEntity living) {
                            EntityCompat.trySetCanPickupItems(living, false);
                            EntityCompat.trySetRemoveWhenFarAway(living, true);
                            EntityCompat.trySetCollidable(living, false);
                            EntityCompat.trySetGravity(living, false);
                        }
                    },
                    steps * 2L + 20L);
            if (warden == null) {
                continue;
            }
            wardens.add(warden);
            wardenDirections.put(warden, dir.clone());
        }

        if (wardens.isEmpty()) {
            ExpandingRing.play(session, visuals, "SOUL", "ELECTRIC_SPARK", center, Color.NAVY, 35, 0.12, 0.02, 2L);
            return;
        }

        int[] currentStep = {0};

        session.runTimer(0L, 2L, () -> {
            if (currentStep[0] > steps) {
                return false;
            }

            double currentRadius = splashRadius + currentStep[0] * radiusIncrement;

            for (int j = 0; j < ringPoints; j++) {
                double angle = 2 * Math.PI * j / ringPoints;
                double rx = Math.cos(angle) * currentRadius;
                double rz = Math.sin(angle) * currentRadius;

                Location ringLoc = center.clone().add(rx, 0.2, rz);
                visuals.particle("SOUL", ringLoc, 1, 0, 0.01, 0, 0, Color.NAVY);
            }

            for (Entity warden : wardens) {
                if (warden.isDead()) {
                    continue;
                }

                Vector dir = wardenDirections.get(warden);
                if (dir == null) {
                    continue;
                }

                Location newLoc = center.clone().add(dir.clone().multiply(currentRadius));
                newLoc.setY(center.getY());

                orientLocationTowardsPlayer(newLoc, killer);
                warden.teleport(newLoc);

                drawParticleLine(
                        visuals,
                        newLoc,
                        center.clone().add(0, 3, 0),
                        "ELECTRIC_SPARK",
                        0.15);
            }

            currentStep[0]++;
            return currentStep[0] <= steps;
        });
    }

    private static void orientLocationTowardsPlayer(Location from, Player player) {
        Location to = player.getEyeLocation();

        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));

        from.setYaw(yaw);
        from.setPitch(pitch);
    }

    private static void drawParticleLine(
            VisualEffectService visuals, Location start, Location end, String particle, double step) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        direction.normalize();

        for (double i = 0; i <= length; i += step) {
            Location point = start.clone().add(direction.clone().multiply(i));

            double offset = Math.sin(i * 10 + System.currentTimeMillis() * 0.01) * 0.05;
            Vector rotated = rotateAroundY(direction.clone(), Math.PI / 2);
            point.add(rotated.multiply(offset));

            visuals.particle(particle, point, 1, 0, 0, 0, 0.01, Color.NAVY);
        }
    }

    private static Vector rotateAroundY(Vector vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }
}
