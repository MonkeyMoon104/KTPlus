package com.monkey.ktplus.effects.visual.display;

import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Color;
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

public final class DisplayFactory {
    private static final int DEFAULT_INTERPOLATION = 1;

    private DisplayFactory() {}

    public static @Nullable ItemDisplay item(
            Location location, Material material, float scaleX, float scaleY, float scaleZ) {
        return item(location, new ItemStack(material), scaleX, scaleY, scaleZ, null);
    }

    public static @Nullable ItemDisplay item(
            Location location,
            ItemStack stack,
            float scaleX,
            float scaleY,
            float scaleZ,
            @Nullable Consumer<ItemDisplay> configure) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(stack, "stack");
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            applyDefaults(display);
            display.setItemStack(stack.clone());
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setTransformation(scaled(scaleX, scaleY, scaleZ));
            if (configure != null) {
                configure.accept(display);
            }
        });
    }

    public static @Nullable BlockDisplay block(
            Location location, Material material, float scaleX, float scaleY, float scaleZ) {
        return block(location, material, scaleX, scaleY, scaleZ, null);
    }

    public static @Nullable BlockDisplay block(
            Location location,
            Material material,
            float scaleX,
            float scaleY,
            float scaleZ,
            @Nullable Consumer<BlockDisplay> configure) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(material, "material");
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            applyDefaults(display);
            display.setBlock(material.createBlockData());
            display.setTransformation(scaled(scaleX, scaleY, scaleZ));
            if (configure != null) {
                configure.accept(display);
            }
        });
    }

    public static void glow(@Nullable Display display, Color color) {
        if (display == null || !display.isValid()) {
            return;
        }
        display.setGlowColorOverride(color);
        display.setGlowing(true);
    }

    public static void place(
            @Nullable Display display, Location at, float scaleX, float scaleY, float scaleZ, float yawRad) {
        place(display, at, scaleX, scaleY, scaleZ, yawRad, 0.0f);
    }

    public static void place(
            @Nullable Display display,
            Location at,
            float scaleX,
            float scaleY,
            float scaleZ,
            float yawRad,
            float pitchRad) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.teleport(target);
        display.setInterpolationDuration(DEFAULT_INTERPOLATION);
        display.setTeleportDuration(DEFAULT_INTERPOLATION);
        Quaternionf rotation = new Quaternionf().rotateY(yawRad).rotateX(pitchRad);
        display.setTransformation(new Transformation(
                new Vector3f(),
                rotation,
                new Vector3f(scaleX, scaleY, scaleZ),
                new Quaternionf()));
    }

    public static void scale(@Nullable Display display, float scaleX, float scaleY, float scaleZ) {
        if (display == null || !display.isValid() || display.isDead()) {
            return;
        }
        Transformation current = display.getTransformation();
        display.setInterpolationDuration(DEFAULT_INTERPOLATION);
        display.setTransformation(new Transformation(
                current.getTranslation(),
                current.getLeftRotation(),
                new Vector3f(scaleX, scaleY, scaleZ),
                current.getRightRotation()));
    }

    public static void remove(@Nullable Display display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    private static void applyDefaults(Display display) {
        display.setBillboard(Display.Billboard.FIXED);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setViewRange(96.0f);
        display.setShadowRadius(0.0f);
        display.setShadowStrength(0.0f);
        display.setPersistent(false);
        display.setInvulnerable(true);
        display.setInterpolationDuration(DEFAULT_INTERPOLATION);
        display.setTeleportDuration(DEFAULT_INTERPOLATION);
    }

    private static Transformation scaled(float scaleX, float scaleY, float scaleZ) {
        return new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(Math.max(0.01f, scaleX), Math.max(0.01f, scaleY), Math.max(0.01f, scaleZ)),
                new Quaternionf());
    }
}
