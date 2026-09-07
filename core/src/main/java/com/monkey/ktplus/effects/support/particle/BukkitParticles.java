package com.monkey.ktplus.effects.support.particle;

import java.util.Locale;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;

public final class BukkitParticles {
    private BukkitParticles() {}

    public static void redstoneDust(World world, Location location, Color color, float size) {
        redstoneDust(world, location, color, size, 0);
    }

    public static void redstoneDust(World world, Location location, Color color, float size, int count) {
        if (world == null || location == null) {
            return;
        }
        DustOptions dustOptions = new DustOptions(color != null ? color : Color.WHITE, size);
        world.spawnParticle(Particle.DUST, location, count, 0, 0, 0, 0, dustOptions);
    }

    public static void dust(Location location, Color color, float size) {
        dust(location, color, size, 0, 0, 0, 0, 0);
    }

    public static void dust(
            Location location,
            Color color,
            float size,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        DustOptions dustOptions = new DustOptions(color != null ? color : Color.WHITE, size);
        location.getWorld()
                .spawnParticle(Particle.DUST, location, count, offsetX, offsetY, offsetZ, extra, dustOptions);
    }

    public static void particle(
            World world,
            String name,
            Location location,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra,
            String... fallbacks) {
        spawnVoid(world, location, count, offsetX, offsetY, offsetZ, extra, prepend(name, fallbacks));
    }

    public static void particleSimple(World world, String name, Location location, int count, String... fallbacks) {
        spawnVoid(world, location, count, 0, 0, 0, 0, prepend(name, fallbacks));
    }

    public static void spawn(
            String particleName,
            Location location,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        spawnVoid(
                location.getWorld(), location, count, offsetX, offsetY, offsetZ, extra, new String[] {particleName});
    }

    private static void spawnVoid(
            World world,
            Location location,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra,
            String... names) {
        if (world == null || location == null) {
            return;
        }
        Particle particle = resolveParticle(names);
        if (particle == null || particle.getDataType() != Void.class) {
            return;
        }
        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    private static String[] prepend(String name, String[] fallbacks) {
        if (fallbacks == null || fallbacks.length == 0) {
            return new String[] {name};
        }
        String[] names = new String[fallbacks.length + 1];
        names[0] = name;
        System.arraycopy(fallbacks, 0, names, 1, fallbacks.length);
        return names;
    }

    private static @Nullable Particle resolveParticle(String... names) {
        for (String name : names) {
            if (name == null) {
                continue;
            }
            try {
                return Particle.valueOf(normalizeParticleName(name));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private static String normalizeParticleName(String name) {
        String value = name.toUpperCase(Locale.ROOT);
        if ("LARGE_SMOKE".equals(value)) {
            return "SMOKE_LARGE";
        }
        if ("SMOKE_NORMAL".equals(value)) {
            return "SMOKE";
        }
        if ("REDSTONE".equals(value)) {
            return "DUST";
        }
        return value;
    }
}
