package com.monkey.ktplus.effects.list.grave.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.list.grave.corpse.GraveCorpseSpawner;
import com.monkey.ktplus.effects.list.grave.animation.util.GraveOrientation;
import com.monkey.ktplus.effects.list.grave.animation.util.GraveShape;
import com.monkey.ktplus.effects.list.grave.animation.util.GraveStructurePlacer;
import com.monkey.ktplus.effects.list.grave.animation.util.structure.placer.GravePlacer;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.world.SensitiveBlocks;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.MaterialResolver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class GraveLauncher {
    private static final long RESTORE_TICKS = 100L;

    private GraveLauncher() {}

    public static void launch(
            EffectSession session, VisualEffectService visuals, EffectContext context, Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Player killer = context.killer();
        Entity victim = context.victim();
        int groundY = world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ());
        Location anchor = new Location(world, center.getBlockX(), groundY, center.getBlockZ());
        Location surface = anchor.clone();
        BlockFace viewFace = GraveOrientation.viewFace(killer.getLocation());
        BlockFace towardKiller = GraveOrientation.towardKiller(viewFace);

        GraveStructurePlacer.placeAll(session, killer, anchor, viewFace);
        placeWallSignWithName(session, killer, anchor, viewFace, towardKiller, victimDisplayName(victim));
        placeHeadOnCross(session, killer, anchor, viewFace, towardKiller, victim);
        GraveCorpseSpawner.spawn(session, victim, anchor, viewFace, towardKiller, RESTORE_TICKS);
        GravePlacer.randomlyPlaceCoarseDirt(
                session, killer, surface, 12, context.config().effectStructure("grave", true));
        visuals.sound("BLOCK_DEEPSLATE_BRICKS_PLACE", surface, 1.0f, 0.9f);
        visuals.sound("BLOCK_NOTE_BLOCK_BASS", surface, 0.7f, 0.8f);
    }

    private static void placeWallSignWithName(
            EffectSession session,
            Player actor,
            Location anchor,
            BlockFace viewFace,
            BlockFace towardKiller,
            String displayName) {
        Location attach = GraveStructurePlacer.rotatedBlockLocation(
                anchor, viewFace, GraveShape.SIGN_ATTACH_DX, GraveShape.SIGN_ATTACH_DY, GraveShape.SIGN_ATTACH_DZ);
        Location signLoc = GraveOrientation.offsetFace(attach, towardKiller);
        Block signBlock = signLoc.getBlock();
        Material wallSign = MaterialResolver.resolve("OAK_WALL_SIGN", "WALL_SIGN");
        if (SensitiveBlocks.isSensitive(signBlock)) {
            return;
        }
        session.temporaryBlockForStructure(actor, signBlock, wallSign, RESTORE_TICKS, false);
        GraveBlockHelper.configureWallSign(signBlock, towardKiller, displayName);
    }

    private static void placeHeadOnCross(
            EffectSession session,
            Player actor,
            Location anchor,
            BlockFace viewFace,
            BlockFace towardKiller,
            Entity victim) {
        Location attach = GraveStructurePlacer.rotatedBlockLocation(
                anchor, viewFace, GraveShape.HEAD_ATTACH_DX, GraveShape.HEAD_ATTACH_DY, GraveShape.HEAD_ATTACH_DZ);
        Location headLoc = GraveOrientation.offsetFace(attach, towardKiller);
        Block headBlock = headLoc.getBlock();
        Material wallHead = MaterialResolver.find("WALL_PLAYER_HEAD");
        Material playerHead = wallHead != null
                ? wallHead
                : MaterialResolver.resolve("PLAYER_HEAD", "SKULL");
        if (SensitiveBlocks.isSensitive(headBlock)) {
            return;
        }
        session.temporaryBlockForStructure(actor, headBlock, playerHead, RESTORE_TICKS, false);
        Player owner = victim instanceof Player ? (Player) victim : null;
        GraveBlockHelper.configureWallHead(headBlock, towardKiller, owner);
    }

    private static String victimDisplayName(Entity victim) {
        if (victim instanceof Player) {
            return ((Player) victim).getName();
        }
        return victim.getName() != null ? victim.getName() : "Unknown";
    }
}
