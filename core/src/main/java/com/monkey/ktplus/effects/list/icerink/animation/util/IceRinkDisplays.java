package com.monkey.ktplus.effects.list.icerink.animation.util;

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

public final class IceRinkDisplays {
    private static final int INTERPOLATION_TICKS = 2;

    private IceRinkDisplays() {}

    public static Material tileMaterial(int index) {
        return switch (index % 3) {
            case 0 -> MaterialResolver.resolve("PACKED_ICE", "ICE");
            case 1 -> MaterialResolver.resolve("BLUE_ICE", "PACKED_ICE");
            default -> MaterialResolver.resolve("ICE", "LIGHT_BLUE_STAINED_GLASS");
        };
    }

    public static @Nullable ItemDisplay spawnTile(Location location, Material material) {
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
            display.setViewRange(56.0f);
            display.setShadowRadius(0.05f);
            display.setShadowStrength(0.15f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(0.95f, 0.12f, 0.95f),
                    new Quaternionf()));
        });
    }

    public static void place(@Nullable ItemDisplay display, Location at, float yaw, float scaleX, float scaleY) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float sx = Math.max(0.2f, scaleX);
        float sy = Math.max(0.05f, scaleY);
        Quaternionf rotation = new Quaternionf().rotateY(yaw);
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
