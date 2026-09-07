package com.monkey.ktplus.hook;

import com.monkey.ktplus.access.effect.EffectAccessService;
import com.monkey.ktplus.economy.EconomyService;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.hook.luckperms.LuckPermsHook;
import com.monkey.ktplus.hook.placeholder.PlaceholderHook;
import com.monkey.ktplus.hook.worldguard.WorldGuardHook;
import com.monkey.ktplus.user.UserService;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class HookManager {
    private final Map<String, HookStatus> hooks = new HashMap<String, HookStatus>();
    private WorldGuardHook worldGuard =
            WorldGuardHook.create(WorldGuardHook.BlockChangeMode.RESPECT, "ktplus.worldguard.bypass", silentLogger());
    private LuckPermsHook luckPerms = LuckPermsHook.create(false, silentLogger());
    private PlaceholderHook placeholders = PlaceholderHook.disabled();

    public void refresh(
            JavaPlugin plugin,
            WorldGuardHook.BlockChangeMode worldGuardMode,
            String worldGuardBypassPermission,
            boolean luckPermsGrantOnPurchase,
            EconomyService economy,
            UserService users,
            EffectRegistry registry,
            EffectAccessService access) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(access, "access");
        Logger logger = plugin.getLogger();
        updatePresence("Vault");
        updatePresence("LuckPerms");
        updatePresence("WorldGuard");
        updatePresence("PlaceholderAPI");
        worldGuard = WorldGuardHook.create(worldGuardMode, worldGuardBypassPermission, logger);
        luckPerms = LuckPermsHook.create(luckPermsGrantOnPurchase, logger);
        placeholders.unregister();
        placeholders = PlaceholderHook.create(plugin, economy, users, registry, access, logger);
    }

    public boolean enabled(String name) {
        HookStatus status = hooks.get(name.toLowerCase(Locale.ROOT));
        return status != null && status.enabled();
    }

    public WorldGuardHook worldGuard() {
        return worldGuard;
    }

    public LuckPermsHook luckPerms() {
        return luckPerms;
    }

    public PlaceholderHook placeholders() {
        return placeholders;
    }

    private void updatePresence(String name) {
        hooks.put(name.toLowerCase(Locale.ROOT), new HookStatus(name, Bukkit.getPluginManager().isPluginEnabled(name)));
    }

    private static Logger silentLogger() {
        return Logger.getLogger("KTPlus-Hooks");
    }
}
