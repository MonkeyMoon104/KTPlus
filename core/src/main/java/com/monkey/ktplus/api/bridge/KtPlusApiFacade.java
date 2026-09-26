package com.monkey.ktplus.api.bridge;

import com.monkey.ktplus.api.ApiVersion;
import com.monkey.ktplus.api.KtPlus;
import com.monkey.ktplus.api.bridge.service.BridgeAccessService;
import com.monkey.ktplus.api.bridge.service.BridgeAvailabilityService;
import com.monkey.ktplus.api.bridge.service.BridgeConditionService;
import com.monkey.ktplus.api.bridge.service.BridgeConfigService;
import com.monkey.ktplus.api.bridge.service.BridgeCooldownService;
import com.monkey.ktplus.api.bridge.service.BridgeEconomyService;
import com.monkey.ktplus.api.bridge.service.BridgeEffectRegistrationService;
import com.monkey.ktplus.api.bridge.service.BridgeEffectService;
import com.monkey.ktplus.api.bridge.service.BridgeGuiService;
import com.monkey.ktplus.api.bridge.service.BridgeHookService;
import com.monkey.ktplus.api.bridge.service.BridgeLangService;
import com.monkey.ktplus.api.bridge.service.BridgePermissionService;
import com.monkey.ktplus.api.bridge.service.BridgePlayerDataService;
import com.monkey.ktplus.api.bridge.service.BridgeRandomEventService;
import com.monkey.ktplus.api.bridge.service.BridgeResourcePackService;
import com.monkey.ktplus.api.bridge.service.BridgeReviewService;
import com.monkey.ktplus.api.bridge.service.BridgeRuntimeService;
import com.monkey.ktplus.api.service.AccessService;
import com.monkey.ktplus.api.service.AvailabilityService;
import com.monkey.ktplus.api.service.ConditionService;
import com.monkey.ktplus.api.service.ConfigService;
import com.monkey.ktplus.api.service.CooldownService;
import com.monkey.ktplus.api.service.EconomyService;
import com.monkey.ktplus.api.service.EffectService;
import com.monkey.ktplus.api.service.GuiService;
import com.monkey.ktplus.api.service.HookService;
import com.monkey.ktplus.api.service.LangService;
import com.monkey.ktplus.api.service.PermissionService;
import com.monkey.ktplus.api.service.PlayerDataService;
import com.monkey.ktplus.api.service.RandomEventService;
import com.monkey.ktplus.api.service.ResourcePackService;
import com.monkey.ktplus.api.service.ReviewService;
import com.monkey.ktplus.api.service.RuntimeService;
import com.monkey.ktplus.api.spi.EffectRegistrationService;
import com.monkey.ktplus.bootstrap.PluginBootstrap;
import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.runtime.EffectPlayPipeline;
import java.util.Objects;
import org.bukkit.plugin.Plugin;

public final class KtPlusApiFacade implements KtPlus {
    private final Plugin plugin;
    private final EffectService effects;
    private final EffectRegistrationService registration;
    private final PlayerDataService players;
    private final EconomyService economy;
    private final AccessService access;
    private final AvailabilityService availability;
    private final LangService lang;
    private final BridgeRuntimeService runtime;
    private volatile ConfigService config;
    private final PermissionService permissions;
    private final GuiService gui;
    private final CooldownService cooldowns;
    private final ConditionService conditions;
    private final BridgeHookService hooks;
    private final BridgeRandomEventService randomEvents;
    private final BridgeResourcePackService resourcePack;
    private final BridgeReviewService reviews;

    public KtPlusApiFacade(Plugin plugin, PluginBootstrap bootstrap, EffectPlayPipeline pipeline) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(bootstrap, "bootstrap");
        Objects.requireNonNull(pipeline, "pipeline");
        this.effects = new BridgeEffectService(bootstrap.registry());
        this.registration = new BridgeEffectRegistrationService(bootstrap.registry());
        this.players = new BridgePlayerDataService(bootstrap.users());
        this.economy = new BridgeEconomyService(bootstrap.economy(), bootstrap.registry());
        this.access =
                new BridgeAccessService(bootstrap.access(), bootstrap.selection(), bootstrap.registry());
        this.availability = new BridgeAvailabilityService(bootstrap.availability(), bootstrap.registry());
        this.lang = new BridgeLangService(bootstrap.lang());
        this.runtime = new BridgeRuntimeService(bootstrap.runtime(), bootstrap.registry(), pipeline);
        this.config = new BridgeConfigService(bootstrap.config());
        this.permissions = new BridgePermissionService(bootstrap.permission(), bootstrap.registry());
        this.gui = new BridgeGuiService(bootstrap.gui());
        this.cooldowns = new BridgeCooldownService(bootstrap.cooldowns());
        this.conditions = new BridgeConditionService(bootstrap.conditions(), bootstrap.registry());
        this.hooks = new BridgeHookService(bootstrap.hooks(), bootstrap.config());
        this.randomEvents = new BridgeRandomEventService(bootstrap.randomEvents(), bootstrap.config());
        this.resourcePack = new BridgeResourcePackService(bootstrap.config());
        this.reviews = new BridgeReviewService(bootstrap.reviews());
    }

    public void reload(ConfigSnapshot snapshot, EffectPlayPipeline pipeline) {
        this.config = new BridgeConfigService(snapshot);
        this.runtime.reload(pipeline);
        this.hooks.reload(snapshot);
        this.randomEvents.reload(snapshot);
        this.resourcePack.reload(snapshot);
        this.reviews.reload(null);
    }

    public void reloadReviews(com.monkey.ktplus.review.ReviewRewardService reviews) {
        this.reviews.reload(reviews);
    }

    @Override
    public String version() {
        return ApiVersion.VERSION;
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public EffectService effects() {
        return effects;
    }

    @Override
    public EffectRegistrationService registration() {
        return registration;
    }

    @Override
    public PlayerDataService players() {
        return players;
    }

    @Override
    public EconomyService economy() {
        return economy;
    }

    @Override
    public AccessService access() {
        return access;
    }

    @Override
    public AvailabilityService availability() {
        return availability;
    }

    @Override
    public LangService lang() {
        return lang;
    }

    @Override
    public RuntimeService runtime() {
        return runtime;
    }

    @Override
    public ConfigService config() {
        return config;
    }

    @Override
    public PermissionService permissions() {
        return permissions;
    }

    @Override
    public GuiService gui() {
        return gui;
    }

    @Override
    public CooldownService cooldowns() {
        return cooldowns;
    }

    @Override
    public ConditionService conditions() {
        return conditions;
    }

    @Override
    public HookService hooks() {
        return hooks;
    }

    @Override
    public RandomEventService randomEvents() {
        return randomEvents;
    }

    @Override
    public ResourcePackService resourcePack() {
        return resourcePack;
    }

    @Override
    public ReviewService reviews() {
        return reviews;
    }
}
