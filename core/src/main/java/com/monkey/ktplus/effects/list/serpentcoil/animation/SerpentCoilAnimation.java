package com.monkey.ktplus.effects.list.serpentcoil.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.serpentcoil.animation.util.SerpentCoilDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class SerpentCoilAnimation {
    private static final int DEFAULT_SEGMENTS = 14;
    private static final int DEFAULT_COIL_TICKS = 60;
    private static final double DEFAULT_CRUSH = 5.0;
    private static final double DEFAULT_SEEK = 14.0;
    private static final int EMERGE_TICKS = 25;
    private static final int CRUSH_TICKS = 70;
    private static final String[] SOUNDS = {
        "ENTITY_BOA_HISS",
        "ENTITY_PHANTOM_FLAP",
        "BLOCK_SLIME_BLOCK_HIT",
        "ENTITY_PLAYER_ATTACK_CRIT",
        "BLOCK_GRASS_BREAK"
    };

    private static final Color GREEN = Color.fromRGB(40, 160, 55);
    private static final Color LIME = Color.fromRGB(90, 220, 70);
    private static final Color DARK = Color.fromRGB(20, 70, 30);

    private SerpentCoilAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.4, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection perks = perks(context);
        int segments = floorInt(perks, "segments", DEFAULT_SEGMENTS, 1);
        int coilTicks = floorInt(perks, "coil-ticks", DEFAULT_COIL_TICKS, 1);
        double crushDamageBase = perkDouble(perks, "crush-damage", DEFAULT_CRUSH, 1.0);
        double seekRange = perkDouble(perks, "seek-range", DEFAULT_SEEK, 6.0);

        EffectDamageConfig damageCfg = context.config().effectDamage("serpentcoil");
        final double crushDamage = damageCfg.enabled()
                ? Math.max(crushDamageBase, damageCfg.value())
                : crushDamageBase;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        List<Segment> body = new ArrayList<>(segments);
        for (int i = 0; i < segments; i++) {
            Location spawn = origin.clone().add(0, -0.4 - i * 0.15, 0);
            BlockDisplay display = SerpentCoilDisplays.spawnSegment(spawn, i);
            if (display != null) {
                session.trackEntity(display);
                body.add(new Segment(display, spawn, i));
            }
        }

        AtomicInteger tick = new AtomicInteger();
        Set<UUID> crushed = new HashSet<>();
        Map<UUID, Integer> coilDamageTicks = new HashMap<>();
        boolean[] finished = {false};
        Location head = origin.clone();
        double[] coilProgress = {0.0};

        session.onCleanup(() -> {
            stopSounds(origin);
            clear(body);
            PerkActionBar.clear(killer);
        });
        int durationTicks = EMERGE_TICKS + coilTicks + CRUSH_TICKS;
        session.resetDeadline(durationTicks + 40L);

        visuals.sound("ENTITY_PHANTOM_FLAP", origin, 1.0f, 0.55f);
        visuals.sound("BLOCK_SLIME_BLOCK_PLACE", origin, 0.8f, 0.7f);

        double finalDamage = crushDamage;
        double finalSeek = seekRange;
        int finalCoil = coilTicks;

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                clear(body);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks || body.isEmpty()) {
                finish(visuals, head, body, killer);
                finished[0] = true;
                return false;
            }

            Player target = findNearest(world, head, killer, victimId, finalSeek, session);
            if (current < EMERGE_TICKS) {
                
                double rise = (current + 1) / (double) EMERGE_TICKS;
                head.setY(origin.getY() - 1.2 + rise * 2.0);
                placeChain(body, head, current, 0.35 + rise * 0.4, 0.0);
                visuals.dust(head, DARK, 1.2f, ParticleScale.scale(3), 0.2, 0.1, 0.2, 0.0);
                visuals.blockParticle("BLOCK", head, ParticleScale.scale(4), 0.2, 0.15, 0.2, 0.02, org.bukkit.Material.DIRT);
                if (current % 6 == 0) {
                    visuals.sound("BLOCK_GRASS_BREAK", head, 0.6f, 0.6f);
                }
                showBar(killer, "EMERGE", (int) (rise * 100), 0);
            } else if (current < EMERGE_TICKS + finalCoil) {
                int local = current - EMERGE_TICKS;
                double t = (local + 1) / (double) finalCoil;
                coilProgress[0] = t;
                if (target != null) {
                    Location aim = target.getLocation().clone().add(0, 1.0, 0);
                    Vector to = aim.toVector().subtract(head.toVector());
                    if (to.lengthSquared() > 0.01) {
                        head.add(to.normalize().multiply(0.22));
                    }
                } else {
                    head.add(Math.cos(current * 0.12) * 0.08, Math.sin(current * 0.09) * 0.02, Math.sin(current * 0.12) * 0.08);
                }
                double radius = 0.6 + t * 1.8;
                placeCoil(body, head, current, radius, t);
                drawCoilAura(visuals, head, radius, current);
                if (current % 10 == 0) {
                    visuals.sound("ENTITY_PHANTOM_FLAP", head, 0.45f, 0.7f + (float) t * 0.4f);
                }
                if (t > 0.45 && target != null) {
                    tryCrush(session, visuals, killer, target, head, finalDamage * 0.35, coilDamageTicks, current);
                }
                showBar(killer, "COIL", (int) (t * 100), coilDamageTicks.size());
            } else {
                
                int local = current - EMERGE_TICKS - finalCoil;
                placeCoil(body, head, current, 2.2 - local * 0.02, 1.0);
                if (local == 0) {
                    crushBurst(session, visuals, world, head, killer, victimId, finalDamage * 1.15, crushed);
                }
                if (local % 8 == 0) {
                    visuals.dust(head, LIME, 1.4f, ParticleScale.scale(8), 0.5, 0.3, 0.5, 0.0);
                }
                showBar(killer, "CRUSH", Math.min(100, local * 3), crushed.size());
                if (local > CRUSH_TICKS) {
                    finish(visuals, head, body, killer);
                    finished[0] = true;
                    return false;
                }
            }
            return true;
        });
    }

    private static void placeChain(List<Segment> body, Location head, int tick, double spacing, double coil) {
        Location prev = head.clone();
        for (int i = 0; i < body.size(); i++) {
            Segment seg = body.get(i);
            double angle = tick * 0.18 + i * 0.35;
            Location at = prev.clone().add(
                    Math.cos(angle) * coil * 0.15,
                    -spacing * 0.55,
                    Math.sin(angle) * coil * 0.15);
            float scale = i == 0 ? 0.55f : 0.42f - Math.min(0.18f, i * 0.012f);
            float yaw = (float) Math.atan2(at.getZ() - prev.getZ(), at.getX() - prev.getX());
            SerpentCoilDisplays.place(seg.display, at, scale, yaw, (float) Math.sin(tick * 0.2 + i) * 0.2f);
            seg.loc = at;
            prev = at;
        }
    }

    private static void placeCoil(List<Segment> body, Location head, int tick, double radius, double t) {
        for (int i = 0; i < body.size(); i++) {
            Segment seg = body.get(i);
            double frac = i / (double) Math.max(1, body.size() - 1);
            double angle = tick * 0.22 + frac * Math.PI * 2.4 * (0.5 + t);
            double r = radius * (0.35 + frac * 0.75);
            double y = Math.sin(frac * Math.PI * 2.0 + tick * 0.15) * 0.55 + frac * 0.35;
            Location at = head.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
            float scale = i == 0 ? 0.58f : 0.4f - Math.min(0.16f, i * 0.01f);
            SerpentCoilDisplays.place(seg.display, at, scale, (float) angle, (float) (frac * 0.4));
            seg.loc = at;
        }
    }

    private static void drawCoilAura(VisualEffectService visuals, Location head, double radius, int tick) {
        int pts = ParticleScale.scale(10);
        for (int i = 0; i < pts; i++) {
            double a = tick * 0.15 + (Math.PI * 2.0 * i) / pts;
            Location p = head.clone().add(Math.cos(a) * radius, 0.2, Math.sin(a) * radius);
            visuals.dust(p, i % 2 == 0 ? GREEN : LIME, 1.0f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        visuals.particle("COMPOSTER", head, ParticleScale.scale(2), 0.3, 0.2, 0.3, 0.0, null);
    }

    private static void tryCrush(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Player target,
            Location head,
            double damage,
            Map<UUID, Integer> lastCrushTick,
            int currentTick) {
        if (target.getLocation().distanceSquared(head) > 6.25) {
            return;
        }
        Integer last = lastCrushTick.get(target.getUniqueId());
        if (last != null && currentTick - last < 9) {
            return;
        }
        if (killer != null && !session.allowsWorldMutation(killer, target.getLocation())) {
            return;
        }
        lastCrushTick.put(target.getUniqueId(), currentTick);
        Location at = target.getLocation().clone().add(0, 1, 0);
        visuals.dust(at, LIME, 1.6f, 10, 0.3, 0.3, 0.3, 0.0);
        visuals.sound("ENTITY_PLAYER_ATTACK_CRIT", at, 1.0f, 0.7f);
        visuals.sound("BLOCK_SLIME_BLOCK_HIT", at, 0.9f, 0.55f);
        if (killer != null && damage > 0) {
            target.damage(damage, killer);
        } else if (damage > 0) {
            target.damage(damage);
        }
        Vector pull = head.toVector().subtract(target.getLocation().toVector());
        if (pull.lengthSquared() > 0.01) {
            pull.normalize().multiply(0.35).setY(0.15);
            target.setVelocity(pull);
        }
    }

    private static void crushBurst(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location head,
            Player killer,
            UUID victimId,
            double damage,
            Set<UUID> crushed) {
        visuals.sound("ENTITY_GENERIC_EXPLODE", head, 0.7f, 0.85f);
        visuals.dust(head, GREEN, 1.8f, ParticleScale.scale(20), 1.0, 0.5, 1.0, 0.0);
        visuals.particle("EXPLOSION", head, ParticleScale.scale(2), 0.4, 0.2, 0.4, 0.0, null);
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
            if (player.getLocation().distanceSquared(head) > 16.0) {
                continue;
            }
            if (crushed.contains(player.getUniqueId())) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            crushed.add(player.getUniqueId());
            if (killer != null) {
                player.damage(damage, killer);
            } else {
                player.damage(damage);
            }
        }
    }

    private static void finish(VisualEffectService visuals, Location head, List<Segment> body, Player killer) {
        visuals.dust(head, DARK, 1.3f, ParticleScale.scale(12), 0.6, 0.4, 0.6, 0.0);
        visuals.sound("ENTITY_PHANTOM_DEATH", head, 0.55f, 0.8f);
        stopSounds(head);
        clear(body);
        PerkActionBar.clear(killer);
    }

    private static void clear(List<Segment> body) {
        for (Segment seg : body) {
            SerpentCoilDisplays.remove(seg.display);
        }
        body.clear();
    }

    private static void stopSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 64.0 * 64.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
            EntityCompat.stopSound(player, "BLOCK_SLIME_BLOCK_PLACE");
            EntityCompat.stopSound(player, "ENTITY_GENERIC_EXPLODE");
        }
    }

    private static void showBar(Player killer, String phase, int pct, int hits) {
        if (killer == null || !killer.isOnline()) {
            return;
        }
        PerkActionBar.show(
                killer,
                String.format("&a蛇 SERPENTCOIL &8| &f%s &a%d%% &8| &cHITS &f%d", phase, pct, hits));
    }

    private static List<Player> nearby(
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
        found.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(origin)));
        return found;
    }

    private static Player findNearest(
            World world, Location origin, Player killer, UUID victimId, double radius, EffectSession session) {
        List<Player> list = nearby(world, origin, killer, victimId, radius, session);
        return list.isEmpty() ? null : list.get(0);
    }

    private static ConfigurationSection perks(EffectContext context) {
        ConfigurationSection section = context.config().effectSection("serpentcoil");
        return section == null ? null : section.getConfigurationSection("perks");
    }

    private static int floorInt(ConfigurationSection perks, String key, int def, int min) {
        int v = perks == null ? def : perks.getInt(key, def);
        return Math.max(min, v);
    }

    private static double perkDouble(ConfigurationSection perks, String key, double def, double min) {
        return Math.max(min, perks == null ? def : perks.getDouble(key, def));
    }

    private static final class Segment {
        private final BlockDisplay display;
        private Location loc;
        private final int index;

        private Segment(BlockDisplay display, Location loc, int index) {
            this.display = display;
            this.loc = loc.clone();
            this.index = index;
        }
    }
}
