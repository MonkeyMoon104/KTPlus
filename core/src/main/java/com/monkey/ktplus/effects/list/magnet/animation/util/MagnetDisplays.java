package com.monkey.ktplus.effects.list.magnet.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class MagnetDisplays {
    private static final int INTERPOLATION_TICKS = 2;

    private MagnetDisplays() {}

    public static @Nullable ItemDisplay spawnCore(Location location) {
        return spawn(location, Material.LODESTONE, 0.22f, 0.35f);
    }

    public static @Nullable ItemDisplay spawnRingPiece(Location location, Material material, float scale) {
        return spawn(location, material, Math.max(0.08f, scale), 0.15f);
    }

    public static void place(
            @Nullable ItemDisplay display,
            Location at,
            float scale,
            float yawRadians,
            float pitchRadians,
            float rollRadians) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.05f, scale);
        Quaternionf rotation = new Quaternionf()
                .rotateY(yawRadians)
                .rotateX(pitchRadians)
                .rotateZ(rollRadians);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(0.0f, 0.2f * safe, 0.0f),
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

    private static @Nullable ItemDisplay spawn(
            Location location, Material material, float startScale, float shadow) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float scale = Math.max(0.05f, startScale);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(14, 14));
            display.setViewRange(64.0f);
            display.setShadowRadius(shadow);
            display.setShadowStrength(0.55f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(0.0f, 0.2f * scale, 0.0f),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
        });
    }
}
