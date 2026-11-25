package com.monkey.ktplus.effects.visual.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class FxGeometry {
    private FxGeometry() {}

    public static void helix(
            Location origin, double radius, double height, int turns, int points, Consumer<Location> consumer) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(consumer, "consumer");
        if (origin.getWorld() == null || points <= 0) {
            return;
        }
        for (int i = 0; i < points; i++) {
            double t = (double) i / (double) points;
            double angle = t * turns * Math.PI * 2.0;
            double y = t * height;
            consumer.accept(origin.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
        }
    }

    public static void torus(
            Location origin, double majorRadius, double minorRadius, int majorSteps, int minorSteps, Consumer<Location> consumer) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(consumer, "consumer");
        if (origin.getWorld() == null || majorSteps <= 0 || minorSteps <= 0) {
            return;
        }
        for (int i = 0; i < majorSteps; i++) {
            double theta = (Math.PI * 2.0 * i) / majorSteps;
            double cx = Math.cos(theta) * majorRadius;
            double cz = Math.sin(theta) * majorRadius;
            for (int j = 0; j < minorSteps; j++) {
                double phi = (Math.PI * 2.0 * j) / minorSteps;
                double x = cx + Math.cos(phi) * Math.cos(theta) * minorRadius;
                double y = Math.sin(phi) * minorRadius;
                double z = cz + Math.cos(phi) * Math.sin(theta) * minorRadius;
                consumer.accept(origin.clone().add(x, y, z));
            }
        }
    }

    public static void rose(
            Location origin, double radius, int petals, int points, double y, Consumer<Location> consumer) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(consumer, "consumer");
        if (origin.getWorld() == null || points <= 0 || petals <= 0) {
            return;
        }
        for (int i = 0; i < points; i++) {
            double t = (Math.PI * 2.0 * i) / points;
            double r = radius * Math.cos(petals * t);
            consumer.accept(origin.clone().add(Math.cos(t) * r, y, Math.sin(t) * r));
        }
    }

    public static void sphereShell(Location origin, double radius, int rings, int pointsPerRing, Consumer<Location> consumer) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(consumer, "consumer");
        if (origin.getWorld() == null || rings <= 0 || pointsPerRing <= 0) {
            return;
        }
        for (int ring = 0; ring <= rings; ring++) {
            double phi = Math.PI * ring / (double) rings;
            double y = Math.cos(phi) * radius;
            double ringRadius = Math.sin(phi) * radius;
            for (int i = 0; i < pointsPerRing; i++) {
                double theta = (Math.PI * 2.0 * i) / pointsPerRing;
                consumer.accept(origin.clone().add(Math.cos(theta) * ringRadius, y, Math.sin(theta) * ringRadius));
            }
        }
    }

    public static void beam(Location from, Location to, int points, Consumer<Location> consumer) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(consumer, "consumer");
        if (from.getWorld() == null || points <= 0) {
            return;
        }
        Vector delta = to.toVector().subtract(from.toVector());
        for (int i = 0; i <= points; i++) {
            double t = (double) i / (double) points;
            consumer.accept(from.clone().add(delta.clone().multiply(t)));
        }
    }

    public static void circle(Location origin, double radius, double y, int points, Consumer<Location> consumer) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(consumer, "consumer");
        if (origin.getWorld() == null || points <= 0) {
            return;
        }
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            consumer.accept(origin.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
        }
    }

    public static List<Location> sampleCircle(Location origin, double radius, double y, int points) {
        List<Location> out = new ArrayList<>(Math.max(0, points));
        circle(origin, radius, y, points, out::add);
        return out;
    }

    public static Vector orbitOffset(double radius, double angleRad, double y) {
        return new Vector(Math.cos(angleRad) * radius, y, Math.sin(angleRad) * radius);
    }
}
