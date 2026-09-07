package com.monkey.ktplus.effects.list.dragonnest.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class DragonNestDisplays {
    private static final int INTERPOLATION_TICKS = 2;

    private DragonNestDisplays() {}

    public static @Nullable ItemDisplay spawnEgg(Location location) {
        return spawn(location, Material.DRAGON_EGG, 0.55f);
    }

    public static @Nullable ItemDisplay spawnBreathOrb(Location location) {
        return spawn(location, Material.DRAGON_BREATH, 0.35f);
    }

    public static void place(
            @Nullable ItemDisplay display,
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
        Quaternionf rotation = new Quaternionf().rotateY(yawRadians).rotateX(pitchRadians);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(0.0f, safe * 0.35f, 0.0f),
                rotation,
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void remove(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    private static @Nullable ItemDisplay spawn(Location location, Material material, float startScale) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float scale = Math.max(0.08f, startScale);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(96.0f);
            display.setShadowRadius(0.4f);
            display.setShadowStrength(0.65f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(0.0f, scale * 0.35f, 0.0f),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
        });
    }
}
