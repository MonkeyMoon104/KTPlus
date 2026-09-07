package com.monkey.ktplus.effects.list.grave.animation.util.structure.placer;

import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.world.SensitiveBlocks;
import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class GravePlacer {
    private GravePlacer() {}

    public static void randomlyPlaceCoarseDirt(
            EffectSession session, Player actor, Location center, int radius, boolean allowStructure) {
        if (!allowStructure) {
            return;
        }
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Random random = new Random(1);
        int radiusSquared = radius * radius;
        List<Location> candidatePositions = new ArrayList<>();
        int maxBlocks = Math.min(45, radiusSquared / 4);
        Set<String> usedPositions = new HashSet<>();
        for (int i = 0; i < maxBlocks * 3 && candidatePositions.size() < maxBlocks; i++) {
            int x = random.nextInt(radius * 2 + 1) - radius;
            int z = random.nextInt(radius * 2 + 1) - radius;
            if (x * x + z * z > radiusSquared) {
                continue;
            }
            if (random.nextDouble() > 0.65) {
                int baseX = center.getBlockX() + x;
                int baseZ = center.getBlockZ() + z;
                int surfaceY = world.getHighestBlockYAt(baseX, baseZ);
                String key = baseX + "," + surfaceY + "," + baseZ;
                if (!usedPositions.contains(key)) {
                    usedPositions.add(key);
                    candidatePositions.add(new Location(world, baseX, surfaceY, baseZ));
                }
            }
        }
        Collections.shuffle(candidatePositions, random);
        if (candidatePositions.isEmpty()) {
            return;
        }
        Material coarseDirt = MaterialResolver.resolve("COARSE_DIRT", "DIRT");
        Material grass = MaterialResolver.resolve("GRASS_BLOCK", "GRASS");
        AtomicInteger index = new AtomicInteger();
        session.runTimer(0L, 2L, () -> {
            int processed = 0;
            while (index.get() < candidatePositions.size() && processed < 3) {
                Location groundLoc = candidatePositions.get(index.getAndIncrement());
                Block targetBlock = groundLoc.getBlock();
                if (!SensitiveBlocks.isSensitive(targetBlock)) {
                    Material originalMat = targetBlock.getType();
                    if (originalMat == Material.DIRT
                            || originalMat == grass
                            || originalMat == Material.PODZOL
                            || originalMat == Material.COARSE_DIRT) {
                        if (originalMat != Material.COARSE_DIRT) {
                            session.temporaryBlockWithPhysics(actor, targetBlock, coarseDirt, 100L);
                        }
                    }
                }
                processed++;
            }
            return index.get() < candidatePositions.size();
        });
    }
}
