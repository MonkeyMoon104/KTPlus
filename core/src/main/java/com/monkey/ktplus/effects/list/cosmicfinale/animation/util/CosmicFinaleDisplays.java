package com.monkey.ktplus.effects.list.cosmicfinale.animation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class CosmicFinaleDisplays {
    private static final int INTERPOLATION_TICKS = 1;

    private CosmicFinaleDisplays() {}

    public static @Nullable ItemDisplay spawnStar(Location location, float scale) {
        return spawn(location, Material.NETHER_STAR, scale, true);
    }

    public static @Nullable ItemDisplay spawnCore(Location location, float scale) {
        return spawn(location, Material.END_CRYSTAL, scale, true);
    }

    public static void place(@Nullable ItemDisplay display, Location at, float scale, float yaw, float pitch) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.2f, scale);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateY(yaw).rotateX(pitch),
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void remove(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    private static @Nullable ItemDisplay spawn(Location location, Material material, float scale, boolean glow) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float safe = Math.max(0.2f, scale);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.CENTER);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(180.0f);
            display.setShadowRadius(1.0f);
            display.setShadowStrength(0.7f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            if (glow) {
                display.setGlowColorOverride(org.bukkit.Color.fromRGB(220, 240, 255));
                display.setGlowing(true);
            }
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
