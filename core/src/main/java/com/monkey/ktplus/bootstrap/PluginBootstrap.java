package com.monkey.ktplus.bootstrap;

import com.monkey.ktplus.access.effect.EffectAccessService;
import com.monkey.ktplus.access.effect.EffectSelectionService;
import com.monkey.ktplus.access.platform.InvuiAccess;
import com.monkey.ktplus.access.platform.PlatformAccess;
import com.monkey.ktplus.bridge.VersionBridge;
import com.monkey.ktplus.command.lamp.EffectIdSuggestions;
import com.monkey.ktplus.common.gui.GuiBackend;
import com.monkey.ktplus.common.platform.PlatformCapability;
import com.monkey.ktplus.config.ConfigManager;
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
import com.monkey.ktplus.effects.registry.BuiltInEffectRegistrar;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.effects.runtime.EffectRuntime;
import com.monkey.ktplus.effects.runtime.block.TemporaryBlockService;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.event.random.RandomEventService;
import com.monkey.ktplus.gui.EffectGuiService;
import com.monkey.ktplus.gui.inventory.PendingInventoryRepository;
import com.monkey.ktplus.gui.inventory.PlayerInventoryGuard;
import com.monkey.ktplus.hook.HookManager;
import com.monkey.ktplus.hook.worldguard.WorldGuardHook;
import com.monkey.ktplus.listener.resourcepack.ResourcePackJoinListener;
import com.monkey.ktplus.logging.BootLogger;
import com.monkey.ktplus.logging.EffectBootReport;
import com.monkey.ktplus.logging.FlywayBootReport;
import com.monkey.ktplus.nms.NmsBridgeManager;
import com.monkey.ktplus.permission.EffectPermissionRegistrar;
import com.monkey.ktplus.permission.PermissionService;
import com.monkey.ktplus.platform.PlatformDetector;
import com.monkey.ktplus.platform.ServerPlatform;
import com.monkey.ktplus.review.ReviewRewardService;
import com.monkey.ktplus.scheduler.BukkitPlatformScheduler;
import com.monkey.ktplus.scheduler.FoliaPlatformScheduler;
import com.monkey.ktplus.scheduler.PlatformScheduler;
import com.monkey.ktplus.schematic.SchematicLibrary;
import com.monkey.ktplus.storage.DatabaseService;
import com.monkey.ktplus.storage.repository.KillCoinsRepository;
import com.monkey.ktplus.storage.repository.PlayerEffectRepository;
import com.monkey.ktplus.storage.repository.PurchaseRepository;
import com.monkey.ktplus.storage.repository.ReviewClaimRepository;
import com.monkey.ktplus.storage.repository.TemporaryBlockRepository;
import com.monkey.ktplus.task.CancellationReason;
import com.monkey.ktplus.task.TaskRegistry;
import com.monkey.ktplus.user.UserService;
import com.monkey.ktplus.util.OnceLogger;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class PluginBootstrap {
    private final JavaPlugin plugin;
    private ConfigManager configManager;
    private ConfigSnapshot config;
    private ServerPlatform platform;
    private VersionBridge versionBridge;
    private PlatformScheduler scheduler;
    private TaskRegistry taskRegistry;
    private TemporaryBlockService temporaryBlocks;
    private TemporaryBlockRepository temporaryBlockRepository;
    private VisualEffectService visuals;
    private DatabaseService database;
    private final com.monkey.ktplus.storage.migration.MigrationLock migrationLock =
            new com.monkey.ktplus.storage.migration.MigrationLock();
    private KillCoinsRepository killCoins;
    private UserService users;
    private EconomyService economy;
    private PermissionService permission;
    private EffectAccessService access;
    private EffectSelectionService selection;
    private HookManager hooks;
    private EffectRegistry registry;
    private @Nullable HeadCollectorService headCollector;
    private EffectRuntime runtime;
    private EffectGuiService gui;
    private CooldownService cooldowns;
    private RandomEventService randomEvents;
    private EffectConditionService conditions;
    private ResourcePackJoinListener resourcePackJoinListener;
    private @Nullable ReviewRewardService reviews;

    public PluginBootstrap(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void enable() {
        BootLogger boot = new BootLogger(plugin);
        try {
            boot.beginPhase(1, "Config", "Configuration");
            configManager = new ConfigManager(plugin);
            config = configManager.load();
            new ConfigIntegrityChecker(plugin).validate();
            boot.detail(
                    "Config",
                    "Files loaded -> main/messages/effects/economy/gui/database/events/resourcepack/performance");
            boot.detail(
                    "Config",
                    "Economy -> enabled="
                            + config.economyEnabled()
                            + " | provider="
                            + config.economyProviderRaw()
                            + " | starting-balance="
                            + config.startingBalance());
            boot.detail(
                    "Config",
                    "GUI -> rows="
                            + config.guiRows()
                            + " | effect-slots="
                            + config.guiEffectSlots().size()
                            + " | categories="
                            + config.effectCategories().size());
            boot.completePhase("config integrity ok");

            boot.beginPhase(2, "Platform", "Platform and NMS");
            platform = new PlatformDetector().detect();
            NmsBridgeManager.init(boot.logger());
            InvuiAccess.ensureBridgeReady(plugin.getClass().getClassLoader(), plugin.getLogger());
            versionBridge = PlatformAccess.compat();
            scheduler = createScheduler();
            taskRegistry = new TaskRegistry();
            String capabilities = platform.capabilities().stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(", "));
            boot.detail(
                    "Platform",
                    "Server -> MC="
                            + platform.minecraftVersion()
                            + " | Bukkit="
                            + platform.bukkitVersion()
                            + " | Folia="
                            + platform.has(PlatformCapability.FOLIA));
            boot.detail("Platform", "Capabilities -> " + (capabilities.isEmpty() ? "none" : capabilities));
            boot.detail(
                    "NMS",
                    "Bridge -> "
                            + versionBridge.id()
                            + " | supported="
                            + NmsBridgeManager.getSupportedVersions());
            boot.completePhase("platform ready | bridge=" + versionBridge.id());

            boot.beginPhase(3, "Storage", "Database and persistence");
            OnceLogger onceLogger = new OnceLogger(plugin.getLogger());
            visuals = new VisualEffectService(versionBridge, onceLogger);
            database = new DatabaseService(plugin);
            var migrateResult = database.start(config);
            temporaryBlockRepository = new TemporaryBlockRepository(database);
            temporaryBlocks = new TemporaryBlockService(temporaryBlockRepository);
            temporaryBlocks.restorePersisted();
            PlayerEffectRepository playerEffects = new PlayerEffectRepository(database);
            PurchaseRepository purchases = new PurchaseRepository(database);
            killCoins = new KillCoinsRepository(database, purchases, config.startingBalance());
            boot.detail(
                    "Storage",
                    "Dialect -> "
                            + database.dialect()
                            + " | pool="
                            + ("sqlite".equals(database.dialect())
                                    ? "1"
                                    : String.valueOf(Math.max(1, config.database().getInt("database.pool-size", 5)))));
            FlywayBootReport.log(boot, migrateResult);
            boot.detail("Storage", "Temp-blocks restore completed");
            boot.completePhase("storage online | dialect=" + database.dialect());

            boot.beginPhase(4, "Services", "Economy and runtime services");
            users = new UserService(playerEffects);
            EconomyProviderType providerType = EconomyProviderType.fromConfig(config.economyProviderRaw());
            BalanceProvider balanceProvider =
                    BalanceProviderFactory.create(providerType, killCoins, plugin.getLogger());
            economy = new EconomyService(config, killCoins, purchases, balanceProvider);
            permission = new PermissionService();
            access = new EffectAccessService(permission, economy);
            selection = new EffectSelectionService(access, economy, users);
            conditions = new EffectConditionService(config);
            cooldowns = new CooldownService();
            hooks = new HookManager();
            headCollector = new HeadCollectorService(scheduler, users, hooks, visuals);
            boot.detail(
                    "Economy",
                    "Provider -> "
                            + economy.providerId()
                            + " | enabled="
                            + economy.enabled()
                            + " | kill-reward player/mob="
                            + config.killReward(true)
                            + "/"
                            + config.killReward(false));
            boot.detail("Services", "Ready -> users, access, selection, conditions, cooldowns, head-collector");
            boot.completePhase("services ready | economy=" + economy.providerId());

            boot.beginPhase(5, "Effects", "Effect registry and categories");
            registry = new EffectRegistry();
            new BuiltInEffectRegistrar(
                            config, visuals, new SchematicLibrary(plugin), plugin.getLogger(), headCollector)
                    .registerAll(registry);
            int builtInCount = registry.all().size();
            new CustomEffectLoader(plugin, config, visuals, cooldowns).registerAll(registry);
            EffectPermissionRegistrar.register(plugin, registry);
            EffectBootReport.log(boot, config, registry, builtInCount);
            boot.completePhase("effects=" + registry.all().size() + " | permissions registered");

            boot.beginPhase(6, "Hooks", "Soft-depend integrations");
            hooks.refresh(
                    plugin,
                    worldGuardMode(),
                    config.worldGuardBypassPermission(),
                    config.luckPermsGrantOnPurchase(),
                    economy,
                    users,
                    registry,
                    access);
            economy.setLuckPermsHook(hooks.luckPerms());
            boot.detail(
                    "Hooks",
                    "Vault="
                            + onOff(hooks.enabled("Vault"))
                            + " | LuckPerms="
                            + onOff(hooks.enabled("LuckPerms"))
                            + " | WorldGuard="
                            + onOff(hooks.enabled("WorldGuard"))
                            + " | PlaceholderAPI="
                            + onOff(hooks.enabled("PlaceholderAPI")));
            boot.detail(
                    "Hooks",
                    "WorldGuard mode -> "
                            + worldGuardMode()
                            + " | PAPI expansion="
                            + onOff(hooks.placeholders().available())
                            + " | LuckPerms grant-on-purchase="
                            + config.luckPermsGrantOnPurchase());
            boot.completePhase("hooks refreshed");

            boot.beginPhase(7, "GUI", "GUI, commands and listeners");
            runtime = new EffectRuntime(
                    config,
                    scheduler,
                    taskRegistry,
                    temporaryBlocks,
                    hooks.worldGuard()::allowsProtectedAction);
            GuiBackend guiBackend = PlatformAccess.preferredGuiBackend(platform);
            gui = new EffectGuiService(
                    config,
                    registry,
                    users,
                    economy,
                    access,
                    selection,
                    new PlayerInventoryGuard(new PendingInventoryRepository(database)),
                    guiBackend,
                    onceLogger);
            randomEvents = new RandomEventService(config, economy, visuals);
            reviews = new ReviewRewardService(
                    plugin, scheduler, new ReviewClaimRepository(database), economy, config);
            PluginCommandRegistrar.register(plugin, this);
            resourcePackJoinListener = PluginListenerRegistrar.register(
                    plugin, this, config, gui, runtime, temporaryBlocks, cooldowns, hooks, headCollector, users);
            boot.detail("GUI", "Backend -> " + gui.backend() + " | rows=" + config.guiRows());
            boot.detail("Commands", "Registered -> /ktplus (+ aliases kt, killeffect) | Lamp suggestions wired");
            boot.detail("Listeners", "Kill/GUI/protection/resource-pack listeners active");
            boot.completePhase("gui=" + gui.backend() + " | commands+listeners ready");

            boot.beginPhase(8, "Boot", "Finalize");
            PluginPostEnableTasks.run(plugin, config.main(), scheduler, onceLogger, registry, boot);
            boot.detail("Boot", "Post-enable -> bStats, update-check, effect catalog export");
            boot.completePhase("ready");

            boot.complete();
        } catch (RuntimeException ex) {
            boot.fail(ex);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw ex;
        }
    }

    public void disable() {
        if (runtime != null) {
            runtime.cancelAll(CancellationReason.PLUGIN_DISABLE);
            runtime.entityRegistry().clear();
        }
        if (gui != null) {
            gui.closeAll();
        }
        if (taskRegistry != null) {
            taskRegistry.cancelAll();
        }
        EffectIdSuggestions.clear();
        if (headCollector != null) {
            headCollector.shutdown();
        }
        if (reviews != null) {
            reviews.shutdown();
        }
        if (hooks != null) {
            hooks.placeholders().unregister();
        }
        if (temporaryBlocks != null) {
            temporaryBlocks.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }

    public void reload() {
        config = PluginReloadSupport.reload(
                plugin,
                config,
                configManager,
                database,
                runtime,
                taskRegistry,
                gui,
                registry,
                visuals,
                cooldowns,
                killCoins,
                economy,
                hooks,
                users,
                access,
                headCollector,
                conditions,
                randomEvents,
                resourcePackJoinListener,
                reviews);
    }

    private WorldGuardHook.BlockChangeMode worldGuardMode() {
        return WorldGuardHook.BlockChangeMode.fromConfig(
                config.worldGuardBlockChangesRaw(), config.worldGuardRespectBuildFlag());
    }

    private static String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }

    public ConfigSnapshot config() {
        return config;
    }

    public EffectRegistry registry() {
        return registry;
    }

    public EffectRuntime runtime() {
        return runtime;
    }

    public EffectGuiService gui() {
        return gui;
    }

    public UserService users() {
        return users;
    }

    public EconomyService economy() {
        return economy;
    }

    public PermissionService permission() {
        return permission;
    }

    public EffectAccessService access() {
        return access;
    }

    public EffectSelectionService selection() {
        return selection;
    }

    public EffectConditionService conditions() {
        return conditions;
    }

    public @Nullable HeadCollectorService headCollector() {
        return headCollector;
    }

    public RandomEventService randomEvents() {
        return randomEvents;
    }

    public @Nullable ReviewRewardService reviews() {
        return reviews;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public DatabaseService database() {
        return database;
    }

    public void clearPlayerDataCaches() {
        if (killCoins != null) {
            killCoins.clearCaches();
        }
        if (users != null) {
            users.clearCache();
        }
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public TemporaryBlockRepository temporaryBlockRepository() {
        return temporaryBlockRepository;
    }

    public com.monkey.ktplus.storage.migration.MigrationLock migrationLock() {
        return migrationLock;
    }

    private PlatformScheduler createScheduler() {
        BukkitPlatformScheduler bukkit = new BukkitPlatformScheduler(plugin);
        if (platform.has(PlatformCapability.FOLIA)) {
            return new FoliaPlatformScheduler(plugin, bukkit);
        }
        return bukkit;
    }
}
