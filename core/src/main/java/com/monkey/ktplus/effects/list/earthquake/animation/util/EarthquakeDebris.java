package com.monkey.ktplus.effects.list.earthquake.animation.util;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class EarthquakeDebris {
    public static final String TAG = "ktplus_earthquake_debris";
    private static final long DESPAWN_TICKS = 40L;

    private EarthquakeDebris() {}

    public static void burst(
            EffectSession session, Player killer, Location center, List<BlockData> samples) {
        World world = center.getWorld();
        if (world == null || samples.isEmpty()) {
            return;
        }
        Random random = new Random();
        int limit = Math.min(samples.size(), Math.max(6, ParticleScale.scale(18)));
        for (int i = 0; i < limit; i++) {
            BlockData data = samples.get(i % samples.size()).clone();
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 0.4 + random.nextDouble() * 1.6;
            Location spawnAt = center.clone().add(Math.cos(angle) * dist, 0.9, Math.sin(angle) * dist);
            if (!session.allowsWorldMutation(killer, spawnAt)) {
                continue;
            }
            FallingBlock falling = world.spawn(spawnAt, FallingBlock.class, entity -> {
                entity.setBlockData(data);
                entity.setDropItem(false);
                entity.setHurtEntities(false);
                entity.setGravity(true);
                entity.setPersistent(false);
                entity.addScoreboardTag(TAG);
            });
            falling.setVelocity(burstVelocity(center, spawnAt, random));
            session.trackEntity(falling);
            session.runLater(DESPAWN_TICKS, () -> {
                if (falling.isValid() && !falling.isDead()) {
                    falling.remove();
                }
            });
        }
    }

    public static void burstFromBlocks(
            EffectSession session, Player killer, Location center, List<Block> blocks) {
        List<BlockData> data = blocks.stream().map(b -> b.getBlockData().clone()).toList();
        burst(session, killer, center, data);
    }

    private static Vector burstVelocity(Location center, Location spawnAt, Random random) {
        Vector away = spawnAt.toVector().subtract(center.toVector());
        if (away.lengthSquared() < 0.01) {
            away = new Vector(random.nextGaussian(), 0.0, random.nextGaussian());
        }
        away.setY(0.0);
        if (away.lengthSquared() < 0.01) {
            away = new Vector(1.0, 0.0, 0.0);
        }
        away.normalize();
        double horizontal = 0.25 + random.nextDouble() * 0.55;
        double vertical = 0.4 + random.nextDouble() * 0.55;
        return away.multiply(horizontal).setY(vertical);
    }
}
