package com.monkey.ktplus.effects.list.tornado.animation;

import com.monkey.ktplus.effects.list.tornado.animation.TornadoOrbit.OrbitBlock;
import com.monkey.ktplus.effects.list.tornado.animation.TornadoOrbit.ThrownBlock;
import com.monkey.ktplus.effects.list.tornado.animation.util.TornadoDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class TornadoCombat {
    private TornadoCombat() {}

    static void hitPlayer(
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

        visuals.particle(
                "CLOUD", target.getLocation().add(0, 1, 0), ParticleScale.scale(18), 0.5, 0.5, 0.5, 0.08, null);
        visuals.dust(target.getLocation().add(0, 1, 0), TornadoFunnel.DUST_DIRT, 1.4f, 10, 0.35, 0.35, 0.35, 0.0);
        visuals.sound("ENTITY_PLAYER_HURT", target.getLocation(), 1.0f, 0.9f);
        visuals.sound("ENTITY_BREEZE_JUMP", core, 1.0f, 0.8f);
    }

    static void tryLevelUp(
            EffectSession session,
            VisualEffectService visuals,
            Location core,
            AtomicInteger level,
            AtomicInteger huntLeft,
            int huntTicks,
            ConfigurationSection levelsSection,
            List<OrbitBlock> orbit,
            List<ThrownBlock> thrown,
            Set<String> rippedKeys,
            int fuseMin,
            int fuseMax) {
        int lvl = level.get();
        if (lvl < TornadoStats.MAX_LEVEL) {
            level.incrementAndGet();
            visuals.sound("ENTITY_WIND_CHARGE_THROW", core, 1.5f, 0.7f + lvl * 0.08f);
            visuals.sound("ENTITY_BREEZE_SHOOT", core, 1.1f, 0.85f);
            visuals.sound("ENTITY_GENERIC_EXPLODE", core, 0.7f, 1.4f);
            huntLeft.set(huntTicks);
            session.resetDeadline(huntTicks + 40L);
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

    static void tryRipBlocks(
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
        int need = orbitFull
                ? Math.min(2, 1 + stats.level / 2)
                : Math.min(2 + stats.level / 2, stats.maxBlocks - orbit.size());
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
                double orbitR = 0.45 + Math.random() * 0.85;
                orbit.add(OrbitBlock.create(display, angle, height, orbitR, stats));
            } else {
                double angle = Math.random() * Math.PI * 2.0;
                double speed = 0.45 + Math.random() * 0.55 + stats.level * 0.06;
                Vector velocity = new Vector(
                        Math.cos(angle) * speed, 0.35 + Math.random() * 0.55, Math.sin(angle) * speed);
                int fuse = fuseMin + (int) (Math.random() * Math.max(1, fuseMax - fuseMin));
                thrown.add(new ThrownBlock(display, spawnAt.clone(), velocity, fuse));
                visuals.sound("ENTITY_WIND_CHARGE_THROW", spawnAt, 0.7f, 1.15f + (float) (Math.random() * 0.3));
            }
            need--;
        }
    }

    static void dissipate(
            VisualEffectService visuals,
            Location core,
            List<OrbitBlock> orbit,
            List<ThrownBlock> thrown,
            Player killer,
            boolean force) {
        TornadoFunnel.stopTornadoSounds(core);
        visuals.sound("BLOCK_SAND_BREAK", core, 1.0f, 0.7f);
        visuals.sound("ENTITY_GENERIC_EXTINGUISH_FIRE", core, 0.7f, 0.65f);
        visuals.particle("CLOUD", core.clone().add(0, 2, 0), ParticleScale.scale(35), 1.2, 1.5, 1.2, 0.05, null);
        visuals.dust(core.clone().add(0, 1.5, 0), TornadoFunnel.DUST_MID, 1.5f, 20, 0.8, 1.0, 0.8, 0.0);
        for (OrbitBlock ob : orbit) {
            if (ob.display != null && ob.display.isValid()) {
                visuals.dust(ob.display.getLocation(), TornadoFunnel.DUST_DIRT, 1.0f, 4, 0.15, 0.15, 0.15, 0.0);
            }
            TornadoDisplays.remove(ob.display);
        }
        orbit.clear();
        TornadoOrbit.releaseThrown(thrown);
        PerkActionBar.clear(killer);
        if (force) {
            visuals.sound("ENTITY_GENERIC_EXTINGUISH_FIRE", core, 0.55f, 0.5f);
        }
        TornadoFunnel.stopTornadoSounds(core);
    }
}
