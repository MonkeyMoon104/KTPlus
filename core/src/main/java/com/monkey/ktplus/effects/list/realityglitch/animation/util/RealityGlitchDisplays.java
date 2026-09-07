package com.monkey.ktplus.effects.list.realityglitch.animation.util;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.world.SensitiveBlocks;
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
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class RealityGlitchDisplays {
    private static final int INTERPOLATION_TICKS = 1;

    private RealityGlitchDisplays() {}

    public static @Nullable BlockDisplay spawn(Location location, BlockData data) {
        if (location.getWorld() == null || data == null) {
            return null;
        }
        Location at = location.clone();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return location.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(data);
            display.setBillboard(Display.Billboard.FIXED);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(120.0f);
            display.setShadowRadius(0.55f);
            display.setShadowStrength(0.7f);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setGlowColorOverride(org.bukkit.Color.fromRGB(180, 40, 255));
            display.setGlowing(true);
            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setTeleportDuration(INTERPOLATION_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new Quaternionf(),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new Quaternionf()));
        });
    }

    public static void place(
            @Nullable BlockDisplay display,
            Location at,
            float scale,
            float yaw,
            float pitch,
            float roll) {
        if (display == null || !display.isValid() || display.isDead() || at.getWorld() == null) {
            return;
        }
        Location target = at.clone();
        target.setYaw(0.0f);
        target.setPitch(0.0f);
        display.setTeleportDuration(INTERPOLATION_TICKS);
        display.teleport(target);
        float safe = Math.max(0.35f, scale);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll),
                new Vector3f(safe, safe, safe),
                new Quaternionf()));
    }

    public static void remove(@Nullable BlockDisplay display) {
        if (display != null && display.isValid() && !display.isDead()) {
            display.remove();
        }
    }

    public static List<Block> sampleSurface(
            World world,
            Location center,
            Player killer,
            EffectSession session,
            double radius,
            int maxBlocks) {
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
                if (surface == null || !isSampleable(surface.getType())) {
                    continue;
                }
                if (SensitiveBlocks.isSensitive(surface)) {
                    continue;
                }
                if (!session.allowsWorldMutation(killer, surface.getLocation())) {
                    continue;
                }
                found.add(surface);
            }
        }
        Collections.shuffle(found);
        if (found.size() > maxBlocks) {
            return new ArrayList<>(found.subList(0, maxBlocks));
        }
        return found;
    }

    private static @Nullable Block findSurface(World world, int x, int aroundY, int z) {
        int minY = Math.max(world.getMinHeight(), aroundY - 4);
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

    private static boolean isSampleable(Material type) {
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
