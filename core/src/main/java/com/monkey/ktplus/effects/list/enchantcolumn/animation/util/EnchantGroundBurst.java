package com.monkey.ktplus.effects.list.enchantcolumn.animation.util;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class EnchantGroundBurst {
    public static final String TAG = "ktplus_enchant_debris";
    private static final int SAMPLE_RADIUS = 3;
    private static final int MAX_BLOCKS = 28;
    private static final long DESPAWN_TICKS = 45L;

    private EnchantGroundBurst() {}

    public static void burst(EffectSession session, Player killer, Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Random random = new Random();
        List<Block> samples = sampleGroundBlocks(world, center, killer, session);
        int limit = Math.min(samples.size(), Math.max(8, ParticleScale.scale(MAX_BLOCKS)));
        for (int i = 0; i < limit; i++) {
            Block block = samples.get(i);
            if (!session.allowsWorldMutation(killer, block.getLocation())) {
                continue;
            }
            BlockData data = block.getBlockData().clone();
            Location spawnAt = block.getLocation().add(0.5, 0.85, 0.5);
            FallingBlock falling = world.spawn(spawnAt, FallingBlock.class, entity -> {
                entity.setBlockData(data);
                entity.setDropItem(false);
                entity.setHurtEntities(false);
                entity.setGravity(true);
                entity.setPersistent(false);
                entity.addScoreboardTag(TAG);
            });
            falling.setVelocity(tntLikeVelocity(center, spawnAt, random));
            session.trackEntity(falling);
            session.runLater(DESPAWN_TICKS, () -> {
                if (falling.isValid() && !falling.isDead()) {
                    falling.remove();
                }
            });
        }
    }

    private static List<Block> sampleGroundBlocks(
            World world, Location center, Player killer, EffectSession session) {
        List<Block> found = new ArrayList<>();
        int baseX = center.getBlockX();
        int baseY = center.getBlockY();
        int baseZ = center.getBlockZ();
        for (int dx = -SAMPLE_RADIUS; dx <= SAMPLE_RADIUS; dx++) {
            for (int dz = -SAMPLE_RADIUS; dz <= SAMPLE_RADIUS; dz++) {
                if (dx * dx + dz * dz > SAMPLE_RADIUS * SAMPLE_RADIUS + 1) {
                    continue;
                }
                Block surface = findSurfaceBlock(world, baseX + dx, baseY, baseZ + dz);
                if (surface == null || !isBurstMaterial(surface.getType())) {
                    continue;
                }
                if (!session.allowsWorldMutation(killer, surface.getLocation())) {
                    continue;
                }
                found.add(surface);
            }
        }
        java.util.Collections.shuffle(found);
        return found;
    }

    private static Block findSurfaceBlock(World world, int x, int aroundY, int z) {
        int minY = Math.max(world.getMinHeight(), aroundY - 4);
        int maxY = Math.min(world.getMaxHeight() - 1, aroundY + 2);
        for (int y = maxY; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (type.isAir() || !type.isSolid()) {
                continue;
            }
            Block above = world.getBlockAt(x, y + 1, z);
            if (above.getType().isAir() || !above.getType().isSolid()) {
                return block;
            }
        }
        return null;
    }

    private static boolean isBurstMaterial(Material type) {
        if (type.isAir() || !type.isSolid()) {
            return false;
        }
        switch (type) {
            case BEDROCK:
            case BARRIER:
            case COMMAND_BLOCK:
            case CHAIN_COMMAND_BLOCK:
            case REPEATING_COMMAND_BLOCK:
            case STRUCTURE_BLOCK:
            case JIGSAW:
            case LIGHT:
            case END_PORTAL:
            case END_PORTAL_FRAME:
            case NETHER_PORTAL:
            case OBSIDIAN:
            case CRYING_OBSIDIAN:
            case REINFORCED_DEEPSLATE:
            case SPAWNER:
            case TRIAL_SPAWNER:
            case VAULT:
                return false;
            default:
                return true;
        }
    }

    private static Vector tntLikeVelocity(Location center, Location spawnAt, Random random) {
        Vector away = spawnAt.toVector().subtract(center.toVector());
        if (away.lengthSquared() < 0.01) {
            away = new Vector(random.nextGaussian(), 0.0, random.nextGaussian());
        }
        away.setY(0.0);
        if (away.lengthSquared() < 0.01) {
            away = new Vector(1.0, 0.0, 0.0);
        }
        away.normalize();
        double horizontal = 0.35 + random.nextDouble() * 0.75;
        double vertical = 0.55 + random.nextDouble() * 0.85;
        return away.multiply(horizontal).setY(vertical);
    }
}
