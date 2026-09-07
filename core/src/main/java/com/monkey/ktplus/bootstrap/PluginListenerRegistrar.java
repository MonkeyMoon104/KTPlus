package com.monkey.ktplus.bootstrap;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.cooldown.CooldownService;
import com.monkey.ktplus.effects.list.headcollector.HeadCollectorService;
import com.monkey.ktplus.effects.runtime.EffectRuntime;
import com.monkey.ktplus.effects.runtime.block.TemporaryBlockService;
import com.monkey.ktplus.gui.EffectGuiService;
import com.monkey.ktplus.hook.HookManager;
import com.monkey.ktplus.listener.effect.CustomProjectileHitListener;
import com.monkey.ktplus.listener.effect.EffectFireworkDamageListener;
import com.monkey.ktplus.listener.effect.EffectSkeletonGuardListener;
import com.monkey.ktplus.listener.effect.EffectSniperArrowListener;
import com.monkey.ktplus.listener.effect.EnchantDebrisListener;
import com.monkey.ktplus.listener.effect.EndEndermanKillListener;
import com.monkey.ktplus.listener.effect.HeadCollectorListener;
import com.monkey.ktplus.listener.gui.GuiListener;
import com.monkey.ktplus.listener.gui.PlayerInventoryGuardListener;
import com.monkey.ktplus.listener.KillListener;
import com.monkey.ktplus.listener.LifecycleListener;
import com.monkey.ktplus.listener.resourcepack.ResourcePackJoinListener;
import com.monkey.ktplus.listener.world.TemporaryBlockProtectionListener;
import com.monkey.ktplus.resourcepack.ResourcePackService;
import com.monkey.ktplus.user.UserService;
import java.util.Objects;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class PluginListenerRegistrar {
    private PluginListenerRegistrar() {}

    public static ResourcePackJoinListener register(
            JavaPlugin plugin,
            PluginBootstrap bootstrap,
            ConfigSnapshot config,
            EffectGuiService gui,
            EffectRuntime runtime,
            TemporaryBlockService temporaryBlocks,
            CooldownService cooldowns,
            HookManager hooks,
            @Nullable HeadCollectorService headCollector,
            UserService users) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(bootstrap, "bootstrap");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(gui, "gui");
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(temporaryBlocks, "temporaryBlocks");
        Objects.requireNonNull(cooldowns, "cooldowns");
        Objects.requireNonNull(hooks, "hooks");
        Objects.requireNonNull(users, "users");
        PluginManager manager = plugin.getServer().getPluginManager();
        ResourcePackJoinListener resourcePackJoinListener =
                new ResourcePackJoinListener(new ResourcePackService(plugin, config));
        manager.registerEvents(new GuiListener(gui), plugin);
        manager.registerEvents(new PlayerInventoryGuardListener(gui), plugin);
        manager.registerEvents(new KillListener(bootstrap, cooldowns), plugin);
        manager.registerEvents(new LifecycleListener(runtime, gui, temporaryBlocks, cooldowns), plugin);
        manager.registerEvents(new TemporaryBlockProtectionListener(temporaryBlocks), plugin);
        manager.registerEvents(new EffectSkeletonGuardListener(), plugin);
        manager.registerEvents(new EffectSniperArrowListener(), plugin);
        manager.registerEvents(
                new CustomProjectileHitListener(plugin, hooks, runtime.entityRegistry()), plugin);
        manager.registerEvents(
                new EffectFireworkDamageListener(plugin, hooks, runtime.entityRegistry()), plugin);
        manager.registerEvents(
                new EndEndermanKillListener(plugin, hooks, runtime.entityRegistry()), plugin);
        manager.registerEvents(new EnchantDebrisListener(), plugin);
        if (headCollector != null) {
            manager.registerEvents(new HeadCollectorListener(headCollector, users), plugin);
        }
        manager.registerEvents(resourcePackJoinListener, plugin);
        return resourcePackJoinListener;
    }
}
