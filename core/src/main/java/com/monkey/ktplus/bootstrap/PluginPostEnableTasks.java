package com.monkey.ktplus.bootstrap;

import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.export.EffectCatalogExporter;
import com.monkey.ktplus.logging.BootLogger;
import com.monkey.ktplus.metrics.MetricsBootstrap;
import com.monkey.ktplus.scheduler.PlatformScheduler;
import com.monkey.ktplus.update.UpdateChecker;
import com.monkey.ktplus.util.OnceLogger;
import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class PluginPostEnableTasks {
    private PluginPostEnableTasks() {}

    public static void run(
            JavaPlugin plugin,
            FileConfiguration main,
            PlatformScheduler scheduler,
            OnceLogger onceLogger,
            EffectRegistry registry) {
        run(plugin, main, scheduler, onceLogger, registry, null);
    }

    public static void run(
            JavaPlugin plugin,
            FileConfiguration main,
            PlatformScheduler scheduler,
            OnceLogger onceLogger,
            EffectRegistry registry,
            @Nullable BootLogger boot) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(main, "main");
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(onceLogger, "onceLogger");
        Objects.requireNonNull(registry, "registry");
        MetricsBootstrap.start(plugin, boot);
        new UpdateChecker(
                        plugin,
                        scheduler,
                        plugin.getPluginMeta().getVersion(),
                        UpdateChecker.SPIGOT_LEGACY_UPDATE_URL,
                        onceLogger)
                .schedule();
        new EffectCatalogExporter(plugin).export(registry);
        if (boot != null) {
            boot.detail("Update", "Update check scheduled");
            boot.detail("Effects", "Catalog export written");
        }
    }
}
