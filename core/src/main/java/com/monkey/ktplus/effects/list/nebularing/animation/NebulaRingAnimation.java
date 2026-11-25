package com.monkey.ktplus.effects.list.nebularing.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.effects.visual.display.DisplayFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;

public final class NebulaRingAnimation {
    private static final int ORBIT_COUNT = 8;
    private static final int DURATION = 140;
    private static final Color MAGENTA = Color.fromRGB(200, 80, 255);
    private static final Color CYAN = Color.fromRGB(80, 200, 255);
    private static final Color PINK = Color.fromRGB(255, 120, 200);

    private NebulaRingAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.5, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("nebularing");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int duration = perks == null ? DURATION : Math.max(80, perks.getInt("duration-ticks", DURATION));
        int orbitCount = perks == null ? ORBIT_COUNT : Math.max(4, Math.min(12, perks.getInt("orbiter-count", ORBIT_COUNT)));

        List<Orbiter> orbiters = new ArrayList<>();
        Material[] mats = {Material.AMETHYST_SHARD, Material.ECHO_SHARD, Material.NETHER_STAR, Material.GLOW_BERRIES};
        Color[] glows = {MAGENTA, CYAN, PINK, MAGENTA};
        for (int i = 0; i < orbitCount; i++) {
            ItemDisplay display = DisplayFactory.item(origin, mats[i % mats.length], 0.35f, 0.35f, 0.35f);
            if (display == null) {
                continue;
            }
            DisplayFactory.glow(display, glows[i % glows.length]);
            session.trackEntity(display);
            orbiters.add(new Orbiter(display, (Math.PI * 2.0 * i) / orbitCount, 2.0 + (i % 3) * 0.4));
        }

        session.onCleanup(() -> {
            for (Orbiter o : orbiters) {
                DisplayFactory.remove(o.display);
            }
        });
        session.resetDeadline(duration + 20L);
        visuals.sound("AMBIENT_CRIMSON_FOREST_ADDITIONS", origin, 0.6f, 1.2f);
        visuals.sound("BLOCK_BEACON_AMBIENT", origin, 0.4f, 1.6f);

        AtomicInteger tick = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                for (Orbiter o : orbiters) {
                    DisplayFactory.remove(o.display);
                }
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= duration) {
                for (Orbiter o : orbiters) {
                    DisplayFactory.remove(o.display);
                }
                return false;
            }

            double spin = current * 0.052;
            for (Orbiter orb : orbiters) {
                double a = orb.phase + spin * (1.05 + orb.radius * 0.04);
                double y = Math.sin(a * 2 + current * 0.07) * 0.45;
                double r = orb.radius + Math.sin(current * 0.11 + orb.phase) * 0.08;
                Location at = origin.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                DisplayFactory.place(orb.display, at, 0.38f, 0.38f, 0.38f, (float) (a + spin * 0.2));
            }

            Color dustColor = current % 6 < 2 ? MAGENTA : current % 6 < 4 ? CYAN : PINK;
            visuals.torus("DUST", origin, 2.45, 0.42, 36, 12, dustColor);
            visuals.helix("DUST", origin, 1.1, 4.0, 2, 22, dustColor);
            if (current % 2 == 0) {
                visuals.sphereShell("END_ROD", origin, 1.8, 3, 10, CYAN);
            }
            return true;
        });
    }

    private static final class Orbiter {
        private final ItemDisplay display;
        private final double phase;
        private final double radius;

        private Orbiter(ItemDisplay display, double phase, double radius) {
            this.display = display;
            this.phase = phase;
            this.radius = radius;
        }
    }
}