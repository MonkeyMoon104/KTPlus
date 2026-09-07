package com.monkey.ktplus.hook.placeholder;

import com.monkey.ktplus.access.effect.EffectAccessService;
import com.monkey.ktplus.economy.EconomyService;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.user.UserService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;
import io.papermc.paper.plugin.configuration.PluginMeta;

public final class KtExpansion extends PlaceholderExpansion {
    private static final List<String> PLACEHOLDERS = List.of(
            "version",
            "name",
            "plugin",
            "author",
            "authors",
            "identifier",
            "provider",
            "economy",
            "economy_enabled",
            "effect_count",
            "effects",
            "total_effects",
            "balance",
            "coins",
            "balance_formatted",
            "coins_formatted",
            "selected",
            "effect",
            "selected_id",
            "effect_id",
            "selected_name",
            "effect_name",
            "selected_price",
            "effect_price",
            "selected_premium_price",
            "effect_premium_price",
            "selected_category",
            "effect_category",
            "selected_heavy",
            "effect_heavy",
            "has_selected",
            "selected_bool",
            "unlocked_count",
            "owned_count",
            "locked_count",
            "owns_<id>",
            "unlocked_<id>",
            "price_<id>",
            "premium_price_<id>",
            "name_<id>",
            "category_<id>",
            "heavy_<id>",
            "is_selected_<id>");

    private final PlaceholderResponses.PlaceholderSource source;

    public KtExpansion(
            JavaPlugin plugin,
            EconomyService economy,
            UserService users,
            EffectRegistry registry,
            EffectAccessService access) {
        this.source = new PluginPlaceholderSource(
                Objects.requireNonNull(plugin, "plugin"),
                Objects.requireNonNull(economy, "economy"),
                Objects.requireNonNull(users, "users"),
                Objects.requireNonNull(registry, "registry"),
                Objects.requireNonNull(access, "access"));
    }

    @Override
    public String getIdentifier() {
        return source.identifier();
    }

    @Override
    public String getAuthor() {
        return source.authors();
    }

    @Override
    public String getVersion() {
        return source.version();
    }

    @Override
    public @Nullable String getRequiredPlugin() {
        return source.pluginName();
    }

    @Override
    public List<String> getPlaceholders() {
        return PLACEHOLDERS;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, String params) {
        Player online = player instanceof Player p ? p : null;
        if (online == null && player != null && player.isOnline()) {
            online = player.getPlayer();
        }
        return PlaceholderResponses.resolve(params, online, source);
    }

    private static final class PluginPlaceholderSource implements PlaceholderResponses.PlaceholderSource {
        private final String identifier;
        private final String pluginName;
        private final String authors;
        private final String version;
        private final EconomyService economy;
        private final UserService users;
        private final EffectRegistry registry;
        private final EffectAccessService access;

        private PluginPlaceholderSource(
                JavaPlugin plugin,
                EconomyService economy,
                UserService users,
                EffectRegistry registry,
                EffectAccessService access) {
            PluginMeta meta = plugin.getPluginMeta();
            this.pluginName = meta.getName();
            this.identifier = pluginName.toLowerCase(Locale.ROOT).replace(' ', '_');
            this.authors = meta.getAuthors().isEmpty() ? pluginName : String.join(", ", meta.getAuthors());
            this.version = meta.getVersion();
            this.economy = economy;
            this.users = users;
            this.registry = registry;
            this.access = access;
        }

        @Override
        public String identifier() {
            return identifier;
        }

        @Override
        public String pluginName() {
            return pluginName;
        }

        @Override
        public String authors() {
            return authors;
        }

        @Override
        public String version() {
            return version;
        }

        @Override
        public String providerId() {
            return economy.providerId();
        }

        @Override
        public boolean economyEnabled() {
            return economy.enabled();
        }

        @Override
        public int effectCount() {
            return registry.all().size();
        }

        @Override
        public long balance(Player player) {
            return economy.balance(player);
        }

        @Override
        public Optional<String> selectedEffectId(Player player) {
            return users.selectedEffect(player);
        }

        @Override
        public Optional<EffectDefinition> effect(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            return registry.find(id).map(KillEffect::definition);
        }

        @Override
        public boolean ownsOrFree(Player player, EffectDefinition definition) {
            return economy.ownsOrFree(player, definition);
        }

        @Override
        public boolean canActivate(Player player, EffectDefinition definition) {
            return access.canActivate(player, definition);
        }

        @Override
        public int unlockedCount(Player player) {
            int count = 0;
            for (KillEffect effect : registry.all()) {
                if (access.canActivate(player, effect.definition())) {
                    count++;
                }
            }
            return count;
        }
    }
}
