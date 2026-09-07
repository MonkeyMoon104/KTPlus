package com.monkey.ktplus.effects.list.abyssgate.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.abyssgate.animation.util.AbyssGateDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class AbyssGateAnimation {
    private static final double DEFAULT_HEIGHT = 6.0;
    private static final int DEFAULT_TENTACLES = 8;
    private static final double DEFAULT_PULL = 0.2;
    private static final int DEFAULT_DURATION = 180;
    private static final int SEGMENTS = 5;
    private static final String[] SOUNDS = {
        "BLOCK_PORTAL_AMBIENT",
        "BLOCK_RESPAWN_ANCHOR_AMBIENT",
        "ENTITY_ENDERMAN_STARE",
        "ENTITY_ENDERMAN_SCREAM",
        "BLOCK_SCULK_SHRIEKER_SHRIEK",
        "ENTITY_WARDEN_HEARTBEAT"
    };

    private static final Color VOID = Color.fromRGB(20, 5, 30);
    private static final Color ABYSS = Color.fromRGB(60, 20, 90);
    private static final Color EDGE = Color.fromRGB(140, 60, 200);
    private static final Color PULL = Color.fromRGB(90, 40, 160);

    private AbyssGateAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("abyssgate");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double gateHeight = perks == null
                ? DEFAULT_HEIGHT
                : Math.max(3.0, perks.getDouble("gate-height", DEFAULT_HEIGHT));
        int tentacleCount = perks == null
                ? DEFAULT_TENTACLES
                : Math.max(1, perks.getInt("tentacle-count", DEFAULT_TENTACLES));
        double pull = perks == null
                ? DEFAULT_PULL
                : Math.max(0.0, perks.getDouble("pull", DEFAULT_PULL));
        int duration = perks == null
                ? DEFAULT_DURATION
                : Math.max(1, perks.getInt("duration", DEFAULT_DURATION));

        EffectDamageConfig damageCfg = context.config().effectDamage("abyssgate");
        double damage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 4.0;
        double damageRadius = damageCfg.enabled() ? Math.max(3.0, damageCfg.radius()) : 6.0;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        List<BlockDisplay> pillars = new ArrayList<>();
        List<BlockDisplay> frames = new ArrayList<>();
        
        for (int side = -1; side <= 1; side += 2) {
            for (int h = 0; h < (int) gateHeight; h++) {
                Location at = origin.clone().add(side * 1.4, h + 0.5, 0);
                BlockDisplay pillar = AbyssGateDisplays.spawnPillar(at);
                if (pillar != null) {
                    session.trackEntity(pillar);
                    pillars.add(pillar);
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            Location top = origin.clone().add(x * 0.7, gateHeight + 0.2, 0);
            BlockDisplay frame = AbyssGateDisplays.spawnFrame(top);
            if (frame != null) {
                session.trackEntity(frame);
                frames.add(frame);
            }
            Location mid = origin.clone().add(x * 0.5, gateHeight * 0.5, 0.05);
            BlockDisplay midFrame = AbyssGateDisplays.spawnFrame(mid);
            if (midFrame != null) {
                session.trackEntity(midFrame);
                frames.add(midFrame);
            }
        }

        List<Tentacle> tentacles = new ArrayList<>(tentacleCount);
        for (int i = 0; i < tentacleCount; i++) {
            double baseAngle = (Math.PI * 2.0 * i) / tentacleCount;
            List<BlockDisplay> segs = new ArrayList<>(SEGMENTS);
            for (int s = 0; s < SEGMENTS; s++) {
                Location spawn = origin.clone().add(Math.cos(baseAngle) * (0.5 + s * 0.35), 0.4 + s * 0.2, Math.sin(baseAngle) * (0.5 + s * 0.35));
                BlockDisplay seg = AbyssGateDisplays.spawnTentacle(spawn, s + i);
                if (seg != null) {
                    session.trackEntity(seg);
                    segs.add(seg);
                }
            }
            tentacles.add(new Tentacle(segs, baseAngle, i));
        }

        AtomicInteger tick = new AtomicInteger();
        Set<UUID> slapCooldown = new HashSet<>();
        boolean[] finished = {false};
        int openTicks = 28;
        int activeTicks = duration;
        int endTicks = openTicks + activeTicks + 25;

        session.onCleanup(() -> {
            stopSounds(origin);
            clearAll(pillars, frames, tentacles);
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(endTicks + 40L);

        visuals.sound("BLOCK_RESPAWN_ANCHOR_AMBIENT", origin, 0.9f, 0.55f);
        visuals.sound("ENTITY_ENDERMAN_STARE", origin, 0.7f, 0.5f);

        double finalPull = pull;
        double finalDamage = damage;
        double finalRadius = damageRadius;
        double finalHeight = gateHeight;

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                clearAll(pillars, frames, tentacles);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= endTicks) {
                finish(visuals, origin, pillars, frames, tentacles, killer);
                finished[0] = true;
                return false;
            }

            double open = Math.min(1.0, (current + 1) / (double) openTicks);
            placeGate(pillars, frames, origin, finalHeight, open, current);
            drawGateVeil(visuals, origin, finalHeight, open, current);

            if (current >= openTicks) {
                updateTentacles(session, visuals, world, origin, tentacles, killer, victimId, current, finalRadius, finalDamage, slapCooldown);
                pullPlayers(session, world, origin, killer, victimId, finalPull, finalRadius + 2.0, finalHeight);
                if (current % 12 == 0) {
                    visuals.sound("BLOCK_PORTAL_AMBIENT", origin, 0.4f, 0.45f);
                }
                if (current % 28 == 0) {
                    visuals.sound("ENTITY_ENDERMAN_SCREAM", origin, 0.35f, 0.6f);
                }
                if (current % 40 == 0) {
                    visuals.sound("ENTITY_WARDEN_HEARTBEAT", origin, 0.4f, 0.7f);
                }
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&5⟁ ABYSSGATE &8| &d%s &8| &7%d",
                                current < openTicks ? "OPEN" : "PULL",
                                Math.max(0, openTicks + activeTicks - current)));
            }
            return true;
        });
    }

    private static void placeGate(
            List<BlockDisplay> pillars,
            List<BlockDisplay> frames,
            Location origin,
            double height,
            double open,
            int tick) {
        int idx = 0;
        for (int side = -1; side <= 1; side += 2) {
            for (int h = 0; h < (int) height; h++) {
                if (idx >= pillars.size()) {
                    break;
                }
                Location at = origin.clone().add(side * (1.1 + open * 0.35), h * open + 0.3, Math.sin(tick * 0.05 + h) * 0.05);
                AbyssGateDisplays.place(pillars.get(idx++), at, 0.5f, 1.05f, 0.5f, 0.0f, 0.0f);
            }
        }
        int f = 0;
        for (BlockDisplay frame : frames) {
            double y = f < 3 ? height * open + 0.15 : height * 0.45 * open;
            double x = ((f % 3) - 1) * 0.7;
            Location at = origin.clone().add(x, y, Math.sin(tick * 0.08 + f) * 0.08);
            AbyssGateDisplays.place(frame, at, 0.65f, 0.32f, 0.65f, tick * 0.02f, 0.0f);
            f++;
        }
    }

    private static void drawGateVeil(
            VisualEffectService visuals, Location origin, double height, double open, int tick) {
        int cols = ParticleScale.scale(6);
        int rows = ParticleScale.scale(8);
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                double x = (c / (double) (cols - 1) - 0.5) * 2.0 * open;
                double y = (r / (double) (rows - 1)) * height * open;
                Location p = origin.clone().add(x, y, Math.sin(tick * 0.2 + r * 0.4) * 0.15);
                Color color = (c + r + tick) % 3 == 0 ? EDGE : ((c + r) % 2 == 0 ? ABYSS : VOID);
                visuals.dust(p, color, 1.15f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        visuals.particle("PORTAL", origin.clone().add(0, height * 0.5 * open, 0), ParticleScale.scale(6), 0.35, height * 0.25, 0.1, 0.02, null);
        visuals.particle("REVERSE_PORTAL", origin.clone().add(0, 0.3, 0), ParticleScale.scale(4), 0.5, 0.2, 0.15, 0.0, null);
    }

    private static void updateTentacles(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            List<Tentacle> tentacles,
            Player killer,
            UUID victimId,
            int tick,
            double radius,
            double damage,
            Set<UUID> slapCooldown) {
        Player nearest = findNearest(world, origin, killer, victimId, radius + 4.0, session);
        for (Tentacle tentacle : tentacles) {
            double reach = 1.2 + Math.sin(tick * 0.12 + tentacle.index) * 0.4 + 1.4;
            Location tipTarget = nearest != null
                    ? nearest.getLocation().clone().add(0, 1.0, 0)
                    : origin.clone().add(Math.cos(tentacle.angle + tick * 0.04) * reach, 1.5, Math.sin(tentacle.angle + tick * 0.04) * reach);
            Location prev = origin.clone().add(0, 0.8, 0);
            for (int s = 0; s < tentacle.segments.size(); s++) {
                double t = (s + 1) / (double) tentacle.segments.size();
                Location at = lerp(prev, tipTarget, t * 0.85);
                at.add(
                        Math.cos(tick * 0.2 + s + tentacle.index) * 0.25,
                        Math.sin(tick * 0.18 + s) * 0.2,
                        Math.sin(tick * 0.2 + s + tentacle.index) * 0.25);
                float scale = 0.32f - s * 0.03f;
                AbyssGateDisplays.place(
                        tentacle.segments.get(s),
                        at,
                        scale,
                        scale,
                        scale,
                        (float) (tentacle.angle + tick * 0.05),
                        (float) Math.sin(tick * 0.15 + s) * 0.4f);
                prev = at;
                if (s == tentacle.segments.size() - 1) {
                    tentacle.tip = at;
                }
            }
            if (nearest != null
                    && tentacle.tip != null
                    && tentacle.tip.distanceSquared(nearest.getLocation().add(0, 1, 0)) < 1.8
                    && !slapCooldown.contains(nearest.getUniqueId())
                    && (killer == null || session.allowsWorldMutation(killer, nearest.getLocation()))) {
                slapCooldown.add(nearest.getUniqueId());
                if (killer != null) {
                    nearest.damage(damage * 0.5, killer);
                } else {
                    nearest.damage(damage * 0.5);
                }
                visuals.dust(tentacle.tip, EDGE, 1.3f, 5, 0.15, 0.15, 0.15, 0.0);
            }
        }
        if (tick % 16 == 0) {
            slapCooldown.clear();
        }
    }

    private static void pullPlayers(
            EffectSession session,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double pull,
            double radius,
            double height) {
        Location center = origin.clone().add(0, height * 0.45, 0);
        double radiusSq = radius * radius;
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            Vector to = center.toVector().subtract(player.getLocation().toVector());
            if (to.lengthSquared() < 0.01) {
                continue;
            }
            player.setVelocity(player.getVelocity().add(to.normalize().multiply(pull)));
        }
    }

    private static Location lerp(Location a, Location b, double t) {
        return a.clone().add(b.toVector().subtract(a.toVector()).multiply(t));
    }

    private static Player findNearest(
            World world, Location origin, Player killer, UUID victimId, double radius, EffectSession session) {
        double radiusSq = radius * radius;
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            double d = player.getLocation().distanceSquared(origin);
            if (d > radiusSq || d >= bestDist) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            best = player;
            bestDist = d;
        }
        return best;
    }

    private static void finish(
            VisualEffectService visuals,
            Location origin,
            List<BlockDisplay> pillars,
            List<BlockDisplay> frames,
            List<Tentacle> tentacles,
            Player killer) {
        visuals.dust(origin.clone().add(0, 2, 0), VOID, 1.6f, ParticleScale.scale(20), 0.8, 1.2, 0.4, 0.0);
        visuals.sound("BLOCK_SCULK_SHRIEKER_SHRIEK", origin, 0.55f, 0.7f);
        stopSounds(origin);
        clearAll(pillars, frames, tentacles);
        PerkActionBar.clear(killer);
    }

    private static void clearAll(List<BlockDisplay> pillars, List<BlockDisplay> frames, List<Tentacle> tentacles) {
        for (BlockDisplay d : pillars) {
            AbyssGateDisplays.remove(d);
        }
        pillars.clear();
        for (BlockDisplay d : frames) {
            AbyssGateDisplays.remove(d);
        }
        frames.clear();
        for (Tentacle t : tentacles) {
            for (BlockDisplay seg : t.segments) {
                AbyssGateDisplays.remove(seg);
            }
            t.segments.clear();
        }
        tentacles.clear();
    }

    private static void stopSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 72.0 * 72.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static final class Tentacle {
        private final List<BlockDisplay> segments;
        private final double angle;
        private final int index;
        private Location tip;

        private Tentacle(List<BlockDisplay> segments, double angle, int index) {
            this.segments = segments;
            this.angle = angle;
            this.index = index;
        }
    }
}
