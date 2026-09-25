package com.monkey.ktplus.effects.api;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.lang.LangService;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class EffectContext {
    private final Player killer;
    private final @Nullable Entity victim;
    private final Location location;
    private final ConfigSnapshot config;
    private final LangService lang;

    public EffectContext(
            Player killer,
            @Nullable Entity victim,
            Location location,
            ConfigSnapshot config,
            LangService lang) {
        this.killer = Objects.requireNonNull(killer, "killer");
        this.victim = victim;
        this.location = Objects.requireNonNull(location, "location");
        this.config = Objects.requireNonNull(config, "config");
        this.lang = Objects.requireNonNull(lang, "lang");
    }

    public Player killer() {
        return killer;
    }

    public @Nullable Entity victim() {
        return victim;
    }

    public Location location() {
        return location;
    }

    public ConfigSnapshot config() {
        return config;
    }

    public LangService lang() {
        return lang;
    }
}
