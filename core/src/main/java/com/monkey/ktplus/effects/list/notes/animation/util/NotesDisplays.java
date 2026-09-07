package com.monkey.ktplus.effects.list.notes.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class NotesDisplays {
    private static final int INTERPOLATION_TICKS = 2;
    public static final float BLOCK_SCALE = 1.05f;
    public static final float DISC_SCALE = 0.55f;

    private NotesDisplays() {}

    public static @Nullable ItemDisplay spawnNoteBlock(Location location) {
        return spawn(location, Material.NOTE_BLOCK, BLOCK_SCALE * 0.12f, 0.5f);
    }

    public static @Nullable ItemDisplay spawnJukebox(Location location) {
        return spawn(location, Material.JUKEBOX, BLOCK_SCALE * 0.12f, 0.5f);
    }

    public static @Nullable ItemDisplay spawnDisc(Location location, Material disc) {
        return spawn(location, disc, DISC_SCALE * 0.2f, 0.0f);
    }

    public static void setItem(@Nullable ItemDisplay display, Material material) {
        if (display == null || !display.isValid() || display.isDead()) {
            return;
        }
        display.setItemStack(new ItemStack(material));
    }

    public static void placeBlock(@Nullable ItemDisplay display, Location at, float scale, float yawRadians) {
        place(display, at, scale * BLOCK_SCALE, yawRadians, 0.0f, 0.5f * scale);
    }

    public static void placeDisc(
            @Nullable ItemDisplay display, Location at, float yawRadians, float pitchRadians, float rollRadians) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        Quaternionf rotation = new Quaternionf()
                .rotateY(yawRadians)
                .rotateX(pitchRadians)
                .rotateZ(rollRadians);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                rotation,
                new Vector3f(DISC_SCALE, DISC_SCALE, DISC_SCALE),
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
            Location location, Material material, float startScale, float yOffsetFactor) {
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
            display.setViewRange(64.0f);
            display.setShadowRadius(material == Material.NOTE_BLOCK || material == Material.JUKEBOX ? 0.35f : 0.1f);
            display.setShadowStrength(0.55f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(0.0f, yOffsetFactor * scale, 0.0f),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
        });
    }

    private static void place(
            @Nullable ItemDisplay display,
            Location at,
            float scale,
            float yawRadians,
            float pitchRadians,
            float yLift) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safeScale = Math.max(0.05f, scale);
        Quaternionf rotation = new Quaternionf().rotateY(yawRadians).rotateX(pitchRadians);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(0.0f, yLift * safeScale, 0.0f),
                rotation,
                new Vector3f(safeScale, safeScale, safeScale),
                new Quaternionf()));
    }
}
