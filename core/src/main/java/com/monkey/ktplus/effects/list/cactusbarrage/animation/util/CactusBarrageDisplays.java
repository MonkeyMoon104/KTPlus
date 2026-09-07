package com.monkey.ktplus.effects.list.cactusbarrage.animation.util;

import com.monkey.ktplus.util.compat.MaterialResolver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class CactusBarrageDisplays {
    private static final int INTERPOLATION_TICKS = 1;

    private CactusBarrageDisplays() {}

    public static Material cactusMaterial() {
        return MaterialResolver.resolve("CACTUS", "GREEN_WOOL");
    }

    public static @Nullable ItemDisplay spawn(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        Material cactus = cactusMaterial();
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(cactus));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(12, 12));
            display.setViewRange(56.0f);
            display.setShadowRadius(0.1f);
            display.setShadowStrength(0.3f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(0.4f, 0.55f, 0.4f),
                    new Quaternionf()));
        });
    }

    public static void place(
            @Nullable ItemDisplay display, Location at, float yaw, float pitch, float roll, float scaleX, float scaleY) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float sx = Math.max(0.12f, scaleX);
        float sy = Math.max(0.12f, scaleY);
        Quaternionf rotation = new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                rotation,
                new Vector3f(sx, sy, sx),
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
}
