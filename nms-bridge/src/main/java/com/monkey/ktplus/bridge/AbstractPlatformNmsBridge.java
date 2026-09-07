package com.monkey.ktplus.bridge;

import com.monkey.ktplus.common.gui.GuiBackend;
import com.monkey.ktplus.common.platform.PlatformCapability;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractPlatformNmsBridge implements IPlatformNmsBridge {
    private final VersionBridge compat;
    private final Set<PlatformCapability> capabilities;

    protected AbstractPlatformNmsBridge(VersionBridge compat, Set<PlatformCapability> capabilities) {
        this.compat = Objects.requireNonNull(compat, "compat");
        this.capabilities = Collections.unmodifiableSet(new HashSet<>(Objects.requireNonNull(capabilities, "capabilities")));
    }

    @Override
    public Set<PlatformCapability> capabilities() {
        return capabilities;
    }

    @Override
    public VersionBridge compat() {
        return compat;
    }

    @Override
    public GuiBackend preferredGuiBackend() {
        return GuiBackend.IF_MODERN;
    }
}
