package com.monkey.ktplus.effects.list.sniper.animation.util;

import com.monkey.ktplus.util.compat.MaterialResolver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class SniperDisplays {
    private static final int INTERPOLATION_TICKS = 2;
    private static final float SCALE = 1.15f;

    private SniperDisplays() {}

    public static @Nullable ItemDisplay spawnTurret(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        Material rod = MaterialResolver.resolve("LIGHTNING_ROD", "BLAZE_ROD");
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(rod));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(96.0f);
            display.setShadowRadius(0.2f);
            display.setShadowStrength(0.45f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    tipRotation(new Vector(0.0, 1.0, 0.0)),
                    new Vector3f(SCALE, SCALE, SCALE),
                    new Quaternionf()));
        });
    }

    public static void place(@Nullable ItemDisplay turret, Location at, Vector direction) {
        if (turret == null || !turret.isValid() || turret.isDead() || at.getWorld() == null) {
            return;
        }
        Vector dir = direction.clone();
        if (dir.lengthSquared() < 1.0e-8) {
            dir = new Vector(0.0, 1.0, 0.0);
        } else {
            dir.normalize();
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        turret.setTeleportDuration(INTERPOLATION_TICKS);
        turret.teleport(target);
        turret.setInterpolationDuration(INTERPOLATION_TICKS);
        turret.setTransformation(new Transformation(
                new Vector3f(0.0f, 0.0f, 0.0f),
                tipRotation(dir),
                new Vector3f(SCALE, SCALE, SCALE),
                new Quaternionf()));
    }

    public static void place(@Nullable ItemDisplay turret, Location at, double yawRadians, double pitchRadians) {
        place(turret, at, directionFromYawPitch(yawRadians, pitchRadians));
    }

    public static void remove(@Nullable ItemDisplay turret) {
        if (turret != null && turret.isValid() && !turret.isDead()) {
            turret.remove();
        }
    }

    public static Vector directionFromYawPitch(double yaw, double pitch) {
        double cosPitch = Math.cos(pitch);
        return new Vector(-Math.sin(yaw) * cosPitch, -Math.sin(pitch), Math.cos(yaw) * cosPitch);
    }

    private static Quaternionf tipRotation(Vector direction) {
        Vector3f from = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f to = new Vector3f(
                (float) direction.getX(), (float) direction.getY(), (float) direction.getZ());
        if (to.lengthSquared() < 1.0e-8f) {
            to.set(0.0f, 1.0f, 0.0f);
        } else {
            to.normalize();
        }
        return new Quaternionf().rotationTo(from, to);
    }
}
