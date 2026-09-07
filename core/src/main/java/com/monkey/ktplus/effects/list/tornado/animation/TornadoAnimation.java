package com.monkey.ktplus.effects.list.tornado.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.tornado.animation.util.TornadoDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class TornadoAnimation {
    private static final int MAX_LEVEL = 5;
    private static final int DEFAULT_HUNT_TICKS = 200;
    private static final int MAX_LEVEL_HUNT_TICKS = 300;
    private static final double DEFAULT_SEEK_RANGE = 40.0;
    private static final double DEFAULT_DAMAGE_BASE = 2.0;

    private static final Color DUST_DARK = Color.fromRGB(70, 70, 75);
    private static final Color DUST_MID = Color.fromRGB(120, 115, 110);
    private static final Color DUST_LIGHT = Color.fromRGB(190, 190, 195);
    private static final Color DUST_DIRT = Color.fromRGB(110, 75, 45);

    private static final String[] AMBIENT_SOUNDS = {
        "ITEM_ELYTRA_FLYING",
        "ENTITY_BREEZE_IDLE_GROUND",
        "ENTITY_BREEZE_IDLE_AIR",
        "ENTITY_WIND_CHARGE_WIND_BURST",
        "ENTITY_WIND_CHARGE_THROW",
        "ENTITY_BREEZE_SHOOT",
        "ENTITY_BREEZE_JUMP"
    };

    private TornadoAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("tornado");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        final int huntTicks = perks == null
                ? DEFAULT_HUNT_TICKS
                : Math.max(40, perks.getInt("hunt-ticks", DEFAULT_HUNT_TICKS));
        final int maxHuntTicks = perks == null
                ? MAX_LEVEL_HUNT_TICKS
                : Math.max(huntTicks, perks.getInt("max-hunt-ticks", MAX_LEVEL_HUNT_TICKS));
        double seekRange = perks == null
                ? DEFAULT_SEEK_RANGE
                : Math.max(8.0, perks.getDouble("seek-range", DEFAULT_SEEK_RANGE));
        EffectDamageConfig damageCfg = context.config().effectDamage("tornado");
        double damageBase = damageCfg.enabled()
                ? Math.max(0.5, damageCfg.value())
                : (perks == null ? DEFAULT_DAMAGE_BASE : perks.getDouble("damage-base", DEFAULT_DAMAGE_BASE));
        int hitCooldownTicks = perks == null ? 18 : Math.max(5, perks.getInt("hit-cooldown-ticks", 18));
        ConfigurationSection levelsSection = perks == null ? null : perks.getConfigurationSection("levels");

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        Location core = origin.clone();
        AtomicInteger level = new AtomicInteger(1);
        AtomicInteger huntLeft = new AtomicInteger(huntTicks);
        AtomicInteger tick = new AtomicInteger();
        List<OrbitBlock> orbit = new ArrayList<>();
        List<ThrownBlock> thrown = new ArrayList<>();
        Set<String> rippedKeys = new HashSet<>();
        Set<UUID> hitCooldown = new HashSet<>();
        boolean[] finished = {false};
        Vector[] wanderDir = {randomWander(0.11)};
        int[] wanderLeft = {28 + (int) (Math.random() * 28)};
        Location prevCore = origin.clone();
        int throwFuseMin = perks == null ? 12 : Math.max(6, perks.getInt("throw-fuse-min-ticks", 12));
        int throwFuseMax = perks == null ? 28 : Math.max(throwFuseMin + 1, perks.getInt("throw-fuse-max-ticks", 28));
        double throwBlastRadius = perks == null ? 2.6 : Math.max(1.0, perks.getDouble("throw-blast-radius", 2.6));
        double throwDamageMult = perks == null ? 0.45 : Math.max(0.1, perks.getDouble("throw-damage-multiplier", 0.45));

        session.onCleanup(() -> {
            stopTornadoSounds(core);
            releaseOrbit(orbit);
            releaseThrown(thrown);
            PerkActionBar.clear(killer);
        });

        visuals.sound("ENTITY_WIND_CHARGE_WIND_BURST", core, 1.4f, 0.55f);
        visuals.sound("ENTITY_BREEZE_IDLE_GROUND", core, 0.7f, 0.75f);
        session.resetDeadline(huntTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopTornadoSounds(core);
                PerkActionBar.clear(killer);
                return false;
            }

            int current = tick.getAndIncrement();
            int lvl = level.get();
            TornadoStats stats = TornadoStats.forLevel(lvl, levelsSection);

            int left = huntLeft.decrementAndGet();
            if (left <= 0) {
                dissipate(visuals, core, orbit, thrown, killer, false);
                finished[0] = true;
                return false;
            }

            Player target = findNearest(world, core, killer, victimId, seekRange, session);
            if (target != null) {
                Vector to = target.getLocation().clone().add(0, 0.2, 0).toVector().subtract(core.toVector());
                double dist = to.length();
                if (dist > 0.001) {
                    
                    double step = Math.min(stats.speed, dist * 0.12 + 0.02);
                    to.normalize().multiply(step);
                    Vector side = to.clone().crossProduct(new Vector(0, 1, 0));
                    if (side.lengthSquared() > 1.0e-6) {
                        side.normalize().multiply(Math.sin(current * 0.11) * 0.025 * lvl);
                        to.add(side);
                    }
                    core.add(to);
                    snapToGround(world, core);
                }

                if (dist <= stats.hitRadius && !hitCooldown.contains(target.getUniqueId())) {
                    hitCooldown.add(target.getUniqueId());
                    final UUID hitId = target.getUniqueId();
                    session.runLater(hitCooldownTicks, () -> hitCooldown.remove(hitId));
                    hitPlayer(session, visuals, killer, target, core, stats, damageBase);
                    tryLevelUp(
                            session,
                            visuals,
                            core,
                            level,
                            huntLeft,
                            huntTicks,
                            maxHuntTicks,
                            levelsSection,
                            orbit,
                            thrown,
                            rippedKeys,
                            throwFuseMin,
                            throwFuseMax);
                }
            } else {
                if (wanderLeft[0]-- <= 0) {
                    wanderDir[0] = randomWander(stats.speed * 0.75);
                    wanderLeft[0] = 24 + (int) (Math.random() * 36);
                }
                double turn = (Math.random() - 0.5) * 0.18;
                double cos = Math.cos(turn);
                double sin = Math.sin(turn);
                double nx = wanderDir[0].getX() * cos - wanderDir[0].getZ() * sin;
                double nz = wanderDir[0].getX() * sin + wanderDir[0].getZ() * cos;
                wanderDir[0].setX(nx);
                wanderDir[0].setZ(nz);
                if (wanderDir[0].lengthSquared() > 1.0e-6) {
                    wanderDir[0].normalize().multiply(stats.speed * 0.7);
                }
                core.add(wanderDir[0]);
                snapToGround(world, core);
            }

            Vector moveDelta = core.toVector().subtract(prevCore.toVector());
            prevCore.setX(core.getX());
            prevCore.setY(core.getY());
            prevCore.setZ(core.getZ());

            if (current % Math.max(2, 7 - lvl) == 0) {
                tryRipBlocks(
                        session,
                        visuals,
                        core,
                        orbit,
                        thrown,
                        rippedKeys,
                        stats,
                        throwFuseMin,
                        throwFuseMax);
            }

            drawFunnel(visuals, core, stats, current, moveDelta);
            updateOrbit(orbit, core, stats, current);
            updateThrown(
                    session,
                    visuals,
                    world,
                    core,
                    killer,
                    victimId,
                    thrown,
                    level,
                    huntLeft,
                    huntTicks,
                    maxHuntTicks,
                    levelsSection,
                    orbit,
                    rippedKeys,
                    damageBase,
                    throwBlastRadius,
                    throwDamageMult,
                    throwFuseMin,
                    throwFuseMax);

            if (current % 10 == 0) {
                visuals.sound("ENTITY_BREEZE_IDLE_AIR", core, 0.35f + lvl * 0.05f, 0.7f);
            }
            if (current % 16 == 0) {
                visuals.sound("ENTITY_PLAYER_ATTACK_SWEEP", core, 0.28f, 0.45f);
            }
            if (current % 22 == 0) {
                visuals.sound("ENTITY_WIND_CHARGE_THROW", core, 0.25f, 0.55f);
            }

            if (current % 3 == 0 && killer != null && killer.isOnline()) {
                String targetName = target != null ? target.getName() : "WANDER";
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&7🌪 TORNADO &8| &eF%d &8| &bTARGET &f%s &8| &cHUNT &f%.1fs &8| &6ORB &f%d&8/&f%d &8| &eFLING &f%d",
                                level.get(),
                                targetName,
                                left / 20.0,
                                orbit.size(),
                                stats.maxBlocks,
                                thrown.size()));
            }

            if (current > huntTicks * (MAX_LEVEL + 1) + 40) {
                dissipate(visuals, core, orbit, thrown, killer, true);
                finished[0] = true;
                return false;
            }
            return true;
        });
    }

    private static Vector randomWander(double speed) {
        double angle = Math.random() * Math.PI * 2.0;
        return new Vector(Math.cos(angle), 0.0, Math.sin(angle)).multiply(Math.max(0.06, speed));
    }

    private static void stopTornadoSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 72.0 * 72.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : AMBIENT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static void hitPlayer(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Player target,
            Location core,
            TornadoStats stats,
            double damageBase) {
        if (killer != null && !session.allowsWorldMutation(killer, target.getLocation())) {
            return;
        }
        double dmg = damageBase * stats.damageMultiplier;
        if (killer != null) {
            target.damage(dmg, killer);
        } else {
            target.damage(dmg);
        }
        Vector away = target.getLocation().toVector().subtract(core.toVector());
        away.setY(0.0);
        if (away.lengthSquared() < 0.01) {
            away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
        }
        away.normalize().multiply(stats.knockback).setY(0.45 + stats.level * 0.1);
        target.setVelocity(away);

        visuals.particle("CLOUD", target.getLocation().add(0, 1, 0), ParticleScale.scale(18), 0.5, 0.5, 0.5, 0.08, null);
        visuals.dust(target.getLocation().add(0, 1, 0), DUST_DIRT, 1.4f, 10, 0.35, 0.35, 0.35, 0.0);
        visuals.sound("ENTITY_PLAYER_HURT", target.getLocation(), 1.0f, 0.9f);
        visuals.sound("ENTITY_BREEZE_JUMP", core, 1.0f, 0.8f);
    }

    private static void tryLevelUp(
            EffectSession session,
            VisualEffectService visuals,
            Location core,
            AtomicInteger level,
            AtomicInteger huntLeft,
            int huntTicks,
            int maxHuntTicks,
            ConfigurationSection levelsSection,
            List<OrbitBlock> orbit,
            List<ThrownBlock> thrown,
            Set<String> rippedKeys,
            int fuseMin,
            int fuseMax) {
        int lvl = level.get();
        if (lvl < MAX_LEVEL) {
            int newLevel = level.incrementAndGet();
            visuals.sound("ENTITY_WIND_CHARGE_THROW", core, 1.5f, 0.7f + lvl * 0.08f);
            visuals.sound("ENTITY_BREEZE_SHOOT", core, 1.1f, 0.85f);
            visuals.sound("ENTITY_GENERIC_EXPLODE", core, 0.7f, 1.4f);
            
            int nextHunt = newLevel >= MAX_LEVEL ? maxHuntTicks : huntTicks;
            huntLeft.set(nextHunt);
            session.resetDeadline(nextHunt + 40L);
        } else {
            visuals.sound("ENTITY_WIND_CHARGE_WIND_BURST", core, 1.8f, 0.4f);
            visuals.sound("ENTITY_GENERIC_EXPLODE", core, 1.2f, 0.85f);
        }
        tryRipBlocks(
                session,
                visuals,
                core,
                orbit,
                thrown,
                rippedKeys,
                TornadoStats.forLevel(level.get(), levelsSection),
                fuseMin,
                fuseMax);
    }

    private static void tryRipBlocks(
            EffectSession session,
            VisualEffectService visuals,
            Location core,
            List<OrbitBlock> orbit,
            List<ThrownBlock> thrown,
            Set<String> rippedKeys,
            TornadoStats stats,
            int fuseMin,
            int fuseMax) {
        World world = core.getWorld();
        if (world == null) {
            return;
        }
        boolean orbitFull = orbit.size() >= stats.maxBlocks;
        int need = orbitFull ? Math.min(2, 1 + stats.level / 2) : Math.min(2 + stats.level / 2, stats.maxBlocks - orbit.size());
        if (need <= 0) {
            return;
        }
        List<Block> samples = TornadoDisplays.sampleRipBlocks(world, core, stats.ripRadius, need * 4);
        for (Block block : samples) {
            if (need <= 0) {
                break;
            }
            String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ()
                    + (orbitFull ? ":t" + System.nanoTime() % 10_000 : "");
            if (!orbitFull && !rippedKeys.add(key)) {
                continue;
            }
            Material mat = block.getType();
            Location spawnAt = orbitFull
                    ? core.clone().add(0.0, 1.2 + Math.random() * 1.5, 0.0)
                    : block.getLocation().add(0.5, 0.8, 0.5);
            ItemDisplay display = TornadoDisplays.spawnRippedBlock(spawnAt, mat);
            if (display == null) {
                if (!orbitFull) {
                    rippedKeys.remove(key);
                }
                continue;
            }
            session.trackEntity(display);
            if (!orbitFull && orbit.size() < stats.maxBlocks) {
                double angle = Math.random() * Math.PI * 2.0;
                double height = 0.6 + Math.random() * (stats.height * 0.75);
                double orbitR = 0.2 + Math.random() * 1.55;
                orbit.add(OrbitBlock.create(display, angle, height, orbitR, stats));
            } else {
                
                double angle = Math.random() * Math.PI * 2.0;
                double speed = 0.45 + Math.random() * 0.55 + stats.level * 0.06;
                Vector velocity = new Vector(
                        Math.cos(angle) * speed,
                        0.35 + Math.random() * 0.55,
                        Math.sin(angle) * speed);
                int fuse = fuseMin + (int) (Math.random() * Math.max(1, fuseMax - fuseMin));
                thrown.add(new ThrownBlock(display, spawnAt.clone(), velocity, fuse));
                visuals.sound("ENTITY_WIND_CHARGE_THROW", spawnAt, 0.7f, 1.15f + (float) (Math.random() * 0.3));
            }
            need--;
        }
    }

    private static void updateThrown(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location core,
            Player killer,
            UUID victimId,
            List<ThrownBlock> thrown,
            AtomicInteger level,
            AtomicInteger huntLeft,
            int huntTicks,
            int maxHuntTicks,
            ConfigurationSection levelsSection,
            List<OrbitBlock> orbit,
            Set<String> rippedKeys,
            double damageBase,
            double blastRadius,
            double throwDamageMult,
            int fuseMin,
            int fuseMax) {
        Iterator<ThrownBlock> it = thrown.iterator();
        while (it.hasNext()) {
            ThrownBlock tb = it.next();
            if (tb.display == null || !tb.display.isValid() || tb.display.isDead()) {
                it.remove();
                continue;
            }
            tb.age++;
            tb.velocity.setY(tb.velocity.getY() - 0.045);
            tb.loc.add(tb.velocity);
            float spin = tb.age * 0.55f;
            TornadoDisplays.place(tb.display, tb.loc, spin, spin * 0.4f, spin * 0.7f, 0.9f);
            visuals.dust(tb.loc, DUST_DIRT, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);

            boolean hitGround = tb.loc.getY() <= core.getY() + 0.15 && tb.velocity.getY() < 0;
            if (tb.age >= tb.fuseTicks || hitGround) {
                boolean leveled = explodeThrown(
                        session,
                        visuals,
                        world,
                        killer,
                        victimId,
                        tb,
                        damageBase,
                        blastRadius,
                        throwDamageMult,
                        level.get());
                TornadoDisplays.remove(tb.display);
                it.remove();
                if (leveled) {
                    tryLevelUp(
                            session,
                            visuals,
                            core,
                            level,
                            huntLeft,
                            huntTicks,
                            maxHuntTicks,
                            levelsSection,
                            orbit,
                            thrown,
                            rippedKeys,
                            fuseMin,
                            fuseMax);
                }
            }
        }
    }

    private static boolean explodeThrown(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            ThrownBlock tb,
            double damageBase,
            double blastRadius,
            double throwDamageMult,
            int level) {
        Location at = tb.loc.clone();
        visuals.sound("ENTITY_GENERIC_EXPLODE", at, 0.85f, 1.35f);
        visuals.sound("BLOCK_STONE_BREAK", at, 0.9f, 0.8f);
        visuals.particle("EXPLOSION", at, 1, 0.05, 0.05, 0.05, 0.0, null);
        visuals.dust(at, DUST_DIRT, 1.5f, 12, 0.35, 0.3, 0.35, 0.0);
        visuals.dust(at, DUST_MID, 1.1f, 8, 0.4, 0.25, 0.4, 0.0);

        double radiusSq = blastRadius * blastRadius;
        double dmg = damageBase * throwDamageMult * (0.8 + level * 0.15);
        boolean hitPlayer = false;
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (!player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().distanceSquared(at) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            hitPlayer = true;
            if (killer != null) {
                player.damage(dmg, killer);
            } else {
                player.damage(dmg);
            }
            Vector away = player.getLocation().toVector().subtract(at.toVector());
            away.setY(0.0);
            if (away.lengthSquared() < 0.01) {
                away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            away.normalize().multiply(0.55).setY(0.35);
            player.setVelocity(player.getVelocity().add(away));
        }
        return hitPlayer;
    }

    private static void updateOrbit(List<OrbitBlock> orbit, Location core, TornadoStats stats, int tick) {
        double maxH = Math.max(1.0, stats.height);
        Iterator<OrbitBlock> it = orbit.iterator();
        while (it.hasNext()) {
            OrbitBlock ob = it.next();
            if (ob.display == null || !ob.display.isValid() || ob.display.isDead()) {
                it.remove();
                continue;
            }

            ob.angle += ob.spinRate;
            ob.orbitFactor += (Math.random() - 0.5) * 0.012;
            if (ob.orbitFactor < 0.35) {
                ob.orbitFactor = 0.35;
            } else if (ob.orbitFactor > 1.45) {
                ob.orbitFactor = 1.45;
            }
            ob.height += ob.climbSpeed;
            if (ob.height > maxH * 0.92) {
                ob.height = maxH * 0.92;
                ob.climbSpeed = -Math.abs(ob.climbSpeed) * (0.7 + Math.random() * 0.5);
            } else if (ob.height < 0.35) {
                ob.height = 0.35;
                ob.climbSpeed = Math.abs(ob.climbSpeed) * (0.7 + Math.random() * 0.5);
            }

            double heightFrac = Math.max(0.0, Math.min(1.0, ob.height / maxH));
            
            double funnelR = stats.baseRadius * (0.32 + heightFrac * heightFrac * 1.65);
            double radiusPulse = 1.0 + Math.sin(tick * ob.radiusOscSpeed + ob.phase) * ob.radiusOscAmp;
            double swirlIn = 0.82 + 0.18 * Math.sin(tick * ob.swirlSpeed + ob.phase * 1.7);
            double r = funnelR * ob.orbitFactor * radiusPulse * swirlIn;
            double bob = Math.sin(tick * ob.heightOscSpeed + ob.phase) * ob.heightOscAmp;
            double lean = Math.sin(ob.angle * 2.0 + tick * 0.07 + ob.phase) * (0.08 + heightFrac * 0.12);

            Location at = core.clone().add(
                    Math.cos(ob.angle) * r + Math.cos(ob.angle + Math.PI * 0.5) * lean,
                    ob.height + bob,
                    Math.sin(ob.angle) * r + Math.sin(ob.angle + Math.PI * 0.5) * lean);

            ob.tumbleYaw += ob.tumbleYawRate;
            ob.tumblePitch += ob.tumblePitchRate;
            ob.tumbleRoll += ob.tumbleRollRate;
            float scale = (float) (0.7 + heightFrac * 0.35 + stats.level * 0.04 + Math.sin(tick * 0.15 + ob.phase) * 0.05);
            TornadoDisplays.place(ob.display, at, ob.tumbleYaw, ob.tumblePitch, ob.tumbleRoll, scale);
        }
    }

    private static void drawFunnel(
            VisualEffectService visuals, Location core, TornadoStats stats, int tick, Vector moveDelta) {
        double moveX = moveDelta == null ? 0.0 : moveDelta.getX();
        double moveZ = moveDelta == null ? 0.0 : moveDelta.getZ();
        double moveLen = Math.sqrt(moveX * moveX + moveZ * moveZ);

        int ghosts = moveLen > 0.04 ? 3 : 1;
        for (int g = 0; g < ghosts; g++) {
            double lag = g * 0.55;
            Location ghost = core.clone().subtract(moveX * lag, 0.0, moveZ * lag);
            float alphaScale = g == 0 ? 1.0f : (float) (0.55 - g * 0.12);
            drawFunnelAt(visuals, ghost, stats, tick + g * 3, alphaScale, g == 0);
        }
    }

    private static void drawFunnelAt(
            VisualEffectService visuals,
            Location core,
            TornadoStats stats,
            int tick,
            float density,
            boolean primary) {
        int layers = 8 + stats.level;
        double spin = tick * stats.spinSpeed * 1.15;
        
        double tickBias = (tick % 5) * 0.17;
        for (int layer = 0; layer < layers; layer++) {
            if (density < 0.9f && layer % 2 != 0) {
                continue;
            }
            double t = layer / (double) Math.max(1, layers - 1);
            double y = 0.12 + t * stats.height;
            double radius = stats.baseRadius * (0.28 + t * t * 1.7);
            int points = Math.max(6, ParticleScale.scale(7 + stats.level + (int) (radius * 2.2)));
            if (density < 0.9f) {
                points = Math.max(4, points / 2);
            }
            for (int i = 0; i < points; i++) {
                double a = spin * (1.0 + t * 0.55)
                        + (Math.PI * 2.0 * i) / points
                        + layer * 0.55
                        + tickBias;
                double swirl = radius * (0.78 + 0.22 * Math.sin(tick * 0.22 + i * 0.9 + layer));
                Location p = core.clone().add(Math.cos(a) * swirl, y, Math.sin(a) * swirl);
                Color color = t < 0.22 ? DUST_DIRT : (t < 0.55 ? DUST_DARK : (t < 0.82 ? DUST_MID : DUST_LIGHT));
                float size = (float) ((0.75 + t * 1.0 + stats.level * 0.08) * (0.75 + density * 0.25));
                visuals.dust(p, color, size, 1, 0.0, 0.0, 0.0, 0.0);
                if (primary && i % 4 == 0) {
                    visuals.particle("CLOUD", p, 1, 0.03, 0.03, 0.03, 0.0, null);
                }
            }
        }
        if (!primary) {
            return;
        }
        visuals.particle(
                "CLOUD",
                core.clone().add(0, stats.height * 0.35, 0),
                ParticleScale.scale(4 + stats.level),
                stats.baseRadius * 0.3,
                stats.height * 0.22,
                stats.baseRadius * 0.3,
                0.015,
                null);
        if (tick % 2 == 0) {
            for (int i = 0; i < 4 + stats.level; i++) {
                double a = Math.random() * Math.PI * 2.0;
                double r = Math.random() * stats.baseRadius * 1.2;
                Location g = core.clone().add(Math.cos(a) * r, 0.08, Math.sin(a) * r);
                visuals.dust(g, DUST_DIRT, 1.15f, 1, 0.06, 0.02, 0.06, 0.0);
            }
        }
    }

    private static void dissipate(
            VisualEffectService visuals,
            Location core,
            List<OrbitBlock> orbit,
            List<ThrownBlock> thrown,
            Player killer,
            boolean force) {
        stopTornadoSounds(core);
        visuals.sound("BLOCK_SAND_BREAK", core, 1.0f, 0.7f);
        visuals.sound("ENTITY_GENERIC_EXTINGUISH_FIRE", core, 0.7f, 0.65f);
        visuals.particle("CLOUD", core.clone().add(0, 2, 0), ParticleScale.scale(35), 1.2, 1.5, 1.2, 0.05, null);
        visuals.dust(core.clone().add(0, 1.5, 0), DUST_MID, 1.5f, 20, 0.8, 1.0, 0.8, 0.0);
        for (OrbitBlock ob : orbit) {
            if (ob.display != null && ob.display.isValid()) {
                visuals.dust(ob.display.getLocation(), DUST_DIRT, 1.0f, 4, 0.15, 0.15, 0.15, 0.0);
            }
            TornadoDisplays.remove(ob.display);
        }
        orbit.clear();
        releaseThrown(thrown);
        PerkActionBar.clear(killer);
        if (force) {
            visuals.sound("ENTITY_GENERIC_EXTINGUISH_FIRE", core, 0.55f, 0.5f);
        }
        stopTornadoSounds(core);
    }

    private static void releaseOrbit(List<OrbitBlock> orbit) {
        for (OrbitBlock ob : orbit) {
            TornadoDisplays.remove(ob.display);
        }
        orbit.clear();
    }

    private static void releaseThrown(List<ThrownBlock> thrown) {
        for (ThrownBlock tb : thrown) {
            TornadoDisplays.remove(tb.display);
        }
        thrown.clear();
    }

    private static Player findNearest(
            World world,
            Location core,
            Player killer,
            UUID victimId,
            double range,
            EffectSession session) {
        double best = range * range;
        Player nearest = null;
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (!player.getWorld().equals(world)) {
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
        int startY = Math.min(world.getMaxHeight() - 2, core.getBlockY() + 3);
        int minY = Math.max(world.getMinHeight(), core.getBlockY() - 6);
        for (int y = startY; y >= minY; y--) {
            Block b = world.getBlockAt(x, y, z);
            if (b.getType().isSolid()) {
                core.setY(y + 1.05);
                return;
            }
        }
    }

    private static final class TornadoStats {
        private final int level;
        private final double baseRadius;
        private final double height;
        private final double speed;
        private final double hitRadius;
        private final double ripRadius;
        private final double spinSpeed;
        private final int maxBlocks;
        private final double damageMultiplier;
        private final double knockback;

        private TornadoStats(
                int level,
                double baseRadius,
                double height,
                double speed,
                double hitRadius,
                double ripRadius,
                double spinSpeed,
                int maxBlocks,
                double damageMultiplier,
                double knockback) {
            this.level = level;
            this.baseRadius = baseRadius;
            this.height = height;
            this.speed = speed;
            this.hitRadius = hitRadius;
            this.ripRadius = ripRadius;
            this.spinSpeed = spinSpeed;
            this.maxBlocks = maxBlocks;
            this.damageMultiplier = damageMultiplier;
            this.knockback = knockback;
        }

        private static TornadoStats forLevel(int level, ConfigurationSection levelsSection) {
            int lvl = Math.max(1, Math.min(MAX_LEVEL, level));
            TornadoStats defaults = defaultsFor(lvl);
            if (levelsSection == null) {
                return defaults;
            }
            ConfigurationSection section = levelsSection.getConfigurationSection("f" + lvl);
            if (section == null) {
                return defaults;
            }
            return new TornadoStats(
                    lvl,
                    section.getDouble("base-radius", defaults.baseRadius),
                    section.getDouble("height", defaults.height),
                    section.getDouble("speed", defaults.speed),
                    section.getDouble("hit-radius", defaults.hitRadius),
                    section.getDouble("rip-radius", defaults.ripRadius),
                    section.getDouble("spin-speed", defaults.spinSpeed),
                    Math.max(1, section.getInt("max-blocks", defaults.maxBlocks)),
                    Math.max(0.1, section.getDouble("damage-multiplier", defaults.damageMultiplier)),
                    Math.max(0.2, section.getDouble("knockback", defaults.knockback)));
        }

        private static TornadoStats defaultsFor(int lvl) {
            return switch (lvl) {
                case 1 -> new TornadoStats(1, 1.15, 6.0, 0.11, 1.35, 2.2, 0.18, 5, 1.0, 0.75);
                case 2 -> new TornadoStats(2, 1.55, 7.5, 0.14, 1.7, 2.8, 0.22, 9, 1.35, 0.95);
                case 3 -> new TornadoStats(3, 2.05, 9.0, 0.17, 2.15, 3.5, 0.26, 14, 1.8, 1.15);
                case 4 -> new TornadoStats(4, 2.65, 11.0, 0.20, 2.7, 4.3, 0.30, 19, 2.3, 1.4);
                default -> new TornadoStats(5, 3.35, 13.0, 0.24, 3.4, 5.2, 0.35, 26, 2.9, 1.7);
            };
        }
    }

    private static final class OrbitBlock {
        private final ItemDisplay display;
        private double angle;
        private double height;
        private double orbitFactor;
        private final double spinRate;
        private double climbSpeed;
        private final double heightOscSpeed;
        private final double heightOscAmp;
        private final double radiusOscSpeed;
        private final double radiusOscAmp;
        private final double swirlSpeed;
        private final double phase;
        private float tumbleYaw;
        private float tumblePitch;
        private float tumbleRoll;
        private final float tumbleYawRate;
        private final float tumblePitchRate;
        private final float tumbleRollRate;

        private OrbitBlock(
                ItemDisplay display,
                double angle,
                double height,
                double orbitFactor,
                double spinRate,
                double climbSpeed,
                double heightOscSpeed,
                double heightOscAmp,
                double radiusOscSpeed,
                double radiusOscAmp,
                double swirlSpeed,
                double phase,
                float tumbleYawRate,
                float tumblePitchRate,
                float tumbleRollRate) {
            this.display = display;
            this.angle = angle;
            this.height = height;
            this.orbitFactor = orbitFactor;
            this.spinRate = spinRate;
            this.climbSpeed = climbSpeed;
            this.heightOscSpeed = heightOscSpeed;
            this.heightOscAmp = heightOscAmp;
            this.radiusOscSpeed = radiusOscSpeed;
            this.radiusOscAmp = radiusOscAmp;
            this.swirlSpeed = swirlSpeed;
            this.phase = phase;
            this.tumbleYaw = (float) (Math.random() * Math.PI * 2.0);
            this.tumblePitch = (float) ((Math.random() - 0.5) * 0.8);
            this.tumbleRoll = (float) (Math.random() * Math.PI * 2.0);
            this.tumbleYawRate = tumbleYawRate;
            this.tumblePitchRate = tumblePitchRate;
            this.tumbleRollRate = tumbleRollRate;
        }

        private static OrbitBlock create(
                ItemDisplay display, double angle, double height, double orbitFactor, TornadoStats stats) {
            double spin = stats.spinSpeed * (0.2 + Math.random() * 2.4);
            if (Math.random() < 0.22) {
                spin *= -0.75; 
            }
            double climb = (0.02 + Math.random() * 0.07) * (Math.random() < 0.4 ? -1.0 : 1.0);
            return new OrbitBlock(
                    display,
                    angle + Math.random() * 0.8,
                    height,
                    orbitFactor * (0.7 + Math.random() * 0.7),
                    spin,
                    climb,
                    0.1 + Math.random() * 0.35,
                    0.15 + Math.random() * 0.7,
                    0.06 + Math.random() * 0.22,
                    0.1 + Math.random() * 0.4,
                    0.08 + Math.random() * 0.28,
                    Math.random() * Math.PI * 2.0,
                    (float) ((0.1 + Math.random() * 0.4) * (Math.random() < 0.5 ? -1 : 1)),
                    (float) ((0.05 + Math.random() * 0.22) * (Math.random() < 0.5 ? -1 : 1)),
                    (float) ((0.08 + Math.random() * 0.3) * (Math.random() < 0.5 ? -1 : 1)));
        }
    }

    private static final class ThrownBlock {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector velocity;
        private final int fuseTicks;
        private int age;

        private ThrownBlock(ItemDisplay display, Location loc, Vector velocity, int fuseTicks) {
            this.display = display;
            this.loc = loc.clone();
            this.velocity = velocity.clone();
            this.fuseTicks = fuseTicks;
        }
    }
}
