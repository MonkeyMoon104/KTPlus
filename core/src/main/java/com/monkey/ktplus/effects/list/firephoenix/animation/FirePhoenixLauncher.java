package com.monkey.ktplus.effects.list.firephoenix.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.list.firephoenix.animation.util.FirePhoenixParticles;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.BukkitParticles;
import com.monkey.ktplus.util.compat.WorldCompat;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;

public final class FirePhoenixLauncher {
    private FirePhoenixLauncher() {}

    public static void launch(EffectSession session, EffectContext context, Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        AtomicInteger ticks = new AtomicInteger();
        Color[] chargeColors = {
            Color.fromRGB(255, 80, 0),
            Color.fromRGB(255, 160, 0),
            Color.fromRGB(255, 230, 100)
        };
        session.runTimer(0L, 2L, () -> {
            int current = ticks.getAndIncrement();
            if (current >= 60) {
                FirePhoenixParticles.spawnPhoenixExplosion(session, context, center);
                WorldCompat.playSound(world, center, "entity.blaze.death", 2.0f, 0.8f);
                return false;
            }
            double radius = 0.3 + (current * 0.05);
            int particles = 15 + (current << 1);
            Color color = chargeColors[current % chargeColors.length];
            for (int i = 0; i < particles; i++) {
                double theta = Math.random() * 2.0 * Math.PI;
                double phi = Math.acos(2.0 * Math.random() - 1.0);
                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta);
                BukkitParticles.redstoneDust(world, center.clone().add(x, y, z), color, 1.5f);
                BukkitParticles.particle(world, "FLAME", center.clone().add(x * 0.8, y * 0.8, z * 0.8), 0, 0, 0, 0, 0.01);
            }
            if (current % 5 == 0) {
                WorldCompat.playSound(world, center, "item.firecharge.use", 1.5f, 1.2f - current * 0.02f);
            }
            return true;
        });
    }
}
