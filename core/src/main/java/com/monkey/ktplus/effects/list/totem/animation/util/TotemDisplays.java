package com.monkey.ktplus.effects.list.totem.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class TotemDisplays {
    private static final int INTERPOLATION_TICKS = 2;
    public static final float MAIN_SCALE = 1.0f;
    public static final float SMALL_SCALE = 1.0f;

    private TotemDisplays() {}

    public static @Nullable ItemDisplay spawnMain(Location location) {
        return spawn(location, MAIN_SCALE * 0.15f);
    }

    public static @Nullable ItemDisplay spawnSmall(Location location) {
        return spawn(location, SMALL_SCALE * 0.15f);
    }

    public static void place(ItemDisplay display, Location at, float scale, float yawRadians) {
        place(display, at, scale, yawRadians, 0.0f);
    }

    public static void place(ItemDisplay display, Location at, float scale, float yawRadians, float pitchRadians) {
        place(display, at, scale, yawRadians, pitchRadians, 0.0f);
    }

    public static void place(
            ItemDisplay display, Location at, float scale, float yawRadians, float pitchRadians, float rollRadians) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safeScale = Math.max(0.05f, scale);
        Quaternionf rotation = new Quaternionf().rotateY(yawRadians).rotateX(pitchRadians).rotateZ(rollRadians);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(0.0f, 0.35f * safeScale, 0.0f),
                rotation,
                new Vector3f(safeScale, safeScale, safeScale),
                new Quaternionf()));
    }

    public static void remove(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    public static void removeAll(Iterable<ItemDisplay> displays) {
        for (ItemDisplay display : displays) {
            remove(display);
        }
    }

    private static @Nullable ItemDisplay spawn(Location location, float startScale) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float scale = Math.max(0.05f, startScale);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.TOTEM_OF_UNDYING));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(64.0f);
            display.setShadowRadius(0.2f);
            display.setShadowStrength(0.5f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(0.0f, 0.35f * scale, 0.0f),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
        });
    }
}
