package com.monkey.ktplus.effects.list.gravitywell.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class GravityWellAnimation {
    private static final int DEFAULT_WELL_COUNT = 3;
    private static final double DEFAULT_MOVE_SPEED = 0.1;
    private static final double DEFAULT_PULL = 0.22;
    private static final double DEFAULT_SLAM_DAMAGE = 5.0;
    private static final int DEFAULT_HUNT_TICKS = 200;
    private static final double MERGE_DISTANCE = 1.6;
    private static final double SLAM_DISTANCE = 1.35;
    private static final int SLAM_COOLDOWN = 16;

    private static final Color VOID_CORE = Color.fromRGB(20, 5, 35);
    private static final Color VOID_MID = Color.fromRGB(70, 20, 110);
    private static final Color VOID_EDGE = Color.fromRGB(140, 60, 190);
    private static final Color SLAM = Color.fromRGB(200, 120, 255);

    private GravityWellAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.1, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("gravitywell");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int wellCount = perks == null
                ? DEFAULT_WELL_COUNT
                : Math.max(1, perks.getInt("well-count", DEFAULT_WELL_COUNT));
        double moveSpeed = perks == null
                ? DEFAULT_MOVE_SPEED
                : Math.max(0.01, perks.getDouble("move-speed", DEFAULT_MOVE_SPEED));
        double pull = perks == null ? DEFAULT_PULL : Math.max(0.0, perks.getDouble("pull", DEFAULT_PULL));
        double slamDamage = perks == null
                ? DEFAULT_SLAM_DAMAGE
                : Math.max(0.0, perks.getDouble("slam-damage", DEFAULT_SLAM_DAMAGE));
        boolean mergeOnHit = perks == null || perks.getBoolean("merge-on-hit", true);
        int huntTicks = perks == null
                ? DEFAULT_HUNT_TICKS
                : Math.max(1, perks.getInt("hunt-ticks", DEFAULT_HUNT_TICKS));

        EffectDamageConfig damageCfg = context.config().effectDamage("gravitywell");
        double damageValue = damageCfg.enabled() ? Math.max(0.5, damageCfg.value()) : slamDamage;
        double damageRadius = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 6.0;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        List<Well> wells = new ArrayList<>(wellCount);
        for (int i = 0; i < wellCount; i++) {
            double a = (Math.PI * 2.0 * i) / wellCount + 0.4;
            Location start = origin.clone().add(Math.cos(a) * 2.2, 0.0, Math.sin(a) * 2.2);
            snapToGround(world, start);
            wells.add(new Well(start, i, randomWander(moveSpeed)));
        }

        AtomicInteger tick = new AtomicInteger();
        AtomicInteger huntLeft = new AtomicInteger(huntTicks);
        Set<UUID> slamCooldown = new HashSet<>();
        boolean[] finished = {false};

        session.onCleanup(() -> PerkActionBar.clear(killer));

        visuals.sound("BLOCK_END_PORTAL_SPAWN", origin, 0.85f, 0.55f);
        visuals.sound("ENTITY_ENDERMAN_TELEPORT", origin, 1.1f, 0.45f);
        session.resetDeadline(huntTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            int left = huntLeft.decrementAndGet();
            if (left <= 0 || wells.isEmpty()) {
                dissipate(visuals, wells, killer);
                finished[0] = true;
                return false;
            }

            for (Well well : wells) {
                moveWell(session, visuals, world, well, killer, victimId, moveSpeed, current);
                drawWell(visuals, well, current);
                pullAndSlam(
                        session,
                        visuals,
                        world,
                        well,
                        killer,
                        victimId,
                        pull,
                        damageValue,
                        damageRadius,
                        slamCooldown);
            }

            if (mergeOnHit) {
                tryMerge(session, visuals, wells, current);
            }

            if (current % 3 == 0 && killer != null && killer.isOnline()) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&5🕳 GRAVITY WELLS &8| &dACTIVE &f%d &8| &bLVL-MAX &f%d &8| &cHUNT &f%.1fs",
                                wells.size(),
                                wells.stream().mapToInt(w -> w.level).max().orElse(1),
                                left / 20.0));
            }
            return true;
        });
    }

    private static void moveWell(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Well well,
            Player killer,
            UUID victimId,
            double moveSpeed,
            int tick) {
        Player target = findNearest(world, well.loc, killer, victimId, 28.0, session);
        if (target != null) {
            Vector to = target.getLocation().clone().add(0, 0.1, 0).toVector().subtract(well.loc.toVector());
            double dist = to.length();
            if (dist > 0.001) {
                double step = Math.min(moveSpeed * (0.85 + well.level * 0.12), dist * 0.1 + 0.02);
                to.normalize().multiply(step);
                
                Vector side = to.clone().crossProduct(new Vector(0, 1, 0));
                if (side.lengthSquared() > 1.0e-6) {
                    side.normalize().multiply(Math.sin(tick * 0.09 + well.index) * 0.03);
                    to.add(side);
                }
                well.loc.add(to);
            }
        } else {
            if (well.wanderLeft-- <= 0) {
                well.wander = randomWander(moveSpeed * 0.85);
                well.wanderLeft = 20 + (int) (Math.random() * 30);
            }
            well.loc.add(well.wander);
        }
        snapToGround(world, well.loc);
        well.loc.setY(well.loc.getY() + 0.85 + Math.sin(tick * 0.12 + well.index) * 0.12);

        if (tick % 14 == 0) {
            visuals.sound("ENTITY_ENDERMAN_TELEPORT", well.loc, 0.25f, 0.4f + well.level * 0.05f);
        }
    }

    private static void drawWell(VisualEffectService visuals, Well well, int tick) {
        double radius = 0.55 + well.level * 0.22;
        int shells = 3 + well.level;
        for (int shell = 0; shell < shells; shell++) {
            double t = shell / (double) Math.max(1, shells - 1);
            double r = radius * (0.35 + t * 0.95);
            int points = ParticleScale.scale(8 + well.level * 2);
            double spin = tick * (0.14 + well.level * 0.02) * (shell % 2 == 0 ? 1 : -1);
            for (int i = 0; i < points; i++) {
                double a = spin + (Math.PI * 2.0 * i) / points + shell * 0.4;
                double y = Math.sin(tick * 0.18 + i + shell) * 0.25 * (1.0 - t);
                Location p = well.loc.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                Color color = t < 0.35 ? VOID_CORE : (t < 0.7 ? VOID_MID : VOID_EDGE);
                visuals.dust(p, color, (float) (1.0 + well.level * 0.15), 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        visuals.dust(well.loc, VOID_CORE, 1.6f + well.level * 0.2f, 4, 0.12, 0.12, 0.12, 0.0);
        visuals.particle("PORTAL", well.loc, ParticleScale.scale(3 + well.level), 0.2, 0.25, 0.2, 0.15, null);
        if (tick % 2 == 0) {
            visuals.particle("SQUID_INK", well.loc, 2, 0.15, 0.15, 0.15, 0.01, null);
        }
        
        int ground = ParticleScale.scale(6 + well.level);
        for (int i = 0; i < ground; i++) {
            double a = tick * 0.05 + (Math.PI * 2.0 * i) / ground;
            Location g = well.loc.clone().add(Math.cos(a) * radius * 1.4, -0.7, Math.sin(a) * radius * 1.4);
            visuals.dust(g, VOID_MID, 1.1f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void pullAndSlam(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Well well,
            Player killer,
            UUID victimId,
            double pull,
            double damage,
            double radius,
            Set<UUID> slamCooldown) {
        double pullRadius = radius * (0.9 + well.level * 0.15);
        double pullSq = pullRadius * pullRadius;
        double strength = pull * (1.0 + (well.level - 1) * 0.28);

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
            Location pLoc = player.getLocation();
            if (pLoc.distanceSquared(well.loc) > pullSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, pLoc)) {
                continue;
            }

            Vector toWell = well.loc.toVector().subtract(pLoc.toVector());
            double dist = toWell.length();
            if (dist > 0.001) {
                toWell.normalize().multiply(Math.min(strength, dist * 0.35));
                toWell.setY(Math.max(-0.05, Math.min(0.12, toWell.getY())));
                player.setVelocity(player.getVelocity().multiply(0.65).add(toWell));
            }

            if (dist <= SLAM_DISTANCE + well.level * 0.15 && !slamCooldown.contains(player.getUniqueId())) {
                slamCooldown.add(player.getUniqueId());
                UUID id = player.getUniqueId();
                session.runLater(SLAM_COOLDOWN, () -> slamCooldown.remove(id));

                double dmg = damage * (0.9 + well.level * 0.2);
                if (killer != null) {
                    player.damage(dmg, killer);
                } else {
                    player.damage(dmg);
                }
                Vector slam = new Vector(0, -0.55 - well.level * 0.1, 0);
                player.setVelocity(player.getVelocity().add(slam));

                Location at = pLoc.clone().add(0, 0.5, 0);
                visuals.sound("ENTITY_GENERIC_EXPLODE", at, 0.7f, 1.35f);
                visuals.sound("ENTITY_WARDEN_SONIC_BOOM", well.loc, 0.35f, 1.6f);
                visuals.dust(at, SLAM, 1.6f, 14, 0.35, 0.2, 0.35, 0.0);
                visuals.particle("EXPLOSION", at, 1, 0.05, 0.05, 0.05, 0.0, null);
                well.pendingMergeBoost = true;
            }
        }
    }

    private static void tryMerge(
            EffectSession session, VisualEffectService visuals, List<Well> wells, int tick) {
        if (wells.size() < 2) {
            return;
        }
        for (int i = 0; i < wells.size(); i++) {
            for (int j = i + 1; j < wells.size(); j++) {
                Well a = wells.get(i);
                Well b = wells.get(j);
                if (a.loc.distanceSquared(b.loc) > MERGE_DISTANCE * MERGE_DISTANCE) {
                    continue;
                }
                if (!a.pendingMergeBoost && !b.pendingMergeBoost && tick % 40 != 0) {
                    continue;
                }
                
                a.level = Math.min(5, a.level + b.level);
                a.loc.add(b.loc.toVector().subtract(a.loc.toVector()).multiply(0.5));
                visuals.sound("BLOCK_RESPAWN_ANCHOR_SET_SPAWN", a.loc, 1.0f, 0.7f);
                visuals.sound("ENTITY_GENERIC_EXPLODE", a.loc, 0.65f, 1.5f);
                visuals.dust(a.loc, VOID_EDGE, 1.8f, 24, 0.6, 0.5, 0.6, 0.0);
                visuals.particle("PORTAL", a.loc, ParticleScale.scale(30), 0.6, 0.6, 0.6, 0.4, null);
                wells.remove(j);
                a.pendingMergeBoost = false;
                return;
            }
        }
        
        for (Well well : wells) {
            if (well.pendingMergeBoost && tick % 5 == 0) {
                
                if (well.level < 4) {
                    well.level++;
                    visuals.sound("ENTITY_PLAYER_LEVELUP", well.loc, 0.45f, 0.7f);
                    visuals.dust(well.loc, SLAM, 1.4f, 10, 0.4, 0.3, 0.4, 0.0);
                }
                well.pendingMergeBoost = false;
            }
        }
    }

    private static void dissipate(VisualEffectService visuals, List<Well> wells, Player killer) {
        for (Well well : wells) {
            visuals.sound("ENTITY_ENDERMAN_TELEPORT", well.loc, 0.8f, 0.35f);
            visuals.particle("SQUID_INK", well.loc, ParticleScale.scale(25), 0.6, 0.6, 0.6, 0.04, null);
            visuals.dust(well.loc, VOID_MID, 1.6f, 18, 0.7, 0.5, 0.7, 0.0);
        }
        wells.clear();
        PerkActionBar.clear(killer);
    }

    private static Vector randomWander(double speed) {
        double angle = Math.random() * Math.PI * 2.0;
        return new Vector(Math.cos(angle), 0.0, Math.sin(angle)).multiply(Math.max(0.04, speed));
    }

    private static Player findNearest(
            World world, Location core, Player killer, UUID victimId, double range, EffectSession session) {
        double best = range * range;
        Player nearest = null;
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
            double d = player.getLocation().distanceSquared(core);
            if (d > best) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            best = d;
            nearest = player;
        }
        return nearest;
    }

    private static void snapToGround(World world, Location core) {
        int x = core.getBlockX();
        int z = core.getBlockZ();
        int startY = Math.min(world.getMaxHeight() - 2, core.getBlockY() + 4);
        int minY = Math.max(world.getMinHeight(), core.getBlockY() - 8);
        for (int y = startY; y >= minY; y--) {
            Block b = world.getBlockAt(x, y, z);
            if (b.getType().isSolid() && b.getType() != Material.BARRIER) {
                core.setY(y + 1.05);
                return;
            }
        }
    }

    private static final class Well {
        private final Location loc;
        private final int index;
        private int level = 1;
        private Vector wander;
        private int wanderLeft = 24;
        private boolean pendingMergeBoost;

        private Well(Location loc, int index, Vector wander) {
            this.loc = loc.clone();
            this.index = index;
            this.wander = wander;
        }
    }
}
