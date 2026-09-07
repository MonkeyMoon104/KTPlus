package com.monkey.ktplus.effects.list.glowmissile.animation.util;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.MaterialCompat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class GlowMissileDebris {
    private static final double GRAVITY = 0.055;
    private static final double HIT_RADIUS = 1.25;
    private static final int MAX_LIFE = 55;

    private GlowMissileDebris() {}

    public static void cascade(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            UUID victimId,
            Map<Location, Material> frameBlocks,
            Location explosionCenter,
            double damage,
            double impactRadius,
            Runnable onComplete) {
        World world = explosionCenter.getWorld();
        if (world == null || frameBlocks.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        List<DebrisPiece> pieces = new ArrayList<>(frameBlocks.size());
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (Map.Entry<Location, Material> entry : frameBlocks.entrySet()) {
            Material placed = entry.getKey().getBlock().getType();
            Material visual = !MaterialCompat.isAir(placed) ? placed : entry.getValue();
            if (visual == null || MaterialCompat.isAir(visual)) {
                visual = Material.GLOWSTONE;
            }
            Location spawnAt = entry.getKey().clone().add(0.5, 0.5, 0.5);
            ItemDisplay display = spawnDebris(spawnAt, visual);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            Vector away = spawnAt.toVector().subtract(explosionCenter.toVector());
            if (away.lengthSquared() < 0.01) {
                away = new Vector(rng.nextGaussian(), 0.0, rng.nextGaussian());
            }
            away.setY(0.0);
            if (away.lengthSquared() < 0.01) {
                away = new Vector(1.0, 0.0, 0.0);
            }
            away.normalize();
            Vector velocity = away.multiply(0.12 + rng.nextDouble() * 0.28)
                    .setY(0.05 + rng.nextDouble() * 0.18);
            pieces.add(new DebrisPiece(display, spawnAt.clone(), velocity, visual));
        }

        GlowMissileBlocks.clearBlocks(session, killer, frameBlocks);

        if (pieces.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        visuals.sound("ENTITY_GENERIC_EXPLODE", explosionCenter, 0.7f, 1.35f);
        visuals.sound("BLOCK_GLOWSTONE_BREAK", explosionCenter, 1.1f, 0.85f);

        session.onCleanup(() -> {
            for (DebrisPiece piece : pieces) {
                remove(piece.display);
            }
            pieces.clear();
        });

        boolean[] done = {false};
        session.runTimer(0L, 1L, () -> {
            if (!session.active() || done[0]) {
                for (DebrisPiece piece : pieces) {
                    remove(piece.display);
                }
                pieces.clear();
                return false;
            }

            Iterator<DebrisPiece> it = pieces.iterator();
            while (it.hasNext()) {
                DebrisPiece piece = it.next();
                piece.age++;
                piece.velocity.setY(piece.velocity.getY() - GRAVITY);
                piece.loc.add(piece.velocity);
                float spin = piece.age * 0.4f;
                place(piece.display, piece.loc, spin, spin * 0.55f, spin * 0.35f, 0.72f);
                if (piece.age % 2 == 0) {
                    visuals.particle("BLOCK", piece.loc, 2, 0.05, 0.05, 0.05, 0.0, null);
                    visuals.blockParticle("BLOCK", piece.loc, 1, 0.0, 0.0, 0.0, 0.0, piece.material);
                }

                boolean hitGround = piece.loc.getBlock().getRelative(0, -1, 0).getType().isSolid()
                        && piece.velocity.getY() <= 0.0
                        && piece.loc.getY() - piece.loc.getBlockY() < 0.55;
                boolean hitPlayer = damagePlayers(
                        session, visuals, world, killer, victimId, piece.loc, damage, impactRadius);
                boolean expired = piece.age >= MAX_LIFE;
                if (hitGround || hitPlayer || expired) {
                    impact(visuals, piece.loc, piece.material);
                    if (hitGround && !hitPlayer) {
                        damagePlayers(session, visuals, world, killer, victimId, piece.loc, damage, impactRadius);
                    }
                    remove(piece.display);
                    it.remove();
                }
            }

            if (pieces.isEmpty()) {
                done[0] = true;
                if (onComplete != null) {
                    onComplete.run();
                }
                return false;
            }
            return true;
        });
    }

    private static boolean damagePlayers(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location at,
            double damage,
            double radius) {
        if (killer == null || damage <= 0.0) {
            return false;
        }
        double hitSq = Math.max(HIT_RADIUS, radius) * Math.max(HIT_RADIUS, radius);
        boolean any = false;
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
            if (player.getLocation().clone().add(0, 0.9, 0).distanceSquared(at) > hitSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            player.damage(damage, killer);
            Vector knock = player.getLocation().toVector().subtract(at.toVector());
            if (knock.lengthSquared() > 1.0e-4) {
                player.setVelocity(player.getVelocity().add(knock.normalize().multiply(0.28).setY(0.18)));
            }
            visuals.sound("ENTITY_PLAYER_HURT", at, 0.7f, 0.9f);
            any = true;
        }
        return any;
    }

    private static void impact(VisualEffectService visuals, Location at, Material material) {
        visuals.sound("BLOCK_STONE_BREAK", at, 0.85f, 0.75f + (float) Math.random() * 0.3f);
        visuals.sound("BLOCK_GLOWSTONE_BREAK", at, 0.55f, 1.1f);
        visuals.blockParticle("BLOCK", at, ParticleScale.scale(10), 0.25, 0.2, 0.25, 0.05, material);
        visuals.particle("FLASH", at, 1, 0.0, 0.0, 0.0, 0.0, null);
    }

    private static @Nullable ItemDisplay spawnDebris(Location location, Material material) {
        if (location.getWorld() == null || material == null || MaterialCompat.isAir(material)) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(14, 14));
            display.setViewRange(72.0f);
            display.setShadowRadius(0.2f);
            display.setShadowStrength(0.4f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(1);
            display.setTeleportDuration(1);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(0.55f, 0.55f, 0.55f),
                    new Quaternionf()));
        });
    }

    private static void place(
            @Nullable ItemDisplay display, Location at, float yaw, float pitch, float roll, float scale) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(1);
        display.teleport(target);
        float safe = Math.max(0.2f, scale);
        display.setInterpolationDuration(1);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll),
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    private static void remove(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    private static final class DebrisPiece {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector velocity;
        private final Material material;
        private int age;

        private DebrisPiece(ItemDisplay display, Location loc, Vector velocity, Material material) {
            this.display = display;
            this.loc = loc;
            this.velocity = velocity;
            this.material = material;
        }
    }
}
