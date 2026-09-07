package com.monkey.ktplus.effects.list.echobat.animation.util;

import com.monkey.ktplus.util.compat.EntityCompat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class EchoBatDisplays {
    private static final int INTERPOLATION_TICKS = 1;

    private EchoBatDisplays() {}

    public static @Nullable Bat spawnBat(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        if (at.getY() - at.getBlockY() < 0.4) {
            at.add(0.0, 0.6, 0.0);
        }
        try {
            Bat bat = (Bat) location.getWorld().spawnEntity(at, EntityType.BAT);
            EntityCompat.trySetSilent(bat, true);
            EntityCompat.trySetInvulnerable(bat, true);
            EntityCompat.trySetPersistent(bat, false);
            EntityCompat.trySetGravity(bat, false);
            EntityCompat.trySetAi(bat, false);
            bat.setCollidable(false);
            bat.setRemoveWhenFarAway(true);
            bat.setAwake(true);
            return bat;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static @Nullable ItemDisplay spawnShard(Location location, float scale) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float safe = Math.max(0.1f, scale);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.ECHO_SHARD));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.CENTER);
            display.setBrightness(new Display.Brightness(14, 14));
            display.setViewRange(64.0f);
            display.setShadowRadius(0.1f);
            display.setShadowStrength(0.3f);
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

    public static void placeBat(@Nullable Bat bat, Location at, float yawDegrees) {
        if (bat == null || !bat.isValid() || bat.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(yawDegrees);
        target.setPitch(-10.0f);
        bat.teleport(target);
    }

    public static void placeShard(
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
        float safe = Math.max(0.1f, scale);
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

    public static void removeBat(@Nullable Bat bat) {
        if (bat != null && bat.isValid() && !bat.isDead()) {
            bat.remove();
        }
    }

    public static void removeShard(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }
}
