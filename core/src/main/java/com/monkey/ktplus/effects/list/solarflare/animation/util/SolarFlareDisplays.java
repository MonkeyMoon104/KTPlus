package com.monkey.ktplus.effects.list.solarflare.animation.util;

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

public final class SolarFlareDisplays {
    private static final int INTERPOLATION_TICKS = 1;

    private SolarFlareDisplays() {}

    public static Material coreMaterial() {
        return MaterialResolver.resolve("GLOWSTONE", "GOLD_BLOCK");
    }

    public static Material ringMaterial(int index) {
        return switch (index % 3) {
            case 0 -> MaterialResolver.resolve("GOLD_NUGGET", "YELLOW_DYE");
            case 1 -> MaterialResolver.resolve("BLAZE_POWDER", "ORANGE_DYE");
            default -> MaterialResolver.resolve("GLOWSTONE_DUST", "GOLD_INGOT");
        };
    }

    public static @Nullable ItemDisplay spawn(Location location, Material material, float scale) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float safe = Math.max(0.12f, scale);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(72.0f);
            display.setShadowRadius(0.1f);
            display.setShadowStrength(0.2f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(safe, safe, safe),
                    new Quaternionf()));
        });
    }

    public static void place(
            @Nullable ItemDisplay display, Location at, float yaw, float pitch, float roll, float scale) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.12f, scale);
        Quaternionf rotation = new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                rotation,
                new Vector3f(safe, safe, safe),
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
