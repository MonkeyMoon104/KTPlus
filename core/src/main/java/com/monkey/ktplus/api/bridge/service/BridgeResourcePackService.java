package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.service.ResourcePackService;
import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.resourcepack.ResourcePackSettings;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class BridgeResourcePackService implements ResourcePackService {
    private volatile ConfigSnapshot config;

    public BridgeResourcePackService(ConfigSnapshot config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public void reload(ConfigSnapshot config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public boolean enabled() {
        return settings().enabled();
    }

    @Override
    public boolean required() {
        return settings().required();
    }

    @Override
    public @Nullable String url() {
        return settings().url();
    }

    @Override
    public @Nullable String sha1() {
        return settings().sha1();
    }

    @Override
    public @Nullable String prompt() {
        return settings().prompt();
    }

    @Override
    public @Nullable String soundName(String key) {
        Objects.requireNonNull(key, "key");
        String configured = config.resourcePack().getString("resource_pack.sounds." + key + ".name");
        if (configured == null || configured.isBlank()) {
            return null;
        }
        return configured.trim();
    }

    private ResourcePackSettings settings() {
        return ResourcePackSettings.from(config.resourcePack());
    }
}
