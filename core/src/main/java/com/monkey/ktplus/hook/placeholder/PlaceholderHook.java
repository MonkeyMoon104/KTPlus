package com.monkey.ktplus.hook.placeholder;

import com.monkey.ktplus.access.effect.EffectAccessService;
import com.monkey.ktplus.economy.EconomyService;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.logging.KtPlusLogging;
import com.monkey.ktplus.user.UserService;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class PlaceholderHook {
    private final boolean available;
    private final @Nullable Object expansion;

    private PlaceholderHook(boolean available, @Nullable Object expansion) {
        this.available = available;
        this.expansion = expansion;
    }

    public static PlaceholderHook disabled() {
        return new PlaceholderHook(false, null);
    }

    public static PlaceholderHook create(
            JavaPlugin plugin,
            EconomyService economy,
            UserService users,
            EffectRegistry registry,
            EffectAccessService access,
            Logger logger) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(economy, "economy");
        Objects.requireNonNull(users, "users");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(logger, "logger");
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi == null || !papi.isEnabled()) {
            return new PlaceholderHook(false, null);
        }
        try {
            Class<?> expansionClass = Class.forName("com.monkey.ktplus.hook.placeholder.KtExpansion");
            Object expansion = expansionClass
                    .getConstructor(
                            JavaPlugin.class,
                            EconomyService.class,
                            UserService.class,
                            EffectRegistry.class,
                            EffectAccessService.class)
                    .newInstance(plugin, economy, users, registry, access);
            Object registered = expansionClass.getMethod("register").invoke(expansion);
            if (registered instanceof Boolean && !(Boolean) registered) {
                KtPlusLogging.warn(logger, "Hooks", "PlaceholderAPI expansion register() returned false");
                return new PlaceholderHook(false, null);
            }
            KtPlusLogging.success(logger, "Hooks", "PlaceholderAPI expansion registered");
            return new PlaceholderHook(true, expansion);
        } catch (ReflectiveOperationException | RuntimeException error) {
            KtPlusLogging.warn(logger, "Hooks", "PlaceholderAPI hook failed -> " + error.getMessage());
            return new PlaceholderHook(false, null);
        }
    }

    public void unregister() {
        if (expansion == null) {
            return;
        }
        try {
            expansion.getClass().getMethod("unregister").invoke(expansion);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public boolean available() {
        return available;
    }
}
