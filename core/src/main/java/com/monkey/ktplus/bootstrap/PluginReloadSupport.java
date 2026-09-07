package com.monkey.ktplus.bootstrap;

import com.monkey.ktplus.access.effect.EffectAccessService;
import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.config.validate.ConfigIntegrityChecker;
import com.monkey.ktplus.cooldown.CooldownService;
import com.monkey.ktplus.economy.EconomyProviderType;
import com.monkey.ktplus.economy.EconomyService;
import com.monkey.ktplus.economy.balance.BalanceProvider;
import com.monkey.ktplus.economy.balance.BalanceProviderFactory;
import com.monkey.ktplus.effects.condition.EffectConditionService;
import com.monkey.ktplus.effects.custom.CustomEffectLoader;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorService;
import com.monkey.ktplus.effects.runtime.CustomProjectileTracker;
import com.monkey.ktplus.effects.registry.BuiltInEffectRegistrar;
import com.monkey.ktplus.schematic.SchematicLibrary;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.runtime.EffectRuntime;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.event.random.RandomEventService;
import com.monkey.ktplus.export.EffectCatalogExporter;
import com.monkey.ktplus.gui.EffectGuiService;
import com.monkey.ktplus.hook.HookManager;
import com.monkey.ktplus.hook.worldguard.WorldGuardHook;
import com.monkey.ktplus.listener.resourcepack.ResourcePackJoinListener;
import com.monkey.ktplus.review.ReviewRewardService;
import com.monkey.ktplus.storage.DatabaseService;
import com.monkey.ktplus.storage.repository.KillCoinsRepository;
import com.monkey.ktplus.task.CancellationReason;
import com.monkey.ktplus.task.TaskRegistry;
import com.monkey.ktplus.user.UserService;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class PluginReloadSupport {
    private PluginReloadSupport() {}

    public static ConfigSnapshot reload(
            JavaPlugin plugin,
            ConfigSnapshot previous,
            com.monkey.ktplus.config.ConfigManager configManager,
            DatabaseService database,
            EffectRuntime runtime,
            @Nullable TaskRegistry taskRegistry,
            @Nullable EffectGuiService gui,
            EffectRegistry registry,
            VisualEffectService visuals,
            CooldownService cooldowns,
            KillCoinsRepository killCoins,
            EconomyService economy,
            HookManager hooks,
            UserService users,
            EffectAccessService access,
            @Nullable HeadCollectorService headCollector,
            EffectConditionService conditions,
            RandomEventService randomEvents,
            @Nullable ResourcePackJoinListener resourcePackJoinListener,
            @Nullable ReviewRewardService reviews) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(configManager, "configManager");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(visuals, "visuals");
        Objects.requireNonNull(cooldowns, "cooldowns");
        Objects.requireNonNull(killCoins, "killCoins");
        Objects.requireNonNull(economy, "economy");
        Objects.requireNonNull(hooks, "hooks");
        Objects.requireNonNull(users, "users");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(conditions, "conditions");
        Objects.requireNonNull(randomEvents, "randomEvents");

        runtime.cancelAll(CancellationReason.RELOAD);
        if (taskRegistry != null) {
            taskRegistry.cancelAll();
        }
        CustomProjectileTracker.clear();
        if (gui != null) {
            gui.closeAll();
        }
        if (headCollector != null) {
            headCollector.clearAll();
        }

        ConfigSnapshot config = configManager.load();
        new ConfigIntegrityChecker(plugin).validate();
        warnIfDatabaseChanged(plugin, previous, config, database);

        registry.clear();
        new BuiltInEffectRegistrar(config, visuals, new SchematicLibrary(plugin), plugin.getLogger(), headCollector)
                .registerAll(registry);
        new CustomEffectLoader(plugin, config, visuals, cooldowns).registerAll(registry);
        new EffectCatalogExporter(plugin).export(registry);

        EconomyProviderType providerType = EconomyProviderType.fromConfig(config.economyProviderRaw());
        BalanceProvider balanceProvider = BalanceProviderFactory.create(providerType, killCoins, plugin.getLogger());
        economy.reload(config, balanceProvider);

        WorldGuardHook.BlockChangeMode worldGuardMode = WorldGuardHook.BlockChangeMode.fromConfig(
                config.worldGuardBlockChangesRaw(),
                config.worldGuardRespectBuildFlag());
        hooks.refresh(
                plugin,
                worldGuardMode,
                config.worldGuardBypassPermission(),
                config.luckPermsGrantOnPurchase(),
                economy,
                users,
                registry,
                access);
        economy.setLuckPermsHook(hooks.luckPerms());
        runtime.reload(config, hooks.worldGuard()::allowsProtectedAction);
        conditions.reload(config);
        if (gui != null) {
            gui.reload(config, registry);
        }
        randomEvents.reload(config);
        if (reviews != null) {
            reviews.reload(config);
        }
        if (resourcePackJoinListener != null) {
            resourcePackJoinListener.reload(config);
        }
        return config;
    }

    private static void warnIfDatabaseChanged(
            JavaPlugin plugin, ConfigSnapshot previous, ConfigSnapshot next, DatabaseService database) {
        String previousType = previous.database().getString("database.type", "sqlite");
        String nextType = next.database().getString("database.type", "sqlite");
        if (previousType == null) {
            previousType = "sqlite";
        }
        if (nextType == null) {
            nextType = "sqlite";
        }
        if (!previousType.equalsIgnoreCase(nextType)
                || !database.dialect().equalsIgnoreCase(nextType.toLowerCase(Locale.ROOT))) {
            plugin.getLogger().warning(
                    "database.type changed (or differs from active pool); restart the server to apply storage changes");
        }
    }
}
