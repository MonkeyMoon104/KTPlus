package com.monkey.ktplus.effects.list.glowmissile.animation.util;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.world.SensitiveBlocks;
import com.monkey.ktplus.util.compat.MaterialCompat;
import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class GlowMissileBlocks {
    private GlowMissileBlocks() {}

    public static Location resolveAnchor(Location deathLocation) {
        Location blockLoc = deathLocation.getBlock().getLocation();
        World world = blockLoc.getWorld();
        if (world == null) {
            return deathLocation.clone();
        }
        Location cursor = blockLoc.clone();
        while (!MaterialCompat.isAir(cursor.getBlock().getType()) && cursor.getY() < world.getMaxHeight()) {
            cursor.add(0, 1, 0);
        }
        cursor.add(0, 3, 0);
        return cursor.add(0.5, 0, 0.5);
    }

    public static void clearBlocks(EffectSession session, Player actor, Map<Location, Material> originals) {
        for (Map.Entry<Location, Material> entry : originals.entrySet()) {
            Block block = entry.getKey().getBlock();
            if (session.allowsWorldMutation(actor, block.getLocation())) {
                block.setType(entry.getValue());
            }
        }
        originals.clear();
    }

    public static Location engineLocation(Location anchor) {
        return anchor.clone().add(0, GlowMissileShape.ENGINE_Y + 0.5, 0);
    }

    public static Location tipLocation(Location anchor, int riseOffset) {
        return anchor.clone().add(0, riseOffset + GlowMissileShape.TOP_Y + 0.5, 0);
    }

    public static void placeAnimated(
            EffectSession session,
            Player actor,
            Location anchor,
            int blocksPerTick,
            long tickInterval,
            Map<Location, Material> originals,
            Runnable onComplete) {
        List<GlowMissileShape.BlockEntry> blocks = GlowMissileShape.blocks();
        AtomicInteger cursor = new AtomicInteger();
        session.runTimer(0L, tickInterval, () -> {
            if (cursor.get() >= blocks.size()) {
                onComplete.run();
                return false;
            }
            int placed = 0;
            while (cursor.get() < blocks.size() && placed < blocksPerTick) {
                GlowMissileShape.BlockEntry entry = blocks.get(cursor.get());
                if (tryPlaceEntry(session, actor, anchor, entry, originals, true)) {
                    cursor.incrementAndGet();
                    placed++;
                    continue;
                }
                if (shouldSkipEntry(session, actor, entry, anchor)) {
                    cursor.incrementAndGet();
                    continue;
                }
                break;
            }
            if (cursor.get() >= blocks.size()) {
                onComplete.run();
                return false;
            }
            return true;
        });
    }

    public static Location placeFrame(
            EffectSession session,
            Player actor,
            Location anchor,
            Map<Location, Material> originals) {
        Location engine = null;
        for (GlowMissileShape.BlockEntry entry : GlowMissileShape.blocks()) {
            if (!tryPlaceEntry(session, actor, anchor, entry, originals, false)) {
                continue;
            }
            if (entry.dy() >= GlowMissileShape.ENGINE_Y
                    && (engine == null || entry.dy() > engine.getBlockY() - anchor.getBlockY())) {
                Location target = blockLocation(anchor, entry);
                engine = target.clone().add(0.5, 0.5, 0.5);
            }
        }
        return engine != null ? engine : engineLocation(anchor);
    }

    private static boolean shouldSkipEntry(
            EffectSession session, Player actor, GlowMissileShape.BlockEntry entry, Location anchor) {
        Location target = blockLocation(anchor, entry);
        Block worldBlock = target.getBlock();
        if (!session.allowsWorldMutation(actor, target)) {
            return true;
        }
        if (SensitiveBlocks.isSensitive(worldBlock) && !MaterialCompat.isAir(worldBlock.getType())) {
            return true;
        }
        if (entry.dy() > 0 && !MaterialCompat.isAir(worldBlock.getType())) {
            return true;
        }
        return resolveMaterial(entry.material()) == null;
    }

    private static boolean tryPlaceEntry(
            EffectSession session,
            Player actor,
            Location anchor,
            GlowMissileShape.BlockEntry entry,
            Map<Location, Material> originals,
            boolean playSound) {
        Location target = blockLocation(anchor, entry);
        Block worldBlock = target.getBlock();
        if (!session.allowsWorldMutation(actor, target)) {
            return false;
        }
        if (SensitiveBlocks.isSensitive(worldBlock) && !MaterialCompat.isAir(worldBlock.getType())) {
            return false;
        }
        if (entry.dy() > 0 && !MaterialCompat.isAir(worldBlock.getType())) {
            return false;
        }
        Material material = resolveMaterial(entry.material());
        if (material == null) {
            return false;
        }
        Location key = blockKey(target);
        originals.putIfAbsent(key, worldBlock.getType());
        worldBlock.setType(material);
        if (playSound) {
            playPlaceSound(worldBlock, material);
        }
        return true;
    }

    private static void playPlaceSound(Block block, Material material) {
        World world = block.getWorld();
        if (world == null || material.isAir()) {
            return;
        }
        Location at = block.getLocation().add(0.5, 0.5, 0.5);
        try {
            world.playSound(at, material.createBlockData().getSoundGroup().getPlaceSound(), 1.0f, 0.92f);
        } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
            world.playSound(at, "block.stone.place", 1.0f, 0.92f);
        }
    }

    private static Location blockLocation(Location anchor, GlowMissileShape.BlockEntry entry) {
        return new Location(
                anchor.getWorld(),
                anchor.getBlockX() + entry.dx(),
                anchor.getBlockY() + entry.dy(),
                anchor.getBlockZ() + entry.dz());
    }

    private static Location blockKey(Location location) {
        return new Location(
                location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static Material resolveMaterial(String materialName) {
        switch (materialName) {
            case "STONE_SLAB":
                return MaterialResolver.resolve("SMOOTH_STONE_SLAB", "STEP");
            case "DARK_OAK_FENCE":
                return MaterialResolver.resolve("DARK_OAK_FENCE", "NETHER_FENCE");
            case "END_ROD":
                return MaterialResolver.find("END_ROD") != null
                        ? MaterialResolver.resolve("END_ROD", "END_ROD")
                        : MaterialResolver.resolve("BLAZE_ROD", "BLAZE_ROD");
            default:
                return MaterialResolver.find(materialName);
        }
    }
}
