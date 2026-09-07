package com.monkey.ktplus.effects.support.world;

import org.bukkit.Material;
import org.bukkit.block.Block;

public final class SensitiveBlocks {
    private SensitiveBlocks() {}

    public static boolean isSensitive(Block block) {
        if (block == null) {
            return true;
        }
        Material type = block.getType();
        if (type == null || type.isAir()) {
            return false;
        }
        String name = type.name();
        return name.contains("CHEST")
                || name.contains("SHULKER")
                || name.contains("BARREL")
                || name.contains("FURNACE")
                || name.contains("SPAWNER")
                || name.contains("BEACON")
                || name.contains("COMMAND")
                || name.contains("BANNER")
                || name.contains("SIGN")
                || name.contains("DOOR")
                || name.contains("TRAPDOOR")
                || name.contains("GATE")
                || name.contains("BED")
                || name.contains("ANVIL")
                || name.contains("ENCHANT")
                || name.contains("BREWING")
                || name.contains("HOPPER")
                || name.contains("DROPPER")
                || name.contains("DISPENSER")
                || name.contains("PISTON");
    }
}
