package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.service.VisualsService;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Objects;
import org.bukkit.Color;
import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

public final class BridgeVisualsService implements VisualsService {
    private final VisualEffectService visuals;

    public BridgeVisualsService(VisualEffectService visuals) {
        this.visuals = Objects.requireNonNull(visuals, "visuals");
    }

    @Override
    public void particle(
            String name,
            Location location,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra,
            @Nullable Color color) {
        visuals.particle(name, location, count, offsetX, offsetY, offsetZ, extra, color);
    }

    @Override
    public void sound(String name, Location location, float volume, float pitch) {
        visuals.sound(name, location, volume, pitch);
    }

    @Override
    public void dust(
            Location location,
            Color color,
            float size,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra) {
        visuals.dust(location, color, size, count, offsetX, offsetY, offsetZ, extra);
    }

    @Override
    public void helix(
            String particleName,
            Location origin,
            double radius,
            double height,
            int turns,
            int points,
            @Nullable Color color) {
        visuals.helix(particleName, origin, radius, height, turns, points, color);
    }

    @Override
    public void torus(
            String particleName,
            Location origin,
            double majorRadius,
            double minorRadius,
            int majorSteps,
            int minorSteps,
            @Nullable Color color) {
        visuals.torus(particleName, origin, majorRadius, minorRadius, majorSteps, minorSteps, color);
    }

    @Override
    public void rose(
            String particleName,
            Location origin,
            double radius,
            int petals,
            int points,
            double y,
            @Nullable Color color) {
        visuals.rose(particleName, origin, radius, petals, points, y, color);
    }

    @Override
    public void sphereShell(
            String particleName,
            Location origin,
            double radius,
            int rings,
            int pointsPerRing,
            @Nullable Color color) {
        visuals.sphereShell(particleName, origin, radius, rings, pointsPerRing, color);
    }

    @Override
    public void beam(String particleName, Location from, Location to, int points, @Nullable Color color) {
        visuals.beam(particleName, from, to, points, color);
    }

    @Override
    public void circle(
            String particleName, Location origin, double radius, double y, int points, @Nullable Color color) {
        visuals.circle(particleName, origin, radius, y, points, color);
    }
}
