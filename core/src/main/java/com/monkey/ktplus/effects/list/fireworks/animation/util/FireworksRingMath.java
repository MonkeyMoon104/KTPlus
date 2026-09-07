package com.monkey.ktplus.effects.list.fireworks.animation.util;

public final class FireworksRingMath {
    public static final double RING_WIDTH = 1.0;

    private FireworksRingMath() {}

    public static double horizontalDistance(double centerX, double centerZ, int blockX, int blockZ) {
        double dx = blockX + 0.5 - centerX;
        double dz = blockZ + 0.5 - centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static int boundingRadius(double radius) {
        return (int) Math.ceil(radius + RING_WIDTH);
    }

    public static boolean isInExpandingRing(double distance, double radius, double speed) {
        double inner = Math.max(0.0, radius - speed);
        return distance >= inner && distance <= radius + RING_WIDTH * 0.5;
    }

    public static boolean shouldRestoreWave(double distance, double radius) {
        return distance < radius - RING_WIDTH;
    }

    public static boolean isWithinRadar(double distance, double radius) {
        return distance <= radius;
    }
}
