package com.monkey.ktplus.effects.list.tornado.animation;

import com.monkey.ktplus.effects.list.tornado.animation.util.TornadoDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class TornadoOrbit {
    private TornadoOrbit() {}

    static void updateOrbit(List<OrbitBlock> orbit, Location core, TornadoStats stats, int tick) {
        double maxH = Math.max(1.0, stats.height);
        Iterator<OrbitBlock> it = orbit.iterator();
        while (it.hasNext()) {
            OrbitBlock ob = it.next();
            if (ob.display == null || !ob.display.isValid() || ob.display.isDead()) {
                it.remove();
                continue;
            }

            ob.angle += ob.spinRate;
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

            Location at = core.clone()
                    .add(
                            Math.cos(ob.angle) * r + Math.cos(ob.angle + Math.PI * 0.5) * lean,
                            ob.height + bob,
                            Math.sin(ob.angle) * r + Math.sin(ob.angle + Math.PI * 0.5) * lean);

            ob.tumbleYaw += ob.tumbleYawRate;
            ob.tumblePitch += ob.tumblePitchRate;
            ob.tumbleRoll += ob.tumbleRollRate;
            float scale = (float)
                    (0.7 + heightFrac * 0.35 + stats.level * 0.04 + Math.sin(tick * 0.15 + ob.phase) * 0.05);
            TornadoDisplays.place(ob.display, at, ob.tumbleYaw, ob.tumblePitch, ob.tumbleRoll, scale);
        }
    }

    static void updateThrown(
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
            visuals.dust(tb.loc, TornadoFunnel.DUST_DIRT, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);

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
                    TornadoCombat.tryLevelUp(
                            session,
                            visuals,
                            core,
                            level,
                            huntLeft,
                            huntTicks,
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

    static boolean explodeThrown(
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
        visuals.dust(at, TornadoFunnel.DUST_DIRT, 1.5f, 12, 0.35, 0.3, 0.35, 0.0);
        visuals.dust(at, TornadoFunnel.DUST_MID, 1.1f, 8, 0.4, 0.25, 0.4, 0.0);

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

    static void releaseOrbit(List<OrbitBlock> orbit) {
        for (OrbitBlock ob : orbit) {
            TornadoDisplays.remove(ob.display);
        }
        orbit.clear();
    }

    static void releaseThrown(List<ThrownBlock> thrown) {
        for (ThrownBlock tb : thrown) {
            TornadoDisplays.remove(tb.display);
        }
        thrown.clear();
    }

    static final class OrbitBlock {
        final ItemDisplay display;
        double angle;
        double height;
        final double orbitFactor;
        final double spinRate;
        double climbSpeed;
        final double heightOscSpeed;
        final double heightOscAmp;
        final double radiusOscSpeed;
        final double radiusOscAmp;
        final double swirlSpeed;
        final double phase;
        float tumbleYaw;
        float tumblePitch;
        float tumbleRoll;
        final float tumbleYawRate;
        final float tumblePitchRate;
        final float tumbleRollRate;

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

        static OrbitBlock create(
                ItemDisplay display, double angle, double height, double orbitFactor, TornadoStats stats) {
            double spin = stats.spinSpeed * (0.45 + Math.random() * 1.35);
            if (Math.random() < 0.12) {
                spin *= -0.55;
            }
            double climb = (0.015 + Math.random() * 0.055) * (Math.random() < 0.35 ? -1.0 : 1.0);
            return new OrbitBlock(
                    display,
                    angle,
                    height,
                    orbitFactor,
                    spin,
                    climb,
                    0.08 + Math.random() * 0.22,
                    0.12 + Math.random() * 0.45,
                    0.05 + Math.random() * 0.16,
                    0.08 + Math.random() * 0.28,
                    0.06 + Math.random() * 0.18,
                    Math.random() * Math.PI * 2.0,
                    (float) ((0.08 + Math.random() * 0.28) * (Math.random() < 0.5 ? -1 : 1)),
                    (float) ((0.04 + Math.random() * 0.16) * (Math.random() < 0.5 ? -1 : 1)),
                    (float) ((0.06 + Math.random() * 0.22) * (Math.random() < 0.5 ? -1 : 1)));
        }
    }

    static final class ThrownBlock {
        final ItemDisplay display;
        final Location loc;
        final Vector velocity;
        final int fuseTicks;
        int age;

        ThrownBlock(ItemDisplay display, Location loc, Vector velocity, int fuseTicks) {
            this.display = display;
            this.loc = loc.clone();
            this.velocity = velocity.clone();
            this.fuseTicks = fuseTicks;
        }
    }
}
