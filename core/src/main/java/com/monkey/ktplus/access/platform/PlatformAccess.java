package com.monkey.ktplus.access.platform;

import com.monkey.ktplus.bridge.IPlatformNmsBridge;
import com.monkey.ktplus.bridge.VersionBridge;
import com.monkey.ktplus.common.gui.GuiBackend;
import com.monkey.ktplus.common.gui.GuiCompatibility;
import com.monkey.ktplus.common.platform.PlatformCapability;
import com.monkey.ktplus.nms.NmsBridgeManager;
import com.monkey.ktplus.platform.ServerPlatform;
import java.util.Set;

public final class PlatformAccess {
    private PlatformAccess() {}

    public static IPlatformNmsBridge bridge() {
        return NmsBridgeManager.get();
    }

    public static VersionBridge compat() {
        return NmsBridgeManager.compat();
    }

    public static Set<PlatformCapability> capabilities() {
        return bridge().capabilities();
    }

    public static boolean supports(PlatformCapability capability) {
        return bridge().supports(capability);
    }

    public static GuiBackend preferredGuiBackend(ServerPlatform platform) {
        return GuiCompatibility.resolveBackend(platform.minecraftVersion(), bridge().preferredGuiBackend());
    }
}
