package com.monkey.ktplus.effects.list.tornado.animation.util;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.world.SensitiveBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class TornadoDisplays {
    private static final int INTERPOLATION_TICKS = 1;
    private static final float BLOCK_SCALE = 0.72f;

    private TornadoDisplays() {}

    public static @Nullable ItemDisplay spawnRippedBlock(Location location, Material material) {
        if (location.getWorld() == null || material == null || material.isAir()) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        ItemStack stack = new ItemStack(material);
        return location.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(stack);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(12, 12));
            display.setViewRange(64.0f);
            display.setShadowRadius(0.25f);
            display.setShadowStrength(0.45f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            float s = BLOCK_SCALE * 0.2f;
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(s, s, s),
                    new Quaternionf()));
        });
    }

    public static void place(
            @Nullable ItemDisplay display,
            Location at,
            float yawRadians,
            float pitchRadians,
            float rollRadians,
            float scale) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.2f, scale) * BLOCK_SCALE;
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

    public static List<Block> sampleRipBlocks(World world, Location center, double radius, int max) {
        return sampleRipBlocks(world, center, null, null, radius, max);
    }

    public static List<Block> sampleRipBlocks(
            World world,
            Location center,
            @Nullable Player killer,
            @Nullable EffectSession session,
            double radius,
            int max) {
        List<Block> found = new ArrayList<>();
        int baseX = center.getBlockX();
        int baseY = center.getBlockY();
        int baseZ = center.getBlockZ();
        int r = Math.max(1, (int) Math.ceil(radius));
        double radiusSq = radius * radius;

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radiusSq + 0.25) {
                    continue;
                }
                Block surface = findSurface(world, baseX + dx, baseY, baseZ + dz);
                if (surface == null || !isRippable(surface.getType())) {
                    continue;
                }
                if (SensitiveBlocks.isSensitive(surface)) {
                    continue;
                }
                if (session != null
                        && killer != null
                        && !session.allowsWorldMutation(killer, surface.getLocation())) {
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
            if (block.getType().isAir() || !block.getType().isSolid()) {
                continue;
            }
            Block above = world.getBlockAt(x, y + 1, z);
            if (above.getType().isAir() || !above.getType().isSolid()) {
                return block;
            }
        }
        return null;
    }

    private static boolean isRippable(Material type) {
        if (type.isAir() || !type.isSolid() || !type.isItem()) {
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
