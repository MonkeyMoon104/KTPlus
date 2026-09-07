package com.monkey.ktplus.bridge;

import com.monkey.ktplus.common.gui.GuiBackend;
import com.monkey.ktplus.common.platform.PlatformCapabilitySets;
import java.util.Set;

public final class StubNmsBridge implements IPlatformNmsBridge {
    private static final StubNmsBridge INSTANCE = new StubNmsBridge();

    private StubNmsBridge() {}

    public static StubNmsBridge instance() {
        return INSTANCE;
    }

    @Override
    public Set<com.monkey.ktplus.common.platform.PlatformCapability> capabilities() {
        return PlatformCapabilitySets.stub();
    }

    @Override
    public VersionBridge compat() {
        return new DefaultVersionBridge();
    }

    @Override
    public GuiBackend preferredGuiBackend() {
        return GuiBackend.IF_MODERN;
    }
}
