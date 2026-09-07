package com.monkey.ktplus.packet;

import com.monkey.ktplus.libs.packetevents.packetevents.PacketEvents;
import com.monkey.ktplus.libs.packetevents.packetevents.settings.PacketEventsSettings;
import com.monkey.ktplus.libs.packetevents.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

public final class PacketEventsBootstrap {
    private static volatile boolean loaded;
    private static volatile boolean initialized;

    private PacketEventsBootstrap() {}

    public static void load(JavaPlugin plugin) {
        if (loaded) {
            return;
        }
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(
                plugin, new PacketEventsSettings().checkForUpdates(false)));
        PacketEvents.getAPI().load();
        loaded = true;
    }

    public static void init() {
        if (!loaded || initialized) {
            return;
        }
        PacketEvents.getAPI().init();
        initialized = true;
    }

    public static void terminate() {
        if (!loaded) {
            return;
        }
        if (initialized && PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
            initialized = false;
        }
        loaded = false;
    }
}
