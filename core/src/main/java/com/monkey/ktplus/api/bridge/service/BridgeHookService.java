package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.service.HookService;
import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.hook.HookManager;
import java.util.Objects;

public final class BridgeHookService implements HookService {
    private final HookManager hooks;
    private volatile ConfigSnapshot config;

    public BridgeHookService(HookManager hooks, ConfigSnapshot config) {
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.config = Objects.requireNonNull(config, "config");
    }

    public void reload(ConfigSnapshot config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public boolean vaultPresent() {
        return hooks.enabled("Vault");
    }

    @Override
    public boolean luckPermsPresent() {
        return hooks.enabled("LuckPerms");
    }

    @Override
    public boolean worldGuardPresent() {
        return hooks.enabled("WorldGuard");
    }

    @Override
    public boolean placeholderApiPresent() {
        return hooks.enabled("PlaceholderAPI");
    }

    @Override
    public boolean luckPermsGrantOnPurchase() {
        return config.luckPermsGrantOnPurchase();
    }

    @Override
    public String worldGuardBlockChangesMode() {
        return config.worldGuardBlockChangesRaw();
    }
}
