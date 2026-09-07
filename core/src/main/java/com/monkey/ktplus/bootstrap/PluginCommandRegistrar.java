package com.monkey.ktplus.bootstrap;

import com.monkey.ktplus.command.KtCommand;
import com.monkey.ktplus.command.KtCommandActions;
import com.monkey.ktplus.command.lamp.LampCommandBootstrap;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginCommandRegistrar {
    private PluginCommandRegistrar() {}

    public static void register(JavaPlugin plugin, PluginBootstrap bootstrap) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(bootstrap, "bootstrap");
        KtCommandActions actions = new KtCommandActions(bootstrap);
        if (LampCommandBootstrap.register(plugin, actions)) {
            return;
        }
        KtCommand ktCommand = new KtCommand(actions);
        PluginCommand command = plugin.getCommand("ktplus");
        if (command != null) {
            command.setExecutor(ktCommand);
            command.setTabCompleter(ktCommand);
        }
    }
}
