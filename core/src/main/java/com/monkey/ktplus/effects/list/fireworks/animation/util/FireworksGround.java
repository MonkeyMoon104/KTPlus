package com.monkey.ktplus.effects.list.fireworks.animation.util;

import com.monkey.ktplus.util.compat.MaterialCompat;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class FireworksGround {
    private FireworksGround() {}

    public static Location resolveGroundCenter(Location deathLocation) {
        Objects.requireNonNull(deathLocation, "deathLocation");
        World world = deathLocation.getWorld();
        if (world == null) {
            return deathLocation.clone();
        }
        int x = deathLocation.getBlockX();
        int z = deathLocation.getBlockZ();
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y + 1.0, z + 0.5);
    }

    public static @Nullable Block solidBlockBelow(Player player) {
        Objects.requireNonNull(player, "player");
        if (player.getVelocity().getY() > 0.15) {
            return null;
        }
        World world = player.getWorld();
        Location loc = player.getLocation();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int y = (int) Math.floor(loc.getY() - 0.01);
        Block feet = world.getBlockAt(x, y, z);
        if (MaterialCompat.isAir(feet.getType())) {
            Block below = feet.getRelative(BlockFace.DOWN);
            return isSolid(below.getType()) ? below : null;
        }
        if (isSolid(feet.getType())) {
            return feet;
        }
        Block below = feet.getRelative(BlockFace.DOWN);
        return isSolid(below.getType()) ? below : null;
    }

    public static double surfaceParticleY(World world, double x, double z, double offset) {
        Objects.requireNonNull(world, "world");
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int surfaceY = world.getHighestBlockYAt(blockX, blockZ);
        return surfaceY + 1.0 + offset;
    }

    public static @Nullable Block surfaceBlock(World world, int blockX, int blockZ) {
        Objects.requireNonNull(world, "world");
        int y = world.getHighestBlockYAt(blockX, blockZ);
        Block block = world.getBlockAt(blockX, y, blockZ);
        if (MaterialCompat.isAir(block.getType())) {
            return null;
        }
        return block;
    }

    public static boolean isSolid(Material material) {
        if (material == null || MaterialCompat.isAir(material)) {
            return false;
        }
        try {
            return (Boolean) Material.class.getMethod("isSolid").invoke(material);
        } catch (ReflectiveOperationException ex) {
            String name = material.name();
            return !name.contains("WATER")
                    && !name.contains("LAVA")
                    && !name.contains("GRASS")
                    && !name.contains("SAPLING")
                    && !name.contains("FLOWER")
                    && !name.contains("TORCH")
                    && !name.contains("RAIL")
                    && !name.contains("CARPET")
                    && !name.contains("PRESSURE")
                    && !name.contains("BUTTON")
                    && !name.contains("SIGN")
                    && !name.contains("BANNER");
        }
    }
}
