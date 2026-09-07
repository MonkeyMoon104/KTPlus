package com.monkey.ktplus.effects.list.crystalspire.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class CrystalSpireDisplays {
    private static final int INTERPOLATION_TICKS = 2;

    private CrystalSpireDisplays() {}

    public static @Nullable BlockDisplay spawnSpire(Location location, Material material) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(72.0f);
            display.setShadowRadius(0.35f);
            display.setShadowStrength(0.5f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(0.2f, 0.2f, 0.2f),
                    new Quaternionf()));
        });
    }

    public static void placeSpire(
            @Nullable BlockDisplay display, Location at, float yaw, float scaleX, float scaleY, float scaleZ) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf().rotateY(yaw),
                new Vector3f(Math.max(0.15f, scaleX), Math.max(0.15f, scaleY), Math.max(0.15f, scaleZ)),
                new Quaternionf()));
    }

    public static @Nullable ItemDisplay spawnShard(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.AMETHYST_SHARD));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.CENTER);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(64.0f);
            display.setShadowRadius(0.08f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            float s = 0.35f;
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(s, s, s),
                    new Quaternionf()));
        });
    }

    public static void placeShard(
            @Nullable ItemDisplay display, Location at, float yaw, float pitch, float roll, float scale) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.2f, scale);
        Quaternionf rotation = new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                rotation,
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void remove(@Nullable Display display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }
}
