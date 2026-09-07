package com.monkey.ktplus.platform;

import com.monkey.ktplus.common.platform.PlatformCapability;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class ServerPlatform {
    private final String minecraftVersion;
    private final String bukkitVersion;
    private final EnumSet<PlatformCapability> capabilities;

    public ServerPlatform(String minecraftVersion, String bukkitVersion, EnumSet<PlatformCapability> capabilities) {
        this.minecraftVersion = minecraftVersion;
        this.bukkitVersion = bukkitVersion;
        this.capabilities = capabilities.clone();
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public String bukkitVersion() {
        return bukkitVersion;
    }

    public boolean has(PlatformCapability capability) {
        return capabilities.contains(capability);
    }

    public Set<PlatformCapability> capabilities() {
        return new HashSet<>(capabilities);
    }
}
