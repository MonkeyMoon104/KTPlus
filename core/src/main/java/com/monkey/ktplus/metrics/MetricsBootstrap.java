package com.monkey.ktplus.metrics;

import com.monkey.ktplus.logging.BootLogger;
import com.monkey.ktplus.logging.KtPlusLogging;
import java.util.Objects;
import java.util.logging.Logger;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class MetricsBootstrap {
    public static final int BSTATS_PLUGIN_ID = 26511;

    private MetricsBootstrap() {}

    public static void start(JavaPlugin plugin) {
        start(plugin, null);
    }

    public static void start(JavaPlugin plugin, @Nullable BootLogger boot) {
        Objects.requireNonNull(plugin, "plugin");
        Logger logger = plugin.getLogger();
        try {
            new Metrics(plugin, BSTATS_PLUGIN_ID);
            if (boot != null) {
                boot.detail("Metrics", "bStats started (id=" + BSTATS_PLUGIN_ID + ")");
            } else {
                KtPlusLogging.success(logger, "Metrics", "bStats started (id=" + BSTATS_PLUGIN_ID + ")");
            }
        } catch (Throwable error) {
            if (boot != null) {
                boot.warn("Metrics", "bStats unavailable -> " + error.getMessage());
            } else {
                KtPlusLogging.warn(logger, "Metrics", "bStats unavailable -> " + error);
            }
        }
    }
}
