package com.monkey.ktplus.api.service;

import org.bukkit.Color;
import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

/**
 * High-level particle and sound helpers for external effect authors.
 *
 * <p>Geometry helpers (helix, torus, rose, sphere shell, beam, circle) spawn one particle per
 * sample point. Prefer calling from the region / main thread of {@code location}.
 *
 * @since 4.0.5
 */
public interface VisualsService {
    /**
     * Spawns a named particle at {@code location}.
     *
     * @param name particle name (Bukkit / bridge alias)
     * @param location spawn location
     * @param count particle count
     * @param offsetX x spread
     * @param offsetY y spread
     * @param offsetZ z spread
     * @param extra particle speed / extra
     * @param color optional dust color when applicable
     */
    void particle(
            String name,
            Location location,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra,
            @Nullable Color color);

    /**
     * Plays a named sound at {@code location}.
     *
     * @param name sound name (Bukkit / bridge alias)
     * @param location sound origin
     * @param volume volume
     * @param pitch pitch
     */
    void sound(String name, Location location, float volume, float pitch);

    /**
     * Spawns a dust particle with explicit size.
     *
     * @param location spawn location
     * @param color dust color
     * @param size dust size
     * @param count particle count
     * @param offsetX x spread
     * @param offsetY y spread
     * @param offsetZ z spread
     * @param extra particle extra
     */
    void dust(
            Location location,
            Color color,
            float size,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra);

    /**
     * Helix path of particles rising along {@code height}.
     *
     * @param particleName particle name
     * @param origin base location
     * @param radius helix radius
     * @param height vertical span
     * @param turns number of turns
     * @param points sample count
     * @param color optional color
     */
    void helix(
            String particleName,
            Location origin,
            double radius,
            double height,
            int turns,
            int points,
            @Nullable Color color);

    /**
     * Torus (donut) shell of particles.
     *
     * @param particleName particle name
     * @param origin center
     * @param majorRadius major radius
     * @param minorRadius tube radius
     * @param majorSteps samples around the ring
     * @param minorSteps samples around the tube
     * @param color optional color
     */
    void torus(
            String particleName,
            Location origin,
            double majorRadius,
            double minorRadius,
            int majorSteps,
            int minorSteps,
            @Nullable Color color);

    /**
     * Polar rose curve in the XZ plane at height {@code y}.
     *
     * @param particleName particle name
     * @param origin center
     * @param radius max radius
     * @param petals petal count
     * @param points sample count
     * @param y vertical offset from origin
     * @param color optional color
     */
    void rose(
            String particleName,
            Location origin,
            double radius,
            int petals,
            int points,
            double y,
            @Nullable Color color);

    /**
     * Sphere shell of particles.
     *
     * @param particleName particle name
     * @param origin center
     * @param radius shell radius
     * @param rings latitude rings
     * @param pointsPerRing samples per ring
     * @param color optional color
     */
    void sphereShell(
            String particleName,
            Location origin,
            double radius,
            int rings,
            int pointsPerRing,
            @Nullable Color color);

    /**
     * Straight beam of particles from {@code from} to {@code to}.
     *
     * @param particleName particle name
     * @param from start
     * @param to end
     * @param points sample count
     * @param color optional color
     */
    void beam(String particleName, Location from, Location to, int points, @Nullable Color color);

    /**
     * Horizontal circle of particles.
     *
     * @param particleName particle name
     * @param origin center
     * @param radius circle radius
     * @param y vertical offset from origin
     * @param points sample count
     * @param color optional color
     */
    void circle(
            String particleName, Location origin, double radius, double y, int points, @Nullable Color color);
}
