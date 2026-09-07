package com.monkey.ktplus.effects.list.clockrewind.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.clockrewind.animation.util.ClockRewindDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class ClockRewindAnimation {
    private static final int DEFAULT_REWIND_TICKS = 35;
    private static final int DEFAULT_FREEZE_TICKS = 15;
    private static final double DEFAULT_RADIUS = 7.0;
    private static final double DEFAULT_SNAP_DAMAGE = 4.0;
    private static final int DEFAULT_TICK_RATE = 4;
    private static final int PATH_SAMPLES = 10;

    private static final Color GOLD = Color.fromRGB(255, 200, 60);
    private static final Color AMBER = Color.fromRGB(220, 140, 40);
    private static final Color CYAN = Color.fromRGB(80, 220, 230);
    private static final Color DEEP = Color.fromRGB(40, 90, 140);

    private ClockRewindAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location center = context.location().clone().add(0.5, 1.6, 0.5);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("clockrewind");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int rewindTicks = perks == null
                ? DEFAULT_REWIND_TICKS
                : Math.max(10, perks.getInt("rewind-ticks", DEFAULT_REWIND_TICKS));
        int freezeTicks = perks == null
                ? DEFAULT_FREEZE_TICKS
                : Math.max(5, perks.getInt("freeze-ticks", DEFAULT_FREEZE_TICKS));
        double radius = perks == null ? DEFAULT_RADIUS : Math.max(2.0, perks.getDouble("radius", DEFAULT_RADIUS));
        double snapDamage = perks == null
                ? DEFAULT_SNAP_DAMAGE
                : Math.max(0.0, perks.getDouble("damage-on-snap", DEFAULT_SNAP_DAMAGE));
        int tickRate = perks == null
                ? DEFAULT_TICK_RATE
                : Math.max(2, perks.getInt("tick-rate", DEFAULT_TICK_RATE));

        EffectDamageConfig damageCfg = context.config().effectDamage("clockrewind");
        if (damageCfg.enabled()) {
            snapDamage = Math.max(snapDamage, damageCfg.value());
            radius = Math.max(radius, damageCfg.radius());
        }

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        ItemDisplay clock = ClockRewindDisplays.spawnClock(center);
        if (clock == null) {
            return;
        }
        session.trackEntity(clock);

        Map<UUID, Deque<Location>> paths = new HashMap<>();
        Map<UUID, Vector> frozenVel = new HashMap<>();
        Set<UUID> snapped = new HashSet<>();
        AtomicInteger tick = new AtomicInteger();
        int total = freezeTicks + rewindTicks + 25;
        float[] yaw = {0.0f};

        session.onCleanup(() -> {
            ClockRewindDisplays.remove(clock);
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_NOTE_BLOCK_CHIME", center, 1.0f, 0.7f);
        visuals.sound("BLOCK_BEACON_AMBIENT", center, 0.55f, 1.4f);
        session.resetDeadline(total + 20L);

        double finalRadius = radius;
        double finalSnap = snapDamage;

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                ClockRewindDisplays.remove(clock);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                ClockRewindDisplays.remove(clock);
                PerkActionBar.clear(killer);
                return false;
            }

            yaw[0] -= 0.22f; 
            float scale = 2.4f + (float) Math.sin(current * 0.15) * 0.2f;
            float pitch = (float) Math.sin(current * 0.08) * 0.25f;
            ClockRewindDisplays.place(clock, center, yaw[0], pitch, scale);
            drawClockAura(visuals, center, current, finalRadius);

            if (current % tickRate == 0) {
                float pitchSound = 0.85f + (current % 8) * 0.04f;
                visuals.sound("BLOCK_NOTE_BLOCK_HAT", center, 0.55f, pitchSound);
                visuals.sound("BLOCK_NOTE_BLOCK_PLING", center, 0.25f, 1.6f - current * 0.004f);
            }

            List<Player> nearby = findNearby(world, center, killer, victimId, finalRadius, session);

            if (current < freezeTicks) {
                for (Player player : nearby) {
                    UUID id = player.getUniqueId();
                    frozenVel.putIfAbsent(id, player.getVelocity().clone());
                    Deque<Location> path = paths.computeIfAbsent(id, k -> new ArrayDeque<>());
                    path.addLast(player.getLocation().clone());
                    while (path.size() > PATH_SAMPLES) {
                        path.removeFirst();
                    }
                    
                    player.setVelocity(new Vector(0, Math.min(0.02, player.getVelocity().getY()), 0));
                    Location at = player.getLocation().add(0, 1, 0);
                    visuals.dust(at, CYAN, 1.0f, 2, 0.1, 0.15, 0.1, 0.0);
                    visuals.particle("END_ROD", at, 1, 0.05, 0.1, 0.05, 0.0, null);
                }
                if (killer != null && killer.isOnline() && current % 3 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&e⏱ CLOCKREWIND &8| &bFREEZE &f%d%% &8| &7TARGETS &f%d",
                                    (int) ((current + 1) * 100.0 / freezeTicks),
                                    nearby.size()));
                }
            } else if (current < freezeTicks + rewindTicks) {
                int local = current - freezeTicks;
                double progress = (local + 1) / (double) rewindTicks;
                for (Player player : nearby) {
                    UUID id = player.getUniqueId();
                    Deque<Location> path = paths.get(id);
                    Vector base = frozenVel.getOrDefault(id, player.getVelocity());
                    
                    Vector rewind = base.clone().multiply(-1.15 - progress * 0.4);
                    Vector towardClock = center.toVector().subtract(player.getLocation().toVector());
                    if (towardClock.lengthSquared() > 0.01) {
                        towardClock.normalize().multiply(0.08 * progress);
                        rewind.add(towardClock);
                    }
                    rewind.setY(Math.max(-0.15, Math.min(0.55, rewind.getY() * -0.6 + 0.12)));
                    player.setVelocity(rewind);

                    drawReversePath(visuals, player, path, local);
                }
                if (killer != null && killer.isOnline() && current % 3 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&e⏱ CLOCKREWIND &8| &dREWIND &f%d%% &8| &7TARGETS &f%d",
                                    (int) (progress * 100),
                                    nearby.size()));
                }
            } else {
                
                for (Player player : nearby) {
                    if (!snapped.add(player.getUniqueId())) {
                        continue;
                    }
                    Location at = player.getLocation().clone().add(0, 1, 0);
                    visuals.sound("BLOCK_NOTE_BLOCK_BELL", at, 1.0f, 0.55f);
                    visuals.sound("ENTITY_PLAYER_ATTACK_CRIT", at, 0.8f, 0.7f);
                    visuals.particle("FLASH", at, 1, 0, 0, 0, 0, Color.WHITE);
                    visuals.dust(at, GOLD, 1.6f, 12, 0.35, 0.35, 0.35, 0.0);
                    if (finalSnap > 0.0) {
                        if (killer != null) {
                            player.damage(finalSnap, killer);
                        } else {
                            player.damage(finalSnap);
                        }
                    }
                    Vector away = player.getLocation().toVector().subtract(center.toVector());
                    away.setY(0);
                    if (away.lengthSquared() < 0.01) {
                        away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
                    }
                    away.normalize().multiply(1.1).setY(0.35);
                    player.setVelocity(away);
                }
                if (current == freezeTicks + rewindTicks) {
                    visuals.sound("BLOCK_BELL_USE", center, 1.2f, 0.8f);
                    visuals.sound("ENTITY_EXPERIENCE_ORB_PICKUP", center, 0.7f, 0.5f);
                }
            }
            return true;
        });
    }

    private static void drawClockAura(VisualEffectService visuals, Location center, int tick, double radius) {
        int marks = ParticleScale.scale(12);
        double spin = -tick * 0.12;
        for (int i = 0; i < marks; i++) {
            double a = spin + (Math.PI * 2.0 * i) / marks;
            double r = radius * 0.55;
            Location p = center.clone().add(Math.cos(a) * r, Math.sin(tick * 0.1 + i) * 0.15, Math.sin(a) * r);
            visuals.dust(p, i % 2 == 0 ? GOLD : AMBER, 1.05f, 1, 0.0, 0.0, 0.0, 0.0);
            if (i % 3 == 0) {
                visuals.particle("END_ROD", p, 1, 0.0, 0.0, 0.0, 0.0, null);
            }
        }
        
        double hand = -tick * 0.18;
        for (double t = 0.15; t < radius * 0.5; t += 0.35) {
            Location hour = center.clone().add(Math.cos(hand) * t, 0.05, Math.sin(hand) * t);
            Location minute = center.clone().add(Math.cos(hand * 2.2) * t * 1.15, -0.05, Math.sin(hand * 2.2) * t * 1.15);
            visuals.dust(hour, CYAN, 0.95f, 1, 0.0, 0.0, 0.0, 0.0);
            visuals.dust(minute, DEEP, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void drawReversePath(
            VisualEffectService visuals, Player player, Deque<Location> path, int local) {
        if (path == null || path.isEmpty()) {
            Location at = player.getLocation().add(0, 1, 0);
            visuals.dust(at, CYAN, 1.0f, 2, 0.08, 0.08, 0.08, 0.0);
            return;
        }
        List<Location> pts = new ArrayList<>(path);
        for (int i = 0; i < pts.size(); i++) {
            float t = pts.size() <= 1 ? 1.0f : i / (float) (pts.size() - 1);
            Location p = pts.get(i).clone().add(0, 0.9 + Math.sin(local * 0.3 + i) * 0.05, 0);
            visuals.dust(p, t > 0.5f ? CYAN : DEEP, 0.7f + t * 0.5f, 1, 0.0, 0.0, 0.0, 0.0);
            if (i % 2 == 0) {
                visuals.particle("ENCHANT", p, 1, 0.02, 0.02, 0.02, 0.0, null);
            }
        }
    }

    private static List<Player> findNearby(
            World world, Location origin, Player killer, UUID victimId, double radius, EffectSession session) {
        double radiusSq = radius * radius;
        List<Player> found = new ArrayList<>();
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
            if (player.getLocation().distanceSquared(origin) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            found.add(player);
        }
        return found;
    }
}
