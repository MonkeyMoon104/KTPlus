package com.monkey.ktplus.effects.list.bloodmoon.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.bloodmoon.animation.util.BloodMoonDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class BloodMoonAnimation {
    private static final int MAX_TICKS = 200;
    private static final int DEFAULT_RAYS = 6;
    private static final int DEFAULT_INTERVAL = 18;
    private static final double DEFAULT_RAY_DAMAGE = 4.0;
    private static final int DEFAULT_DURATION = 160;
    private static final int RAY_SEGMENTS = 7;
    private static final double MOON_HEIGHT = 14.0;

    private static final Color BLOOD = Color.fromRGB(180, 20, 30);
    private static final Color CRIMSON = Color.fromRGB(220, 50, 55);
    private static final Color SHADOW = Color.fromRGB(60, 10, 15);

    private static final String[] AMBIENT_SOUNDS = {
        "BLOCK_BEACON_AMBIENT",
        "ENTITY_WARDEN_HEARTBEAT",
        "BLOCK_RESPAWN_ANCHOR_AMBIENT",
        "ENTITY_WITHER_AMBIENT",
        "BLOCK_PORTAL_AMBIENT"
    };

    private BloodMoonAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("bloodmoon");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int rayCount = perks == null
                ? DEFAULT_RAYS
                : Math.max(3, Math.min(10, perks.getInt("ray-count", DEFAULT_RAYS)));
        int intervalTicks = perks == null
                ? DEFAULT_INTERVAL
                : Math.max(8, perks.getInt("interval-ticks", DEFAULT_INTERVAL));
        double rayDamageBase = perks == null
                ? DEFAULT_RAY_DAMAGE
                : Math.max(0.0, perks.getDouble("ray-damage", DEFAULT_RAY_DAMAGE));
        int duration = perks == null
                ? DEFAULT_DURATION
                : Math.max(60, perks.getInt("duration", DEFAULT_DURATION));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("bloodmoon");
        final double rayDamage = damageCfg.enabled()
                ? Math.max(rayDamageBase, damageCfg.value())
                : rayDamageBase;

        Location moonAt = origin.clone().add(0.0, MOON_HEIGHT, 0.0);
        ItemDisplay moon = BloodMoonDisplays.spawn(moonAt, BloodMoonDisplays.moonMaterial(), 2.4f);
        if (moon != null) {
            session.trackEntity(moon);
        }

        List<ItemDisplay> allRayPieces = new ArrayList<>();
        List<Ray> rays = new ArrayList<>(rayCount);
        for (int i = 0; i < rayCount; i++) {
            List<ItemDisplay> segments = new ArrayList<>(RAY_SEGMENTS);
            for (int s = 0; s < RAY_SEGMENTS; s++) {
                ItemDisplay seg = BloodMoonDisplays.spawn(moonAt, BloodMoonDisplays.rayMaterial(s + i), 0.25f);
                if (seg == null) {
                    continue;
                }
                session.trackEntity(seg);
                segments.add(seg);
                allRayPieces.add(seg);
            }
            double angle = (Math.PI * 2.0 * i) / rayCount;
            rays.add(new Ray(segments, angle, i * (intervalTicks / Math.max(1, rayCount))));
        }

        session.onCleanup(() -> {
            stopAmbient(origin);
            BloodMoonDisplays.remove(moon);
            BloodMoonDisplays.removeAll(allRayPieces);
        });

        visuals.sound("BLOCK_BEACON_ACTIVATE", moonAt, 0.9f, 0.55f);
        visuals.sound("ENTITY_WITHER_SPAWN", origin, 0.35f, 1.6f);
        session.resetDeadline(Math.max(MAX_TICKS, duration) + 30L);

        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        Set<UUID> hitCooldown = new HashSet<>();
        double finalDamage = rayDamage;

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopAmbient(origin);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= duration) {
                finish(visuals, origin, moon, allRayPieces);
                finished[0] = true;
                return false;
            }

            float moonPulse = 2.2f + (float) Math.sin(current * 0.08) * 0.25f;
            BloodMoonDisplays.place(moon, moonAt, current * 0.02f, 0.15f, current * 0.01f, moonPulse);
            drawMoonAura(visuals, moonAt, current);

            if (current % 10 == 0) {
                visuals.sound("BLOCK_BEACON_AMBIENT", moonAt, 0.35f, 0.55f);
            }
            if (current % 16 == 0) {
                visuals.sound("ENTITY_WARDEN_HEARTBEAT", origin, 0.4f, 0.7f);
            }

            for (Ray ray : rays) {
                updateRay(
                        session,
                        visuals,
                        world,
                        killer,
                        victimId,
                        origin,
                        moonAt,
                        ray,
                        intervalTicks,
                        finalDamage,
                        hitCooldown,
                        current);
            }
            return true;
        });
    }

    private static void drawMoonAura(VisualEffectService visuals, Location moonAt, int tick) {
        int points = ParticleScale.scale(10);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0 * i) / points + tick * 0.05;
            double r = 1.4 + Math.sin(tick * 0.1 + i) * 0.25;
            Location p = moonAt.clone().add(Math.cos(a) * r, Math.sin(a * 2) * 0.4, Math.sin(a) * r);
            visuals.dust(p, i % 2 == 0 ? BLOOD : CRIMSON, 1.4f, 1, 0, 0, 0, 0);
        }
        if (tick % 2 == 0) {
            visuals.particle("CLOUD", moonAt, 3, 0.5, 0.35, 0.5, 0.0, null);
            visuals.dust(moonAt, SHADOW, 1.8f, 4, 0.6, 0.4, 0.6, 0.0);
        }
    }

    private static void updateRay(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location origin,
            Location moonAt,
            Ray ray,
            int interval,
            double damage,
            Set<UUID> hitCooldown,
            int current) {
        int local = current - ray.phaseOffset;
        if (local < 0) {
            hideRay(ray);
            return;
        }
        int cycle = local % interval;
        boolean firing = cycle < Math.max(6, interval / 3);

        if (!firing) {
            hideRay(ray);
            return;
        }

        if (cycle == 0) {
            ray.targetAngle = ray.baseAngle + (Math.random() - 0.5) * 1.2;
            double dist = 3.0 + Math.random() * 7.0;
            double tx = origin.getX() + Math.cos(ray.targetAngle) * dist;
            double tz = origin.getZ() + Math.sin(ray.targetAngle) * dist;
            double gy = findGroundY(world, tx, origin.getY(), tz);
            ray.impact = new Location(world, tx, gy + 0.1, tz);
            visuals.sound("BLOCK_RESPAWN_ANCHOR_DEPLETE", ray.impact, 0.7f, 0.8f);
            visuals.sound("ENTITY_LIGHTNING_BOLT_IMPACT", ray.impact, 0.35f, 1.4f);
        }

        if (ray.impact == null) {
            return;
        }

        int n = ray.segments.size();
        for (int i = 0; i < n; i++) {
            double t = (i + 0.5) / n;
            Location at = lerp(moonAt, ray.impact, t);
            float wobble = (float) Math.sin(current * 0.4 + i) * 0.08f;
            at.add(wobble, 0, wobble);
            BloodMoonDisplays.placeStretch(ray.segments.get(i), at, (float) ray.targetAngle, 0.9f, 0.28f, 0.9f);
            if (i % 2 == 0) {
                visuals.dust(at, BLOOD, 1.1f, 1, 0, 0, 0, 0);
            }
        }

        if (cycle == 2 || cycle == 4) {
            impactDamage(session, visuals, world, killer, victimId, ray.impact, damage, hitCooldown);
            visuals.dust(ray.impact, CRIMSON, 1.7f, ParticleScale.scale(12), 0.4, 0.15, 0.4, 0.0);
            visuals.particle("CLOUD", ray.impact.clone().add(0, 0.3, 0), ParticleScale.scale(8), 0.35, 0.2, 0.35, 0.02, null);
        }
    }

    private static void hideRay(Ray ray) {
        for (ItemDisplay seg : ray.segments) {
            if (seg != null && seg.isValid() && !seg.isDead()) {
                Location far = seg.getLocation().clone().add(0, -40, 0);
                BloodMoonDisplays.placeStretch(seg, far, 0, 0, 0.01f, 0.01f);
            }
        }
    }

    private static void impactDamage(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location impact,
            double damage,
            Set<UUID> hitCooldown) {
        if (killer == null || damage <= 0.0) {
            return;
        }
        double radiusSq = 2.6 * 2.6;
        for (Player player : world.getPlayers()) {
            if (player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            if (hitCooldown.contains(player.getUniqueId())) {
                continue;
            }
            if (player.getLocation().distanceSquared(impact) > radiusSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            hitCooldown.add(player.getUniqueId());
            UUID id = player.getUniqueId();
            session.runLater(12L, () -> hitCooldown.remove(id));
            player.damage(damage, killer);
            Vector away = player.getLocation().toVector().subtract(impact.toVector());
            away.setY(0.0);
            if (away.lengthSquared() < 1.0e-4) {
                away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            away.normalize().multiply(0.45).setY(0.35);
            player.setVelocity(player.getVelocity().add(away));
            visuals.dust(player.getLocation().add(0, 1, 0), BLOOD, 1.4f, 8, 0.2, 0.25, 0.2, 0.0);
        }
    }

    private static Location lerp(Location a, Location b, double t) {
        return a.clone().add(
                (b.getX() - a.getX()) * t,
                (b.getY() - a.getY()) * t,
                (b.getZ() - a.getZ()) * t);
    }

    private static double findGroundY(World world, double x, double aroundY, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int start = Math.min(world.getMaxHeight() - 2, (int) Math.floor(aroundY) + 4);
        int minY = Math.max(world.getMinHeight(), (int) Math.floor(aroundY) - 8);
        for (int y = start; y >= minY; y--) {
            Block block = world.getBlockAt(bx, y, bz);
            if (block.getType().isSolid()) {
                return y + 1.0;
            }
        }
        return aroundY;
    }

    private static void finish(
            VisualEffectService visuals, Location origin, ItemDisplay moon, List<ItemDisplay> rays) {
        stopAmbient(origin);
        visuals.dust(origin.clone().add(0, 2, 0), BLOOD, 1.8f, ParticleScale.scale(24), 1.2, 1.0, 1.2, 0.0);
        visuals.particle("CLOUD", origin.clone().add(0, 1.5, 0), ParticleScale.scale(18), 0.9, 0.7, 0.9, 0.02, null);
        visuals.sound("ENTITY_WITHER_DEATH", origin, 0.4f, 1.5f);
        BloodMoonDisplays.remove(moon);
        BloodMoonDisplays.removeAll(rays);
    }

    private static void stopAmbient(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 80.0 * 80.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : AMBIENT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static final class Ray {
        private final List<ItemDisplay> segments;
        private final double baseAngle;
        private final int phaseOffset;
        private double targetAngle;
        private Location impact;

        private Ray(List<ItemDisplay> segments, double baseAngle, int phaseOffset) {
            this.segments = segments;
            this.baseAngle = baseAngle;
            this.phaseOffset = phaseOffset;
            this.targetAngle = baseAngle;
        }
    }
}
