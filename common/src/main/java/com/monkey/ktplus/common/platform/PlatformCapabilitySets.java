package com.monkey.ktplus.common.platform;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class PlatformCapabilitySets {
    private PlatformCapabilitySets() {}

    public static Set<PlatformCapability> modernPaper() {
        return Collections.unmodifiableSet(EnumSet.of(PlatformCapability.IF_MODERN, PlatformCapability.PAPER));
    }

    public static Set<PlatformCapability> modernV26() {
        return Collections.unmodifiableSet(
                EnumSet.of(PlatformCapability.IF_MODERN, PlatformCapability.PAPER, PlatformCapability.FOLIA));
    }

    public static Set<PlatformCapability> stub() {
        return Collections.emptySet();
    }
}
