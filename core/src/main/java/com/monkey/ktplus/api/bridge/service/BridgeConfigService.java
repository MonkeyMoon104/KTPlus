package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.ApiVersion;
import com.monkey.ktplus.api.bridge.EffectMappings;
import com.monkey.ktplus.api.model.EffectCategory;
import com.monkey.ktplus.api.service.ConfigService;
import com.monkey.ktplus.config.ConfigSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BridgeConfigService implements ConfigService {
    private final ConfigSnapshot config;

    public BridgeConfigService(ConfigSnapshot config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public BridgeConfigService withConfig(ConfigSnapshot config) {
        return new BridgeConfigService(config);
    }

    @Override
    public String configVersion() {
        String fromFile = config.main().getString("config-version");
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return ApiVersion.VERSION;
    }

    @Override
    public String defaultLanguage() {
        return config.defaultLanguage();
    }

    @Override
    public boolean cosmeticMode() {
        return config.cosmeticMode();
    }

    @Override
    public boolean effectsOnMobs() {
        return config.effectsOnMobs();
    }

    @Override
    public boolean economyEnabled() {
        return config.economyEnabled();
    }

    @Override
    public String economyProvider() {
        return config.economyProviderRaw();
    }

    @Override
    public int startingBalance() {
        return config.startingBalance();
    }

    @Override
    public int killRewardPlayer() {
        return config.killReward(true);
    }

    @Override
    public int killRewardMob() {
        return config.killReward(false);
    }

    @Override
    public long effectCooldownMillis() {
        return config.effectCooldownMillis();
    }

    @Override
    public boolean isWorldDisabled(String worldName) {
        return config.isWorldDisabled(worldName);
    }

    @Override
    public List<EffectCategory> effectCategories() {
        List<EffectCategory> result = new ArrayList<>();
        for (com.monkey.ktplus.effects.api.EffectCategory category : config.effectCategories()) {
            result.add(EffectMappings.toApi(category));
        }
        return result;
    }
}
