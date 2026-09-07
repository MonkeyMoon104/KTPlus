package com.monkey.ktplus.effects.list.geyser.animation.util;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.world.SensitiveBlocks;
import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class GeyserDisplays {
    private static final int INTERPOLATION_TICKS = 1;

    private GeyserDisplays() {}

    public static Material columnMaterial(int index) {
        return switch (index % 4) {
            case 0 -> MaterialResolver.resolve("PRISMARINE", "CYAN_STAINED_GLASS");
            case 1 -> MaterialResolver.resolve("PRISMARINE_BRICKS", "LIGHT_BLUE_STAINED_GLASS");
            case 2 -> MaterialResolver.resolve("SEA_LANTERN", "GLOWSTONE");
            default -> MaterialResolver.resolve("BLUE_STAINED_GLASS", "ICE");
        };
    }

    public static @Nullable ItemDisplay spawn(Location location, Material material, float scale) {
        if (location.getWorld() == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float safe = Math.max(0.08f, scale);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(72.0f);
            display.setShadowRadius(0.08f);
            display.setShadowStrength(0.25f);
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

    public static @Nullable BlockDisplay spawnDebris(Location location, BlockData data) {
        if (location.getWorld() == null || data == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        float scale = 0.55f + (float) (Math.random() * 0.35);
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(data);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(14, 14));
            display.setViewRange(64.0f);
            display.setShadowRadius(0.2f);
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

    public static void place(
            @Nullable ItemDisplay display, Location at, float yaw, float pitch, float roll, float scale) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.08f, scale);
        Quaternionf rotation = new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                rotation,
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void placeDebris(
            @Nullable BlockDisplay display, Location at, float yaw, float pitch, float roll, float scale) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.2f, scale);
        Quaternionf rotation = new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                rotation,
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void remove(@Nullable ItemDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    public static void removeDebris(@Nullable BlockDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    public static void removeAll(Iterable<ItemDisplay> displays) {
        for (ItemDisplay display : displays) {
            remove(display);
        }
    }

    public static void removeAllDebris(Iterable<BlockDisplay> displays) {
        for (BlockDisplay display : displays) {
            removeDebris(display);
        }
    }

    public static List<Block> sampleSurface(
            World world, Location center, @Nullable Player killer, EffectSession session, double radius, int max) {
        List<Block> found = new ArrayList<>();
        int baseX = center.getBlockX();
        int baseY = center.getBlockY();
        int baseZ = center.getBlockZ();
        int r = Math.max(1, (int) Math.ceil(radius));
        double radiusSq = radius * radius;

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radiusSq + 0.5) {
                    continue;
                }
                Block surface = findSurface(world, baseX + dx, baseY, baseZ + dz);
                if (surface == null || !isRipMaterial(surface.getType())) {
                    continue;
                }
                if (SensitiveBlocks.isSensitive(surface)) {
                    continue;
                }
                if (killer != null && !session.allowsWorldMutation(killer, surface.getLocation())) {
                    continue;
                }
                found.add(surface);
            }
        }
        Collections.shuffle(found);
        if (found.size() > max) {
            return new ArrayList<>(found.subList(0, max));
        }
        return found;
    }

    private static @Nullable Block findSurface(World world, int x, int aroundY, int z) {
        int minY = Math.max(world.getMinHeight(), aroundY - 3);
        int maxY = Math.min(world.getMaxHeight() - 1, aroundY + 2);
        for (int y = maxY; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (type.isAir() || !type.isSolid()) {
                continue;
            }
            Block above = world.getBlockAt(x, y + 1, z);
            if (above.getType().isAir() || !above.getType().isSolid()) {
                return block;
            }
        }
        return null;
    }

    private static boolean isRipMaterial(Material type) {
        if (type.isAir() || !type.isSolid()) {
            return false;
        }
        return switch (type) {
            case BEDROCK,
                    BARRIER,
                    COMMAND_BLOCK,
                    CHAIN_COMMAND_BLOCK,
                    REPEATING_COMMAND_BLOCK,
                    STRUCTURE_BLOCK,
                    JIGSAW,
                    LIGHT,
                    END_PORTAL,
                    END_PORTAL_FRAME,
                    NETHER_PORTAL,
                    OBSIDIAN,
                    CRYING_OBSIDIAN,
                    REINFORCED_DEEPSLATE,
                    SPAWNER,
                    TRIAL_SPAWNER,
                    VAULT -> false;
            default -> true;
        };
    }
}
