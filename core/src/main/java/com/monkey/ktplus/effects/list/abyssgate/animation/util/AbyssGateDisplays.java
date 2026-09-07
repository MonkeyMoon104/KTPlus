package com.monkey.ktplus.effects.list.abyssgate.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class AbyssGateDisplays {
    private static final int INTERPOLATION_TICKS = 2;

    private AbyssGateDisplays() {}

    public static @Nullable BlockDisplay spawnPillar(Location location) {
        return spawnBlock(location, Material.OBSIDIAN, 0.55f, 1.2f, 0.55f);
    }

    public static @Nullable BlockDisplay spawnFrame(Location location) {
        return spawnBlock(location, Material.CRYING_OBSIDIAN, 0.7f, 0.35f, 0.7f);
    }

    public static @Nullable BlockDisplay spawnTentacle(Location location, int index) {
        Material mat = index % 2 == 0 ? Material.BLACK_CONCRETE : Material.PURPLE_CONCRETE;
        return spawnBlock(location, mat, 0.28f, 0.28f, 0.28f);
    }

    public static void place(
            @Nullable BlockDisplay display,
            Location at,
            float scaleX,
            float scaleY,
            float scaleZ,
            float yaw,
            float pitch) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateY(yaw).rotateX(pitch),
                new Vector3f(Math.max(0.1f, scaleX), Math.max(0.1f, scaleY), Math.max(0.1f, scaleZ)),
                new Quaternionf()));
    }

    public static void remove(@Nullable Display display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    private static @Nullable BlockDisplay spawnBlock(
            Location location, Material material, float sx, float sy, float sz) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(12, 12));
            display.setViewRange(96.0f);
            display.setShadowRadius(0.3f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(sx, sy, sz),
                    new Quaternionf()));
        });
    }
}
