package com.monkey.ktplus.effects.list.glowmissile.animation.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GlowMissileShape {
    public static final int TOP_Y = 8;
    public static final int ENGINE_Y = 4;

    private static final BlockEntry[] BLOCKS = {
        entry(0, 0, -3, "STONE_SLAB"),
        entry(-1, 0, -2, "STONE_SLAB"),
        entry(1, 0, -2, "STONE_SLAB"),
        entry(-2, 0, -1, "STONE_SLAB"),
        entry(-1, 0, -1, "OBSIDIAN"),
        entry(1, 0, -1, "OBSIDIAN"),
        entry(2, 0, -1, "STONE_SLAB"),
        entry(-3, 0, 0, "STONE_SLAB"),
        entry(0, 0, 0, "OBSIDIAN"),
        entry(3, 0, 0, "STONE_SLAB"),
        entry(-2, 0, 1, "STONE_SLAB"),
        entry(-1, 0, 1, "OBSIDIAN"),
        entry(1, 0, 1, "OBSIDIAN"),
        entry(2, 0, 1, "STONE_SLAB"),
        entry(-1, 0, 2, "STONE_SLAB"),
        entry(1, 0, 2, "STONE_SLAB"),
        entry(0, 0, 3, "STONE_SLAB"),
        entry(0, 1, -3, "DARK_OAK_FENCE"),
        entry(-1, 1, -2, "DARK_OAK_FENCE"),
        entry(0, 1, -2, "GLOWSTONE"),
        entry(1, 1, -2, "DARK_OAK_FENCE"),
        entry(-2, 1, -1, "DARK_OAK_FENCE"),
        entry(-1, 1, -1, "IRON_BLOCK"),
        entry(0, 1, -1, "IRON_BLOCK"),
        entry(1, 1, -1, "IRON_BLOCK"),
        entry(2, 1, -1, "DARK_OAK_FENCE"),
        entry(-3, 1, 0, "DARK_OAK_FENCE"),
        entry(-2, 1, 0, "GLOWSTONE"),
        entry(-1, 1, 0, "IRON_BLOCK"),
        entry(0, 1, 0, "IRON_BLOCK"),
        entry(1, 1, 0, "IRON_BLOCK"),
        entry(2, 1, 0, "GLOWSTONE"),
        entry(3, 1, 0, "DARK_OAK_FENCE"),
        entry(-2, 1, 1, "DARK_OAK_FENCE"),
        entry(-1, 1, 1, "IRON_BLOCK"),
        entry(0, 1, 1, "IRON_BLOCK"),
        entry(1, 1, 1, "IRON_BLOCK"),
        entry(2, 1, 1, "DARK_OAK_FENCE"),
        entry(-1, 1, 2, "DARK_OAK_FENCE"),
        entry(0, 1, 2, "GLOWSTONE"),
        entry(1, 1, 2, "DARK_OAK_FENCE"),
        entry(0, 1, 3, "DARK_OAK_FENCE"),
        entry(0, 2, -3, "DARK_OAK_FENCE"),
        entry(-1, 2, -2, "DARK_OAK_FENCE"),
        entry(0, 2, -2, "IRON_BLOCK"),
        entry(1, 2, -2, "DARK_OAK_FENCE"),
        entry(-2, 2, -1, "DARK_OAK_FENCE"),
        entry(-1, 2, -1, "IRON_BLOCK"),
        entry(0, 2, -1, "IRON_BLOCK"),
        entry(1, 2, -1, "IRON_BLOCK"),
        entry(2, 2, -1, "DARK_OAK_FENCE"),
        entry(-3, 2, 0, "DARK_OAK_FENCE"),
        entry(-2, 2, 0, "IRON_BLOCK"),
        entry(-1, 2, 0, "IRON_BLOCK"),
        entry(0, 2, 0, "IRON_BLOCK"),
        entry(1, 2, 0, "IRON_BLOCK"),
        entry(2, 2, 0, "IRON_BLOCK"),
        entry(3, 2, 0, "DARK_OAK_FENCE"),
        entry(-2, 2, 1, "DARK_OAK_FENCE"),
        entry(-1, 2, 1, "IRON_BLOCK"),
        entry(0, 2, 1, "IRON_BLOCK"),
        entry(1, 2, 1, "IRON_BLOCK"),
        entry(2, 2, 1, "DARK_OAK_FENCE"),
        entry(-1, 2, 2, "DARK_OAK_FENCE"),
        entry(0, 2, 2, "IRON_BLOCK"),
        entry(1, 2, 2, "DARK_OAK_FENCE"),
        entry(0, 2, 3, "DARK_OAK_FENCE"),
        entry(0, 3, -2, "IRON_BLOCK"),
        entry(-1, 3, -1, "IRON_BLOCK"),
        entry(0, 3, -1, "IRON_BLOCK"),
        entry(1, 3, -1, "IRON_BLOCK"),
        entry(-2, 3, 0, "IRON_BLOCK"),
        entry(-1, 3, 0, "IRON_BLOCK"),
        entry(0, 3, 0, "IRON_BLOCK"),
        entry(1, 3, 0, "IRON_BLOCK"),
        entry(2, 3, 0, "IRON_BLOCK"),
        entry(-1, 3, 1, "IRON_BLOCK"),
        entry(0, 3, 1, "IRON_BLOCK"),
        entry(1, 3, 1, "IRON_BLOCK"),
        entry(0, 3, 2, "IRON_BLOCK"),
        entry(0, 4, -2, "GLOWSTONE"),
        entry(-1, 4, -1, "GLOWSTONE"),
        entry(0, 4, -1, "IRON_BLOCK"),
        entry(1, 4, -1, "GLOWSTONE"),
        entry(-2, 4, 0, "GLOWSTONE"),
        entry(-1, 4, 0, "IRON_BLOCK"),
        entry(0, 4, 0, "IRON_BLOCK"),
        entry(1, 4, 0, "IRON_BLOCK"),
        entry(2, 4, 0, "GLOWSTONE"),
        entry(-1, 4, 1, "GLOWSTONE"),
        entry(0, 4, 1, "IRON_BLOCK"),
        entry(1, 4, 1, "GLOWSTONE"),
        entry(0, 4, 2, "GLOWSTONE"),
        entry(0, 5, -2, "IRON_BLOCK"),
        entry(-1, 5, -1, "IRON_BLOCK"),
        entry(0, 5, -1, "IRON_BLOCK"),
        entry(1, 5, -1, "IRON_BLOCK"),
        entry(-2, 5, 0, "IRON_BLOCK"),
        entry(-1, 5, 0, "IRON_BLOCK"),
        entry(0, 5, 0, "IRON_BLOCK"),
        entry(1, 5, 0, "IRON_BLOCK"),
        entry(2, 5, 0, "IRON_BLOCK"),
        entry(-1, 5, 1, "IRON_BLOCK"),
        entry(0, 5, 1, "IRON_BLOCK"),
        entry(1, 5, 1, "IRON_BLOCK"),
        entry(0, 5, 2, "IRON_BLOCK"),
        entry(0, 6, -2, "IRON_BLOCK"),
        entry(-1, 6, -1, "END_ROD"),
        entry(0, 6, -1, "IRON_BLOCK"),
        entry(1, 6, -1, "END_ROD"),
        entry(-2, 6, 0, "IRON_BLOCK"),
        entry(-1, 6, 0, "IRON_BLOCK"),
        entry(0, 6, 0, "IRON_BLOCK"),
        entry(1, 6, 0, "IRON_BLOCK"),
        entry(2, 6, 0, "IRON_BLOCK"),
        entry(-1, 6, 1, "END_ROD"),
        entry(0, 6, 1, "IRON_BLOCK"),
        entry(1, 6, 1, "END_ROD"),
        entry(0, 6, 2, "IRON_BLOCK"),
        entry(0, 7, -2, "DARK_OAK_FENCE"),
        entry(0, 7, -1, "IRON_BLOCK"),
        entry(-2, 7, 0, "DARK_OAK_FENCE"),
        entry(-1, 7, 0, "IRON_BLOCK"),
        entry(0, 7, 0, "IRON_BLOCK"),
        entry(1, 7, 0, "IRON_BLOCK"),
        entry(2, 7, 0, "DARK_OAK_FENCE"),
        entry(0, 7, 1, "IRON_BLOCK"),
        entry(0, 7, 2, "DARK_OAK_FENCE"),
        entry(0, 8, -1, "DARK_OAK_FENCE"),
        entry(-1, 8, 0, "DARK_OAK_FENCE"),
        entry(0, 8, 0, "BEACON"),
        entry(1, 8, 0, "DARK_OAK_FENCE"),
        entry(0, 8, 1, "DARK_OAK_FENCE"),
    };

    private static final List<BlockEntry> OBSIDIAN_THRUSTERS = buildObsidianThrusters();

    private GlowMissileShape() {}

    public static List<BlockEntry> blocks() {
        return Collections.unmodifiableList(Arrays.asList(BLOCKS));
    }

    public static List<BlockEntry> obsidianThrusters() {
        return OBSIDIAN_THRUSTERS;
    }

    private static List<BlockEntry> buildObsidianThrusters() {
        List<BlockEntry> thrusters = new ArrayList<>();
        for (BlockEntry block : BLOCKS) {
            if ("OBSIDIAN".equals(block.material())) {
                thrusters.add(block);
            }
        }
        return Collections.unmodifiableList(thrusters);
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

        public int dx() { return dx; }
        public int dy() { return dy; }
        public int dz() { return dz; }
        public String material() { return material; }
    }

    private static BlockEntry entry(int dx, int dy, int dz, String material) {
        return new BlockEntry(dx, dy, dz, material);
    }
}
