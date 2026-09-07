package com.monkey.ktplus.resourcepack;

import com.monkey.ktplus.util.net.ResourcePackSender;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.Nullable;

public final class ResourcePackSettings {
    private final boolean enabled;
    private final @Nullable String url;
    private final @Nullable String sha1;
    private final @Nullable UUID uuid;
    private final boolean required;
    private final @Nullable String prompt;

    private ResourcePackSettings(
            boolean enabled,
            @Nullable String url,
            @Nullable String sha1,
            @Nullable UUID uuid,
            boolean required,
            @Nullable String prompt) {
        this.enabled = enabled;
        this.url = url;
        this.sha1 = sha1;
        this.uuid = uuid;
        this.required = required;
        this.prompt = prompt;
    }

    public static ResourcePackSettings from(FileConfiguration config) {
        Objects.requireNonNull(config, "config");
        boolean enabled = config.getBoolean("resource_pack.settings.enabled", false);
        String url = trimToNull(config.getString("resource_pack.settings.url"));
        String sha1 = trimToNull(config.getString("resource_pack.settings.sha1"));
        UUID uuid = parseUuid(trimToNull(config.getString("resource_pack.settings.uuid")));
        boolean required = config.getBoolean("resource_pack.settings.required", false);
        String prompt = trimToNull(config.getString("resource_pack.settings.prompt"));
        return new ResourcePackSettings(enabled, url, sha1, uuid, required, prompt);
    }

    public static String soundName(FileConfiguration config, String key, String fallback) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(fallback, "fallback");
        String configured = config.getString("resource_pack.sounds." + key + ".name");
        if (configured == null || configured.trim().isEmpty()) {
            return fallback;
        }
        return configured.trim();
    }

    public boolean enabled() {
        return enabled;
    }

    public @Nullable String url() {
        return url;
    }

    public @Nullable String sha1() {
        return sha1;
    }

    public @Nullable UUID uuid() {
        return uuid;
    }

    public boolean required() {
        return required;
    }

    public @Nullable String prompt() {
        return prompt;
    }

    public boolean isValidForSend() {
        if (!enabled || url == null) {
            return false;
        }
        if (sha1 == null) {
            return false;
        }
        try {
            ResourcePackSender.sha1Bytes(sha1);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return uuid != null;
    }

    private static @Nullable String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static @Nullable UUID parseUuid(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
