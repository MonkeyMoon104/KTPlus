package com.monkey.ktplus.effects.list.inksquid.animation.util;

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

public final class InkSquidDisplays {
    private static final int INTERPOLATION_TICKS = 2;

    private InkSquidDisplays() {}

    public static @Nullable ItemDisplay spawnBody(Location location) {
        return spawnItem(location, Material.INK_SAC, 0.55f, 0.3f);
    }

    public static @Nullable ItemDisplay spawnTentacleTip(Location location) {
        return spawnItem(location, Material.BLACK_DYE, 0.28f, 0.1f);
    }

    public static @Nullable BlockDisplay spawnTentacleSegment(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(Material.BLACK_CONCRETE.createBlockData());
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(10, 10));
            display.setViewRange(64.0f);
            display.setShadowRadius(0.15f);
            display.setShadowStrength(0.4f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(0.28f, 0.28f, 0.28f),
                    new Quaternionf()));
        });
    }

    public static void placeItem(
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
                new Vector3f(),
                rotation,
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void placeBlock(
            @Nullable BlockDisplay display,
            Location at,
            float scale,
            float yawRadians,
            float pitchRadians) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.08f, scale);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateY(yawRadians).rotateX(pitchRadians),
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void removeItem(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    public static void removeBlock(@Nullable BlockDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    private static @Nullable ItemDisplay spawnItem(
            Location location, Material material, float scale, float shadow) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float safe = Math.max(0.05f, scale);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(8, 8));
            display.setViewRange(64.0f);
            display.setShadowRadius(shadow);
            display.setShadowStrength(0.55f);
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
}
