package com.monkey.ktplus.effects.list.quillstorm.animation.util;

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

public final class QuillStormDisplays {
    private static final int INTERPOLATION_TICKS = 1;

    private QuillStormDisplays() {}

    public static Material quillMaterial(int index) {
        return switch (index % 3) {
            case 0 -> MaterialResolver.resolve("ARROW", "FEATHER");
            case 1 -> MaterialResolver.resolve("SPECTRAL_ARROW", "ARROW");
            default -> MaterialResolver.resolve("FEATHER", "STICK");
        };
    }

    public static @Nullable ItemDisplay spawn(Location location, Material material) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(13, 13));
            display.setViewRange(64.0f);
            display.setShadowRadius(0.08f);
            display.setShadowStrength(0.25f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(0.45f, 0.7f, 0.45f),
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
