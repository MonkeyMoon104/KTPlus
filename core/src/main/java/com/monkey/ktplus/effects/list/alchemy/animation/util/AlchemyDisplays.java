package com.monkey.ktplus.effects.list.alchemy.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class AlchemyDisplays {
    private static final int INTERPOLATION_TICKS = 2;

    private AlchemyDisplays() {}

    public static @Nullable ItemDisplay spawnCauldron(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.CAULDRON));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(64.0f);
            display.setShadowRadius(0.5f);
            display.setShadowStrength(0.55f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            float s = 0.3f;
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0.15f, 0),
                    new Quaternionf(),
                    new Vector3f(s, s, s),
                    new Quaternionf()));
        });
    }

    public static void place(@Nullable ItemDisplay display, Location at, float yaw, float scale) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.5f, scale);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(0, 0.35f * safe, 0),
                new Quaternionf().rotateY(yaw),
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void remove(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }
}
