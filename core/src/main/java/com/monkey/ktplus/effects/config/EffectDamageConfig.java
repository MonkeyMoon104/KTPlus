package com.monkey.ktplus.effects.config;

public final class EffectDamageConfig {
    private final boolean enabled;
    private final double value;
    private final double radius;
    private final long delayTicks;

    public EffectDamageConfig(boolean enabled, double value, double radius, long delayTicks) {
        this.enabled = enabled;
        this.value = value;
        this.radius = radius;
        this.delayTicks = delayTicks;
    }

    public static EffectDamageConfig disabled() {
        return new EffectDamageConfig(false, 0D, 0D, 0L);
    }

    public boolean enabled() {
        return enabled;
    }

    public double value() {
        return value;
    }

    public double radius() {
        return radius;
    }

    public long delayTicks() {
        return delayTicks;
    }
}
