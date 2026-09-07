package com.monkey.ktplus.permission;

import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import java.util.Objects;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class EffectPermissionRegistrar {
    private EffectPermissionRegistrar() {}

    public static void register(JavaPlugin plugin, EffectRegistry registry) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(registry, "registry");
        PluginManager manager = plugin.getServer().getPluginManager();
        for (KillEffect effect : registry.all()) {
            String node = effect.definition().permissionNode();
            PermissionDefault defaults = EffectPermissionDefaults.forPrice(effect.definition().price());
            Permission existing = manager.getPermission(node);
            if (existing != null) {
                existing.setDefault(defaults);
                continue;
            }
            manager.addPermission(
                    new Permission(node, "Use the " + effect.definition().id() + " kill effect", defaults));
        }
    }
}
