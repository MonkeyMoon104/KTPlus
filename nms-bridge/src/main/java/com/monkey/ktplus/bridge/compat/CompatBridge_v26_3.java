package com.monkey.ktplus.bridge.compat;

import com.monkey.ktplus.bridge.AbstractVersionBridge;

public final class CompatBridge_v26_3 extends AbstractVersionBridge {
    public CompatBridge_v26_3() {
        particle("DUST", "DUST");
        entity("WARDEN", "WARDEN");
        material("MACE", "MACE");
    }

    @Override
    public String id() {
        return "v26_3";
    }

    @Override
    public boolean supports(String minecraftVersion) {
        return minecraftVersion != null
                && (minecraftVersion.equals("26.3") || minecraftVersion.startsWith("26.3."));
    }
}
