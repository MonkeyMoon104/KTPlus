package com.monkey.ktplus.api.service;

import com.monkey.ktplus.api.model.Effect;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Localized messages and effect display strings.
 *
 * <p>Keys are resolved from language files; missing keys typically fall back to the default
 * language or the raw key depending on configuration.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface LangService {
    /**
     * @return configured default language code
     */
    String defaultLanguage();

    /**
     * Resolves the language code for a sender (player preference or default).
     *
     * @param sender command sender, or {@code null} for the default language
     * @return language code
     */
    String language(@Nullable CommandSender sender);

    /**
     * Resolves a message key in the sender's language.
     *
     * @param sender recipient / language source
     * @param key message key
     * @return resolved message (may include MiniMessage / legacy formatting)
     */
    String message(CommandSender sender, String key);

    /**
     * Resolves a message key in the default language.
     *
     * @param key message key
     * @return resolved message
     */
    String message(String key);

    /**
     * Localized display name for an effect.
     *
     * @param player language source
     * @param effect effect
     * @return localized name
     */
    String effectName(Player player, Effect effect);

    /**
     * Localized description for an effect.
     *
     * @param player language source
     * @param effect effect
     * @return localized description
     */
    String effectDescription(Player player, Effect effect);

    /**
     * Resolves GUI copy with an explicit fallback string.
     *
     * @param player language source
     * @param key GUI text key
     * @param fallback text used when the key is missing
     * @return resolved or fallback text
     */
    String guiText(Player player, String key, String fallback);
}
