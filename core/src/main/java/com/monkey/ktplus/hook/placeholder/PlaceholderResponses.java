package com.monkey.ktplus.hook.placeholder;

import com.monkey.ktplus.effects.api.EffectDefinition;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

final class PlaceholderResponses {
    private PlaceholderResponses() {}

    static @Nullable String resolve(String params, @Nullable Player player, PlaceholderSource source) {
        Objects.requireNonNull(params, "params");
        Objects.requireNonNull(source, "source");
        String key = params.toLowerCase(Locale.ROOT).trim();
        if (key.isEmpty()) {
            return null;
        }

        return switch (key) {
            case "version" -> source.version();
            case "name", "plugin" -> source.pluginName();
            case "author", "authors" -> source.authors();
            case "identifier" -> source.identifier();
            case "provider" -> source.providerId();
            case "economy", "economy_enabled" -> yesNo(source.economyEnabled());
            case "effect_count", "effects", "total_effects" -> Integer.toString(source.effectCount());
            default -> player == null ? null : resolvePlayer(key, player, source);
        };
    }

    private static @Nullable String resolvePlayer(String key, Player player, PlaceholderSource source) {
        String selectedId = source.selectedEffectId(player).orElse("");
        EffectDefinition selected = source.effect(selectedId).orElse(null);

        if ("balance".equals(key) || "coins".equals(key)) {
            return Long.toString(source.balance(player));
        }
        if ("balance_formatted".equals(key) || "coins_formatted".equals(key)) {
            return String.format(Locale.US, "%,d", source.balance(player));
        }
        if ("selected".equals(key) || "effect".equals(key) || "selected_id".equals(key) || "effect_id".equals(key)) {
            return selectedId;
        }
        if ("selected_name".equals(key) || "effect_name".equals(key)) {
            return selected == null ? "" : selected.displayName();
        }
        if ("selected_price".equals(key) || "effect_price".equals(key)) {
            return selected == null ? "0" : Integer.toString(selected.price());
        }
        if ("selected_premium_price".equals(key) || "effect_premium_price".equals(key)) {
            return selected == null ? "0" : Integer.toString(selected.premiumPrice());
        }
        if ("selected_category".equals(key) || "effect_category".equals(key)) {
            return selected == null ? "" : selected.category().configId();
        }
        if ("selected_heavy".equals(key) || "effect_heavy".equals(key)) {
            return yesNo(selected != null && selected.heavy());
        }
        if ("has_selected".equals(key) || "selected_bool".equals(key)) {
            return yesNo(!selectedId.isBlank());
        }
        if ("unlocked_count".equals(key) || "owned_count".equals(key)) {
            return Integer.toString(source.unlockedCount(player));
        }
        if ("locked_count".equals(key)) {
            return Integer.toString(Math.max(0, source.effectCount() - source.unlockedCount(player)));
        }

        String ownedId = stripPrefix(key, "owns_", "owned_", "has_");
        if (ownedId != null) {
            return source.effect(ownedId)
                    .map(definition -> yesNo(source.ownsOrFree(player, definition)))
                    .orElse("no");
        }

        String unlockedId = stripPrefix(key, "unlocked_", "can_use_", "can_activate_");
        if (unlockedId != null) {
            return source.effect(unlockedId)
                    .map(definition -> yesNo(source.canActivate(player, definition)))
                    .orElse("no");
        }

        String priceId = stripPrefix(key, "price_");
        if (priceId != null) {
            return source.effect(priceId).map(definition -> Integer.toString(definition.price())).orElse("");
        }

        String premiumId = stripPrefix(key, "premium_price_");
        if (premiumId != null) {
            return source.effect(premiumId)
                    .map(definition -> Integer.toString(definition.premiumPrice()))
                    .orElse("");
        }

        String nameId = stripPrefix(key, "name_");
        if (nameId != null) {
            return source.effect(nameId).map(EffectDefinition::displayName).orElse("");
        }

        String categoryId = stripPrefix(key, "category_");
        if (categoryId != null) {
            return source.effect(categoryId).map(definition -> definition.category().configId()).orElse("");
        }

        String heavyId = stripPrefix(key, "heavy_");
        if (heavyId != null) {
            return source.effect(heavyId).map(definition -> yesNo(definition.heavy())).orElse("no");
        }

        String selectedMatchId = stripPrefix(key, "is_selected_");
        if (selectedMatchId != null) {
            return yesNo(!selectedId.isBlank() && selectedId.equalsIgnoreCase(selectedMatchId));
        }

        return null;
    }

    private static @Nullable String stripPrefix(String key, String... prefixes) {
        for (String prefix : prefixes) {
            if (key.startsWith(prefix) && key.length() > prefix.length()) {
                return key.substring(prefix.length());
            }
        }
        return null;
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    interface PlaceholderSource {
        String identifier();

        String pluginName();

        String authors();

        String version();

        String providerId();

        boolean economyEnabled();

        int effectCount();

        long balance(Player player);

        Optional<String> selectedEffectId(Player player);

        Optional<EffectDefinition> effect(String id);

        boolean ownsOrFree(Player player, EffectDefinition definition);

        boolean canActivate(Player player, EffectDefinition definition);

        int unlockedCount(Player player);
    }
}
