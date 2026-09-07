package com.monkey.ktplus.effects.list.grave.animation.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GraveShape {
    
    public static final int SIGN_ATTACH_DX = 0;
    public static final int SIGN_ATTACH_DY = 6;
    public static final int SIGN_ATTACH_DZ = -2;
    
    public static final int HEAD_ATTACH_DX = 0;
    public static final int HEAD_ATTACH_DY = 7;
    public static final int HEAD_ATTACH_DZ = -2;

    private static final BlockEntry[] BLOCKS = {
        entry(-1, 0, -2, "DEEPSLATE_BRICKS"),
        entry(0, 0, -2, "DEEPSLATE_BRICKS"),
        entry(1, 0, -2, "DEEPSLATE_BRICKS"),
        entry(-1, 1, -3, "DEEPSLATE_BRICK_SLAB"),
        entry(-2, 1, -2, "DEEPSLATE_BRICK_SLAB"),
        entry(-1, 1, -2, "DEEPSLATE_BRICKS"),
        entry(0, 1, -2, "DEEPSLATE_BRICKS"),
        entry(1, 1, -2, "DEEPSLATE_BRICKS"),
        entry(2, 1, -2, "DEEPSLATE_BRICK_SLAB"),
        entry(-2, 1, -1, "TORCH"),
        entry(-1, 1, -1, "DEEPSLATE_BRICK_SLAB"),
        entry(0, 1, -1, "DEEPSLATE_BRICK_SLAB"),
        entry(1, 1, -1, "DEEPSLATE_BRICK_SLAB"),
        entry(2, 1, -1, "TORCH"),
        entry(-1, 1, 0, "DEEPSLATE_BRICK_SLAB"),
        entry(0, 1, 0, "DEEPSLATE_BRICK_SLAB"),
        entry(1, 1, 0, "DEEPSLATE_BRICK_SLAB"),
        entry(-1, 1, 1, "DEEPSLATE_BRICK_SLAB"),
        entry(0, 1, 1, "DEEPSLATE_BRICK_SLAB"),
        entry(1, 1, 1, "DEEPSLATE_BRICK_SLAB"),
        entry(-1, 1, 2, "DEEPSLATE_BRICK_SLAB"),
        entry(0, 1, 2, "DEEPSLATE_BRICK_SLAB"),
        entry(1, 1, 2, "DEEPSLATE_BRICK_SLAB"),
        entry(0, 1, 3, "DEEPSLATE_BRICK_SLAB"),
        entry(0, 2, -2, "DEEPSLATE_BRICKS"),
        entry(0, 3, -2, "DEEPSLATE_BRICKS"),
        entry(0, 4, -2, "DEEPSLATE_BRICKS"),
        entry(0, 5, -2, "DEEPSLATE_BRICKS"),
        entry(-1, 6, -2, "DEEPSLATE_BRICKS"),
        entry(0, 6, -2, "DEEPSLATE_BRICKS"),
        entry(1, 6, -2, "DEEPSLATE_BRICKS"),
        entry(0, 7, -2, "DEEPSLATE_BRICKS"),
    };

    private GraveShape() {}

    public static List<BlockEntry> blocks() {
        return Collections.unmodifiableList(Arrays.asList(BLOCKS));
    }

    public static int[] rotateXZ(int dx, int dz, org.bukkit.block.BlockFace facing) {
        switch (facing) {
            case NORTH:
                return new int[] {dx, dz};
            case EAST:
                return new int[] {-dz, dx};
            case SOUTH:
                return new int[] {-dx, -dz};
            case WEST:
                return new int[] {dz, -dx};
            default:
                return new int[] {dx, dz};
        }
    }

    public static final class BlockEntry {
        private final int dx;
        private final int dy;
        private final int dz;
        private final String material;

        private BlockEntry(int dx, int dy, int dz, String material) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.material = material;
        }

        public int dx() {
            return dx;
        }

        public int dy() {
            return dy;
        }

        public int dz() {
            return dz;
        }

        public String material() {
            return material;
        }
    }

    private static BlockEntry entry(int dx, int dy, int dz, String material) {
        return new BlockEntry(dx, dy, dz, material);
    }
}
