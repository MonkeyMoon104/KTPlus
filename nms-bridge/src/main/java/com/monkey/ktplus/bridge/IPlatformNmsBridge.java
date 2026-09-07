package com.monkey.ktplus.bridge;

import com.monkey.ktplus.common.gui.GuiBackend;
import com.monkey.ktplus.common.platform.PlatformCapability;
import java.util.Set;

public interface IPlatformNmsBridge {
    Set<PlatformCapability> capabilities();

    VersionBridge compat();

    GuiBackend preferredGuiBackend();

    default boolean supports(PlatformCapability capability) {
        return capabilities().contains(capability);
    }
}
