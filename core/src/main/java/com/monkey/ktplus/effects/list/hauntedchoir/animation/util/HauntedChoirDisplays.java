package com.monkey.ktplus.effects.list.hauntedchoir.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class HauntedChoirDisplays {
    private static final int INTERPOLATION_TICKS = 2;

    private HauntedChoirDisplays() {}

    public static @Nullable ItemDisplay spawnSkull(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.SKELETON_SKULL));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(14, 14));
            display.setViewRange(72.0f);
            display.setShadowRadius(0.35f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            float s = 0.85f;
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(s, s, s),
                    new Quaternionf()));
        });
    }

    public static void place(
            @Nullable ItemDisplay display, Location at, float scale, float yaw, float pitch) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.35f, scale);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateY(yaw).rotateX(pitch),
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void remove(@Nullable Display display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }
}
