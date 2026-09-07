package com.monkey.ktplus.effects.list.judgment.animation.util;

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

public final class JudgmentDisplays {
    private static final int INTERPOLATION_TICKS = 1;

    private JudgmentDisplays() {}

    public static @Nullable BlockDisplay spawnPlate(Location location, Material material, float scale) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float safe = Math.max(0.6f, scale);
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(140.0f);
            display.setShadowRadius(2.5f);
            display.setShadowStrength(1.0f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setGlowColorOverride(org.bukkit.Color.fromRGB(255, 215, 60));
            display.setGlowing(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(safe, 0.35f, safe),
                    new Quaternionf()));
        });
    }

    public static @Nullable ItemDisplay spawnBeam(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.GOLD_INGOT));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(120.0f);
            display.setShadowRadius(0.4f);
            display.setShadowStrength(0.5f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(0.4f, 4.0f, 0.4f),
                    new Quaternionf()));
        });
    }

    public static void placePlate(
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
                new Vector3f(),
                new Quaternionf().rotateY(yaw),
                new Vector3f(Math.max(0.4f, scaleX), Math.max(0.15f, scaleY), Math.max(0.4f, scaleZ)),
                new Quaternionf()));
    }

    public static void placeBeam(@Nullable ItemDisplay display, Location at, float height, float yaw) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float h = Math.max(1.0f, height);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(0.0f, h * 0.5f, 0.0f),
                new Quaternionf().rotateY(yaw),
                new Vector3f(0.35f, h, 0.35f),
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
}
