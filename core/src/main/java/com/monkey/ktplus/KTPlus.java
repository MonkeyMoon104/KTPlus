package com.monkey.ktplus;

import com.monkey.ktplus.bootstrap.PluginBootstrap;
import com.monkey.ktplus.lib.RuntimeLibraryBootstrap;
import com.monkey.ktplus.packet.PacketEventsBootstrap;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class KTPlus extends JavaPlugin {
    private @Nullable PluginBootstrap bootstrap;
    private @Nullable Exception libraryLoadFailure;

    @Override
    public void onLoad() {
        try {
            RuntimeLibraryBootstrap.install(this);
            libraryLoadFailure = null;
        } catch (Exception error) {
            libraryLoadFailure = error;
            getLogger().severe("[Libs] Runtime library install failed: " + error.getMessage());
            error.printStackTrace();
        }
        try {
            PacketEventsBootstrap.load(this);
        } catch (Throwable error) {
            getLogger().warning("[PacketEvents] Failed to load: " + error.getMessage());
        }
    }

    @Override
    public void onEnable() {
        if (libraryLoadFailure != null) {
            getLogger().severe("Disabling KTPlus because runtime libraries failed to install.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            PacketEventsBootstrap.init();
        } catch (Throwable error) {
            getLogger().warning("[PacketEvents] Failed to init: " + error.getMessage());
        }
        bootstrap = new PluginBootstrap(this);
        bootstrap.enable();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.disable();
        }
        try {
            PacketEventsBootstrap.terminate();
        } catch (Throwable ignored) {
        }
    }
}