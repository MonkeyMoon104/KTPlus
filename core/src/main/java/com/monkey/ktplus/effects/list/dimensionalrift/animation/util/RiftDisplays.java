package com.monkey.ktplus.effects.list.dimensionalrift.animation.util;

import com.monkey.ktplus.util.compat.MaterialResolver;
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

public final class RiftDisplays {
    private static final int INTERPOLATION_TICKS = 2;

    private RiftDisplays() {}

    public static @Nullable BlockDisplay spawnPortalFrame(Location location) {
        Material material = MaterialResolver.resolve("CRYING_OBSIDIAN", "OBSIDIAN");
        return spawnBlock(location, material, 0.55f, 1.35f, 0.12f);
    }

    public static @Nullable BlockDisplay spawnPortalCore(Location location) {
        Material material = MaterialResolver.resolve("PURPLE_STAINED_GLASS", "PURPLE_CONCRETE");
        return spawnBlock(location, material, 0.85f, 1.1f, 0.85f);
    }

    public static @Nullable ItemDisplay spawnBolt(Location location) {
        Material material = MaterialResolver.resolve("ENDER_EYE", "ENDER_PEARL");
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.CENTER);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(80.0f);
            display.setShadowRadius(0.05f);
            display.setShadowStrength(0.2f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(1);
            display.setTeleportDuration(1);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(0.45f, 0.45f, 0.45f),
                    new Quaternionf()));
        });
    }

    public static void placeBlock(
            @Nullable BlockDisplay display, Location at, float scaleX, float scaleY, float scaleZ, float yaw) {
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
                new Vector3f(-scaleX * 0.5f, -scaleY * 0.5f, -scaleZ * 0.5f),
                new Quaternionf().rotateY(yaw),
                new Vector3f(Math.max(0.08f, scaleX), Math.max(0.08f, scaleY), Math.max(0.08f, scaleZ)),
                new Quaternionf()));
    }

    public static void placeBolt(@Nullable ItemDisplay display, Location at, float scale, float spin) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(1);
        display.teleport(target);
        float safe = Math.max(0.12f, scale);
        display.setInterpolationDuration(1);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateY(spin).rotateX(spin * 0.6f),
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void removeBlock(@Nullable BlockDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    public static void removeItem(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    private static @Nullable BlockDisplay spawnBlock(
            Location location, @Nullable Material material, float sx, float sy, float sz) {
        if (location.getWorld() == null || material == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(80.0f);
            display.setShadowRadius(0.2f);
            display.setShadowStrength(0.35f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(-sx * 0.5f, -sy * 0.5f, -sz * 0.5f),
                    new Quaternionf(),
                    new Vector3f(sx, sy, sz),
                    new Quaternionf()));
        });
    }
}
