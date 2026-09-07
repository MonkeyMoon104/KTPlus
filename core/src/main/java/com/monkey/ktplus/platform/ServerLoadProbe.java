package com.monkey.ktplus.platform;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;

public final class ServerLoadProbe {
    private final Method getTps;

    public ServerLoadProbe() {
        Method resolved = null;
        try {
            resolved = Bukkit.getServer().getClass().getMethod("getTPS");
        } catch (ReflectiveOperationException ignored) {
        }
        this.getTps = resolved;
    }

    public double currentTps() {
        if (getTps == null) {
            return 20.0D;
        }
        try {
            Object value = getTps.invoke(Bukkit.getServer());
            if (value instanceof double[]) {
                double[] samples = (double[]) value;
                if (samples.length > 0) {
                    return samples[0];
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 20.0D;
    }

    public int adaptedHeavyCap(
            int configuredCap, boolean adaptiveEnabled, double lowThreshold, double criticalThreshold) {
        return adaptedHeavyCapWithTps(
                configuredCap, adaptiveEnabled, lowThreshold, criticalThreshold, currentTps());
    }

    static int adaptedHeavyCapWithTps(
            int configuredCap,
            boolean adaptiveEnabled,
            double lowThreshold,
            double criticalThreshold,
            double tps) {
        int cap = Math.max(1, configuredCap);
        if (!adaptiveEnabled) {
            return cap;
        }
        if (tps < criticalThreshold) {
            return Math.max(1, cap / 4);
        }
        if (tps < lowThreshold) {
            return Math.max(1, cap / 2);
        }
        return cap;
    }
}
