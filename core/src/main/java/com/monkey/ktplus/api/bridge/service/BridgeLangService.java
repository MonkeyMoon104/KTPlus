package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.service.LangService;
import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class BridgeLangService implements LangService {
    private final com.monkey.ktplus.lang.LangService lang;

    public BridgeLangService(com.monkey.ktplus.lang.LangService lang) {
        this.lang = Objects.requireNonNull(lang, "lang");
    }

    @Override
    public String defaultLanguage() {
        return lang.defaultLanguage();
    }

    @Override
    public String language(@Nullable CommandSender sender) {
        return lang.languageCode(sender);
    }

    @Override
    public String message(CommandSender sender, String key) {
        return lang.message(sender, key);
    }

    @Override
    public String message(String key) {
        return lang.message(key);
    }

    @Override
    public String effectName(Player player, Effect effect) {
        Objects.requireNonNull(effect, "effect");
        return lang.effectName(player, effect.id(), effect.displayName());
    }

    @Override
    public String effectDescription(Player player, Effect effect) {
        Objects.requireNonNull(effect, "effect");
        return lang.effectDescription(player, effect.id());
    }

    @Override
    public String guiText(Player player, String key, String fallback) {
        return lang.guiText(player, key, fallback);
    }
}
