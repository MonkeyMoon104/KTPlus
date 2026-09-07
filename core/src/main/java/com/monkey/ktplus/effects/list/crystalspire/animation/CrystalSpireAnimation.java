package com.monkey.ktplus.effects.list.crystalspire.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.crystalspire.animation.util.CrystalSpireDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class CrystalSpireAnimation {
    private static final int DEFAULT_SPIRE_COUNT = 5;
    private static final int DEFAULT_GROW_TICKS = 35;
    private static final int DEFAULT_SHARD_COUNT = 18;
    private static final double DEFAULT_SHARD_DAMAGE = 2.5;
    private static final double DEFAULT_SEEK_RADIUS = 14.0;
    private static final double DEFAULT_SHARD_SPEED = 0.5;
    private static final int ORBIT_TICKS = 28;
    private static final int SEEK_MAX = 90;
    private static final double HIT_DISTANCE = 1.1;
    private static final int TRAIL_LENGTH = 7;
    private static final Vector UP = new Vector(0, 1, 0);

    private static final Color VIOLET = Color.fromRGB(160, 70, 255);
    private static final Color DEEP = Color.fromRGB(90, 30, 160);
    private static final Color PINK = Color.fromRGB(230, 140, 255);
    private static final Color WHITE = Color.fromRGB(230, 210, 255);

    private static final Material[] SPIRE_MATS = {
        Material.AMETHYST_BLOCK,
        Material.BUDDING_AMETHYST,
        Material.AMETHYST_CLUSTER,
        Material.LARGE_AMETHYST_BUD
    };

    private CrystalSpireAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("crystalspire");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int spireCount = perks == null
                ? DEFAULT_SPIRE_COUNT
                : Math.max(4, Math.min(7, perks.getInt("spire-count", DEFAULT_SPIRE_COUNT)));
        int growTicks = perks == null
                ? DEFAULT_GROW_TICKS
                : Math.max(15, perks.getInt("grow-ticks", DEFAULT_GROW_TICKS));
        int shardCount = perks == null
                ? DEFAULT_SHARD_COUNT
                : Math.max(6, perks.getInt("shard-count", DEFAULT_SHARD_COUNT));
        double shardDamageBase = perks == null
                ? DEFAULT_SHARD_DAMAGE
                : Math.max(0.0, perks.getDouble("shard-damage", DEFAULT_SHARD_DAMAGE));
        double seekRadius = perks == null
                ? DEFAULT_SEEK_RADIUS
                : Math.max(4.0, perks.getDouble("seek-radius", DEFAULT_SEEK_RADIUS));
        double shardSpeed = perks == null
                ? DEFAULT_SHARD_SPEED
                : Math.max(0.15, perks.getDouble("shard-speed", DEFAULT_SHARD_SPEED));

        EffectDamageConfig damageCfg = context.config().effectDamage("crystalspire");
        final double shardDamage = damageCfg.enabled()
                ? Math.max(shardDamageBase, damageCfg.value())
                : shardDamageBase;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        List<Spire> spires = new ArrayList<>();
        for (int i = 0; i < spireCount; i++) {
            double angle = (Math.PI * 2.0 * i) / spireCount + 0.15;
            double dist = 1.1 + (i % 3) * 0.45;
            Location base = origin.clone().add(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist);
            Material mat = SPIRE_MATS[i % SPIRE_MATS.length];
            BlockDisplay display = CrystalSpireDisplays.spawnSpire(base, mat);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            spires.add(new Spire(display, base, angle, 1.6 + (i % 4) * 0.35, i));
        }

        List<Shard> shards = new ArrayList<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] shattered = {false};
        int total = growTicks + ORBIT_TICKS + SEEK_MAX + 10;

        session.onCleanup(() -> {
            for (Spire s : spires) {
                CrystalSpireDisplays.remove(s.display);
            }
            for (Shard sh : shards) {
                CrystalSpireDisplays.remove(sh.display);
            }
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(total + 20L);

        visuals.sound("BLOCK_AMETHYST_BLOCK_RESONATE", origin, 1.0f, 0.85f);
        visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", origin, 0.8f, 1.2f);

        double finalDamage = shardDamage;
        double finalSpeed = shardSpeed;
        double finalSeek = seekRadius;

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                clearAll(spires, shards);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total && shards.isEmpty()) {
                clearAll(spires, shards);
                PerkActionBar.clear(killer);
                return false;
            }

            if (current < growTicks) {
                double progress = (current + 1) / (double) growTicks;
                for (Spire spire : spires) {
                    float h = (float) (spire.maxHeight * progress);
                    float w = 0.35f + (float) progress * 0.35f;
                    CrystalSpireDisplays.placeSpire(
                            spire.display,
                            spire.base.clone().add(0, 0.02, 0),
                            (float) (spire.angle + current * 0.02),
                            w,
                            h,
                            w);
                    if (current % 2 == 0) {
                        Location tip = spire.base.clone().add(0, h, 0);
                        visuals.dust(tip, VIOLET, 1.0f, 2, 0.05, 0.05, 0.05, 0.0);
                        visuals.particle("END_ROD", tip, 1, 0.0, 0.0, 0.0, 0.0, null);
                    }
                }
                if (current % 7 == 0) {
                    visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", origin, 0.45f, 0.9f + (float) progress * 0.5f);
                }
                showBar(killer, "GROW", (int) (progress * 100), shards.size());
            } else {
                if (!shattered[0]) {
                    shattered[0] = true;
                    shatter(session, visuals, origin, spires, shards, shardCount);
                    visuals.sound("BLOCK_AMETHYST_BLOCK_BREAK", origin, 1.2f, 0.7f);
                    visuals.sound("BLOCK_GLASS_BREAK", origin, 0.9f, 1.3f);
                    visuals.sound("BLOCK_AMETHYST_BLOCK_RESONATE", origin, 1.0f, 1.5f);
                }

                int local = current - growTicks;
                Iterator<Shard> it = shards.iterator();
                while (it.hasNext()) {
                    Shard shard = it.next();
                    if (shard.done || shard.display == null || !shard.display.isValid()) {
                        CrystalSpireDisplays.remove(shard.display);
                        it.remove();
                        continue;
                    }
                    boolean remove = updateShard(
                            session,
                            visuals,
                            world,
                            origin,
                            killer,
                            victimId,
                            shard,
                            local,
                            finalSpeed,
                            finalSeek,
                            finalDamage);
                    if (remove) {
                        CrystalSpireDisplays.remove(shard.display);
                        it.remove();
                    }
                }

                if (local < ORBIT_TICKS) {
                    showBar(killer, "ORBIT", (int) ((local + 1) * 100.0 / ORBIT_TICKS), shards.size());
                } else {
                    showBar(killer, "SEEK", Math.min(100, (local - ORBIT_TICKS) * 2), shards.size());
                }

                if (local > ORBIT_TICKS + SEEK_MAX) {
                    clearAll(spires, shards);
                    PerkActionBar.clear(killer);
                    return false;
                }
            }
            return !shards.isEmpty() || current < growTicks + 5 || !shattered[0];
        });
    }

    private static void shatter(
            EffectSession session,
            VisualEffectService visuals,
            Location origin,
            List<Spire> spires,
            List<Shard> shards,
            int shardCount) {
        for (Spire spire : spires) {
            Location tip = spire.base.clone().add(0, spire.maxHeight * 0.85, 0);
            visuals.dust(tip, PINK, 1.5f, 10, 0.3, 0.4, 0.3, 0.0);
            visuals.particle("BLOCK", tip, ParticleScale.scale(8), 0.2, 0.3, 0.2, 0.05, null, Material.AMETHYST_BLOCK.createBlockData());
            CrystalSpireDisplays.remove(spire.display);
        }
        spires.clear();

        for (int i = 0; i < shardCount; i++) {
            double angle = (Math.PI * 2.0 * i) / shardCount + Math.random() * 0.2;
            double burst = 0.6 + (i % 4) * 0.25;
            Location start = origin.clone().add(
                    Math.cos(angle) * burst,
                    0.8 + (i % 5) * 0.2,
                    Math.sin(angle) * burst);
            ItemDisplay display = CrystalSpireDisplays.spawnShard(start);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            shards.add(new Shard(
                    display,
                    start,
                    i,
                    angle,
                    0.9 + (i % 3) * 0.25,
                    0.5 + (i % 4) * 0.15,
                    (Math.PI * 2.0 * i) / shardCount));
        }
    }

    private static boolean updateShard(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            Shard shard,
            int globalLocal,
            double speed,
            double seekRadius,
            double damage) {
        shard.age++;
        float spin = shard.age * 0.35f + shard.index * 0.2f;

        if (globalLocal < ORBIT_TICKS || shard.age < ORBIT_TICKS - shard.index % 5) {
            double t = Math.min(1.0, shard.age / (double) ORBIT_TICKS);
            double r = shard.orbitR * (0.7 + t * 0.5);
            shard.phase += 0.18;
            double y = 1.0 + Math.sin(shard.phase * 1.4 + shard.index) * 0.45 + t * 0.8;
            shard.loc.setX(origin.getX() + Math.cos(shard.phase) * r);
            shard.loc.setY(origin.getY() + y);
            shard.loc.setZ(origin.getZ() + Math.sin(shard.phase) * r);
            CrystalSpireDisplays.placeShard(shard.display, shard.loc, spin, spin * 0.5f, spin * 0.3f, 0.55f);
            pushTrail(shard);
            drawTrail(visuals, shard);
            visuals.dust(shard.loc, VIOLET, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);
            return false;
        }

        List<Player> enemies = findNearby(world, shard.loc, killer, victimId, seekRadius, session);
        Player target = null;
        if (!enemies.isEmpty()) {
            if (shard.targetId != null) {
                for (Player p : enemies) {
                    if (p.getUniqueId().equals(shard.targetId)) {
                        target = p;
                        break;
                    }
                }
            }
            if (target == null) {
                target = enemies.get(shard.index % enemies.size());
                shard.targetId = target.getUniqueId();
            }
        }

        if (target == null) {
            
            shard.loc.add(Math.cos(shard.phase) * 0.08, 0.04, Math.sin(shard.phase) * 0.08);
            CrystalSpireDisplays.placeShard(shard.display, shard.loc, spin, spin * 0.4f, spin * 0.2f, 0.4f);
            visuals.dust(shard.loc, DEEP, 0.7f, 1, 0.0, 0.0, 0.0, 0.0);
            return shard.age > ORBIT_TICKS + 40;
        }

        Location aim = target.getLocation().clone().add(0, 1.0, 0);
        double dist = shard.loc.distance(aim);
        if (dist <= HIT_DISTANCE && shard.age > ORBIT_TICKS + 8) {
            impact(session, visuals, killer, target, shard, damage);
            shard.done = true;
            return true;
        }

        Vector toAim = aim.toVector().subtract(shard.loc.toVector());
        if (toAim.lengthSquared() < 1.0e-6) {
            return false;
        }
        Vector forward = toAim.normalize();
        Vector side = forward.clone().crossProduct(UP);
        if (side.lengthSquared() < 1.0e-6) {
            side = new Vector(1, 0, 0);
        } else {
            side.normalize();
        }
        double fade = Math.min(1.0, dist / 5.0);
        double sway = Math.sin(shard.age * 0.25 + shard.phase) * shard.sideBias * fade;
        Vector step = forward.multiply(Math.min(speed, Math.max(0.2, dist * 0.3)));
        step.add(side.multiply(sway * 0.2));
        step.add(UP.clone().multiply(Math.sin(shard.age * 0.2) * 0.08 * fade));
        shard.loc.add(step);

        CrystalSpireDisplays.placeShard(shard.display, shard.loc, spin, spin * 0.55f, spin * 0.25f, 0.6f);
        pushTrail(shard);
        drawTrail(visuals, shard);
        visuals.particle("END_ROD", shard.loc, 1, 0.0, 0.0, 0.0, 0.0, null);
        return shard.age > ORBIT_TICKS + SEEK_MAX;
    }

    private static void impact(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Player target,
            Shard shard,
            double damage) {
        Location at = target.getLocation().clone().add(0, 1, 0);
        visuals.dust(at, PINK, 1.4f, 8, 0.2, 0.2, 0.2, 0.0);
        visuals.dust(at, WHITE, 1.0f, 4, 0.15, 0.15, 0.15, 0.0);
        visuals.particle("CRIT", at, 6, 0.2, 0.2, 0.2, 0.05, null);
        visuals.sound("BLOCK_AMETHYST_BLOCK_HIT", at, 0.8f, 1.4f + shard.index * 0.02f);
        visuals.sound("ENTITY_PLAYER_HURT", at, 0.55f, 1.2f);
        if (damage <= 0.0 || killer == null) {
            return;
        }
        if (!session.allowsWorldMutation(killer, target.getLocation())) {
            return;
        }
        target.damage(damage, killer);
    }

    private static void pushTrail(Shard shard) {
        shard.trail.addLast(shard.loc.clone());
        while (shard.trail.size() > TRAIL_LENGTH) {
            shard.trail.removeFirst();
        }
    }

    private static void drawTrail(VisualEffectService visuals, Shard shard) {
        int i = 0;
        int size = shard.trail.size();
        for (Location point : shard.trail) {
            float t = size <= 1 ? 1.0f : i / (float) (size - 1);
            visuals.dust(point, t > 0.5f ? VIOLET : DEEP, 0.55f + t * 0.6f, 1, 0.0, 0.0, 0.0, 0.0);
            i++;
        }
    }

    private static void showBar(Player killer, String phase, int pct, int shards) {
        if (killer == null || !killer.isOnline()) {
            return;
        }
        PerkActionBar.show(
                killer,
                String.format("&d◆ CRYSTALSPIRE &8| &f%s &a%d%% &8| &bSHARDS &f%d", phase, pct, shards));
    }

    private static void clearAll(List<Spire> spires, List<Shard> shards) {
        for (Spire s : spires) {
            CrystalSpireDisplays.remove(s.display);
        }
        spires.clear();
        for (Shard sh : shards) {
            CrystalSpireDisplays.remove(sh.display);
        }
        shards.clear();
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
        found.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(origin)));
        return found;
    }

    private static final class Spire {
        private final BlockDisplay display;
        private final Location base;
        private final double angle;
        private final double maxHeight;
        private final int index;

        private Spire(BlockDisplay display, Location base, double angle, double maxHeight, int index) {
            this.display = display;
            this.base = base.clone();
            this.angle = angle;
            this.maxHeight = maxHeight;
            this.index = index;
        }
    }

    private static final class Shard {
        private final ItemDisplay display;
        private final Location loc;
        private final int index;
        private final double orbitR;
        private final double sideBias;
        private double phase;
        private UUID targetId;
        private int age;
        private boolean done;
        private final Deque<Location> trail = new ArrayDeque<>(TRAIL_LENGTH + 1);

        private Shard(
                ItemDisplay display,
                Location loc,
                int index,
                double phase,
                double orbitR,
                double sideBias,
                double phaseBias) {
            this.display = display;
            this.loc = loc.clone();
            this.index = index;
            this.phase = phase + phaseBias;
            this.orbitR = orbitR;
            this.sideBias = sideBias;
        }
    }
}
