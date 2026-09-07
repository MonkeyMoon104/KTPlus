package com.monkey.ktplus.effects.list.grave.animation.util;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.world.SensitiveBlocks;
import com.monkey.ktplus.util.compat.MaterialResolver;
import java.lang.reflect.Method;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class GraveStructurePlacer {
    private static final long RESTORE_TICKS = 100L;
    private static final @Nullable Method SET_DATA = findSetData();
    private static final @Nullable Method CREATE_BLOCK_DATA = findCreateBlockData();
    private static final @Nullable Method SET_BLOCK_DATA = findSetBlockData();

    private GraveStructurePlacer() {}

    public static void placeAll(
            EffectSession session, Player actor, Location anchor, BlockFace facing) {
        for (GraveShape.BlockEntry entry : GraveShape.blocks()) {
            placeEntry(session, actor, anchor, facing, entry);
        }
    }

    public static Location rotatedBlockLocation(
            Location anchor, BlockFace facing, int dx, int dy, int dz) {
        int[] rotated = GraveShape.rotateXZ(dx, dz, facing);
        return new Location(
                anchor.getWorld(),
                anchor.getBlockX() + rotated[0],
                anchor.getBlockY() + dy,
                anchor.getBlockZ() + rotated[1]);
    }

    private static void placeEntry(
            EffectSession session,
            Player actor,
            Location anchor,
            BlockFace facing,
            GraveShape.BlockEntry entry) {
        Location target = rotatedBlockLocation(anchor, facing, entry.dx(), entry.dy(), entry.dz());
        Block block = target.getBlock();
        if (SensitiveBlocks.isSensitive(block)) {
            return;
        }
        Material material = resolveMaterial(entry.material());
        if (material == null) {
            return;
        }
        session.temporaryBlockForStructure(actor, block, material, RESTORE_TICKS, false);
        if ("DEEPSLATE_BRICK_SLAB".equals(entry.material())) {
            applyBottomSlab(block, material);
        }
    }

    private static void applyBottomSlab(Block block, Material material) {
        String materialKey = material.name().toLowerCase(Locale.ROOT);
        String[] candidates = {
            materialKey + "[type=bottom]",
            "deepslate_brick_slab[type=bottom]",
            "stone_brick_slab[type=bottom]",
            "smooth_stone_slab[type=bottom]",
            "minecraft:deepslate_brick_slab[type=bottom]",
            "minecraft:stone_brick_slab[type=bottom]"
        };
        for (String candidate : candidates) {
            if (tryBlockData(block, candidate)) {
                return;
            }
        }
        if (SET_DATA != null && isLegacyStep(block.getType())) {
            try {
                SET_DATA.invoke(block, (byte) 5);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static boolean isLegacyStep(Material material) {
        return "STEP".equals(material.name()) || "DOUBLE_STEP".equals(material.name());
    }

    private static boolean tryBlockData(Block block, String data) {
        if (CREATE_BLOCK_DATA == null || SET_BLOCK_DATA == null) {
            return false;
        }
        try {
            Object blockData = CREATE_BLOCK_DATA.invoke(null, data);
            SET_BLOCK_DATA.invoke(block, blockData);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static Material resolveMaterial(String materialName) {
        if ("DEEPSLATE_BRICKS".equals(materialName)) {
            return MaterialResolver.resolve("DEEPSLATE_BRICKS", "STONE_BRICKS");
        }
        if ("DEEPSLATE_BRICK_SLAB".equals(materialName)) {
            return MaterialResolver.resolve("DEEPSLATE_BRICK_SLAB", "STEP");
        }
        if ("TORCH".equals(materialName)) {
            return MaterialResolver.resolve("TORCH", "TORCH");
        }
        return MaterialResolver.find(materialName);
    }

    private static @Nullable Method findSetData() {
        try {
            return Block.class.getMethod("setData", byte.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static @Nullable Method findCreateBlockData() {
        try {
            return Class.forName("org.bukkit.Bukkit").getMethod("createBlockData", String.class);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable Method findSetBlockData() {
        try {
            Class<?> blockData = Class.forName("org.bukkit.block.data.BlockData");
            return Block.class.getMethod("setBlockData", blockData);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
