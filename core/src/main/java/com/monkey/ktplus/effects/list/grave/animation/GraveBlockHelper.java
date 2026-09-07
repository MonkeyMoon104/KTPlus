package com.monkey.ktplus.effects.list.grave.animation;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.data.type.WallSkull;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

final class GraveBlockHelper {
    private GraveBlockHelper() {}

    static void configureWallSign(Block signBlock, BlockFace facing, String displayName) {
        BlockData data = signBlock.getBlockData();
        if (data instanceof WallSign wallSign) {
            wallSign.setFacing(facing);
            signBlock.setBlockData(wallSign, false);
        }
        BlockState state = signBlock.getState();
        if (state instanceof Sign sign) {
            sign.getSide(Side.FRONT).line(0, Component.text("R.I.P."));
            sign.getSide(Side.FRONT).line(1, Component.text(displayName));
            sign.update(true, false);
        }
    }

    static void configureWallHead(Block headBlock, BlockFace facing, @Nullable Player owner) {
        applyWallHeadBlockData(headBlock, headBlock.getType(), facing);
        BlockState state = headBlock.getState();
        if (state instanceof Skull skull && owner != null) {
            skull.setProfile(ResolvableProfile.resolvableProfile(owner.getPlayerProfile()));
            skull.update(true, false);
            return;
        }
        state.update(true, false);
    }

    static boolean applyWallHeadBlockData(Block block, Material material, BlockFace facing) {
        String facingName = facing.name().toLowerCase(Locale.ROOT);
        String materialKey = material.name().toLowerCase(Locale.ROOT);
        String[] candidates = {
            materialKey + "[facing=" + facingName + "]",
            "wall_player_head[facing=" + facingName + "]",
            "player_wall_head[facing=" + facingName + "]",
            "minecraft:wall_player_head[facing=" + facingName + "]",
            "minecraft:player_head[facing=" + facingName + "]"
        };
        for (String candidate : candidates) {
            if (applyBlockDataString(block, candidate)) {
                return true;
            }
        }
        BlockData data = block.getBlockData();
        if (data instanceof WallSkull wallSkull) {
            wallSkull.setFacing(facing);
            block.setBlockData(wallSkull, false);
            return true;
        }
        return false;
    }

    static boolean applyBottomSlabBlockData(Block block, Material material) {
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
            if (applyBlockDataString(block, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean applyBlockDataString(Block block, String data) {
        try {
            block.setBlockData(Bukkit.createBlockData(data), false);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
