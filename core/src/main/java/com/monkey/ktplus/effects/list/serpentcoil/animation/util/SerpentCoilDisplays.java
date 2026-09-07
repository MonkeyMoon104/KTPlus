package com.monkey.ktplus.effects.list.serpentcoil.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class SerpentCoilDisplays {
    private static final int INTERPOLATION_TICKS = 2;
    private static final Material[] BODY = {
        Material.GREEN_CONCRETE,
        Material.LIME_CONCRETE,
        Material.GREEN_TERRACOTTA,
        Material.MOSS_BLOCK
    };

    private SerpentCoilDisplays() {}

    public static @Nullable BlockDisplay spawnSegment(Location location, int index) {
        if (location.getWorld() == null) {
            return null;
        }
        Material mat = BODY[index % BODY.length];
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float scale = index == 0 ? 0.55f : 0.42f - Math.min(0.18f, index * 0.012f);
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(mat.createBlockData());
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(12, 12));
            display.setViewRange(80.0f);
            display.setShadowRadius(0.25f);
            display.setShadowStrength(0.55f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
        });
    }

    public static void place(
            @Nullable BlockDisplay display, Location at, float scale, float yaw, float pitch) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.12f, scale);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateY(yaw).rotateX(pitch),
                new Vector3f(safe, safe * 0.85f, safe),
                new Quaternionf()));
    }

    public static void remove(@Nullable Display display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }
}
