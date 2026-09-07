package com.monkey.ktplus.effects.list.vinegrasp.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class VineGraspDisplays {
    private static final int INTERPOLATION_TICKS = 2;

    private VineGraspDisplays() {}

    public static @Nullable BlockDisplay spawn(Location location, Material material, float scale) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float safe = Math.max(0.15f, scale);
        Material block = material.isBlock() ? material : Material.VINE;
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(block.createBlockData());
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(12, 12));
            display.setViewRange(64.0f);
            display.setShadowRadius(0.18f);
            display.setShadowStrength(0.35f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(-0.5f * safe, 0.0f, -0.5f * safe),
                    new Quaternionf(),
                    new Vector3f(safe, safe, safe),
                    new Quaternionf()));
        });
    }

    public static void place(
            @Nullable BlockDisplay display,
            Location at,
            float scaleX,
            float scaleY,
            float scaleZ,
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
        float sx = Math.max(0.1f, scaleX);
        float sy = Math.max(0.1f, scaleY);
        float sz = Math.max(0.1f, scaleZ);
        Quaternionf rotation = new Quaternionf()
                .rotateY(yawRadians)
                .rotateX(pitchRadians)
                .rotateZ(rollRadians);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(-0.5f * sx, 0.0f, -0.5f * sz),
                rotation,
                new Vector3f(sx, sy, sz),
                new Quaternionf()));
    }

    public static void remove(@Nullable BlockDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    public static void removeAll(Iterable<BlockDisplay> displays) {
        for (BlockDisplay display : displays) {
            remove(display);
        }
    }
}
