package com.monkey.ktplus.platform;

import com.monkey.ktplus.access.runtime.MinecraftVersionAccess;
import com.monkey.ktplus.common.platform.PlatformCapability;
import java.util.EnumSet;
import java.util.Objects;
import org.bukkit.Bukkit;

public final class PlatformDetector {
    public ServerPlatform detect() {
        EnumSet<PlatformCapability> capabilities = EnumSet.noneOf(PlatformCapability.class);
        if (classExists("com.destroystokyo.paper.PaperConfig")
                || classExists("io.papermc.paper.configuration.Configuration")
                || classExists("com.destroystokyo.paper.event.player.PlayerJumpEvent")) {
            capabilities.add(PlatformCapability.PAPER);
        }
        if (classExists("org.bukkit.entity.Display")) {
            capabilities.add(PlatformCapability.DISPLAY_ENTITIES);
        }
        if (classExists("io.papermc.paper.threadedregions.RegionizedServer")) {
            capabilities.add(PlatformCapability.FOLIA);
            capabilities.add(PlatformCapability.REGION_SCHEDULER);
            capabilities.add(PlatformCapability.ENTITY_SCHEDULER);
        }
        return new ServerPlatform(MinecraftVersionAccess.minecraftVersion(), safeBukkitVersion(), capabilities);
    }

    private String safeBukkitVersion() {
        try {
            return Bukkit.getBukkitVersion();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    private boolean classExists(String name) {
        Objects.requireNonNull(name, "name");
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
