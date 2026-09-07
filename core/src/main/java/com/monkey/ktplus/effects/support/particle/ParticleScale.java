package com.monkey.ktplus.effects.support.particle;

import com.monkey.ktplus.platform.ServerLoadProbe;

public final class ParticleScale {
    private static final ServerLoadProbe PROBE = new ServerLoadProbe();

    private ParticleScale() {}

    public static int scale(int baseCount) {
        double tps = PROBE.currentTps();
        if (tps >= 18.0D) {
            return baseCount;
        }
        if (tps >= 15.0D) {
            return Math.max(1, (int) (baseCount * 0.6D));
        }
        return Math.max(1, (int) (baseCount * 0.35D));
    }
}
