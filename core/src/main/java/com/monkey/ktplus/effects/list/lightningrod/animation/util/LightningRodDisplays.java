package com.monkey.ktplus.effects.list.lightningrod.animation.util;

import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.List;
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

public final class LightningRodDisplays {
    private static final int INTERPOLATION_TICKS = 2;
    private static final Material[] COPPER_SPIRAL = {
        MaterialResolver.resolve("COPPER_BLOCK", "ORANGE_CONCRETE"),
        MaterialResolver.resolve("CUT_COPPER", "COPPER_BLOCK"),
        MaterialResolver.resolve("EXPOSED_COPPER", "COPPER_BLOCK"),
        MaterialResolver.resolve("WEATHERED_COPPER", "COPPER_BLOCK")
    };

    private LightningRodDisplays() {}

    public static @Nullable ItemDisplay spawnRod(Location location) {
        Material rod = MaterialResolver.resolve("LIGHTNING_ROD", "BLAZE_ROD");
        return spawnItem(location, rod, 0.35f);
    }

    public static @Nullable ItemDisplay spawnCopperSpark(Location location) {
        Material copper = MaterialResolver.resolve("COPPER_INGOT", "GOLD_NUGGET");
        return spawnItem(location, copper, 0.18f);
    }

    public static @Nullable BlockDisplay spawnCopperSegment(Location location, int levelIndex) {
        if (location.getWorld() == null) {
            return null;
        }
        Material material = COPPER_SPIRAL[Math.floorMod(levelIndex, COPPER_SPIRAL.length)];
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float scale = 0.22f;
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(14, 14));
            display.setViewRange(72.0f);
            display.setShadowRadius(0.12f);
            display.setShadowStrength(0.35f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(-scale * 0.5f, -scale * 0.5f, -scale * 0.5f),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
        });
    }

    public static void placeRod(@Nullable ItemDisplay display, Location at, float scale, float yawRadians) {
        placeItem(display, at, scale, yawRadians, 0.0f, 0.0f);
    }

    public static void placeSpark(
            @Nullable ItemDisplay display, Location at, float scale, float yaw, float pitch, float roll) {
        placeItem(display, at, scale, yaw, pitch, roll);
    }

    public static void placeCopper(
            @Nullable BlockDisplay display, Location at, float scale, float yaw, float pitch) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.1f, scale);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(-safe * 0.5f, -safe * 0.5f, -safe * 0.5f),
                new Quaternionf().rotateY(yaw).rotateX(pitch),
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void remove(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    public static void removeBlock(@Nullable BlockDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    public static void removeAll(Iterable<ItemDisplay> displays) {
        for (ItemDisplay display : displays) {
            remove(display);
        }
    }

    public static void removeAllBlocks(List<BlockDisplay> displays) {
        for (BlockDisplay display : displays) {
            removeBlock(display);
        }
    }

    private static @Nullable ItemDisplay spawnItem(Location location, Material material, float startScale) {
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
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(72.0f);
            display.setShadowRadius(0.15f);
            display.setShadowStrength(0.4f);
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

    private static void placeItem(
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
}
