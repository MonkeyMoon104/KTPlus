package com.monkey.ktplus.bridge.compat;

import com.monkey.ktplus.bridge.AbstractVersionBridge;

public final class CompatBridge_v26_2 extends AbstractVersionBridge {
    public CompatBridge_v26_2() {
        particle("DUST", "DUST");
        entity("WARDEN", "WARDEN");
        material("MACE", "MACE");
    }

    @Override
    public String id() {
        return "v26_2";
    }

    @Override
    public boolean supports(String minecraftVersion) {
        return minecraftVersion != null && minecraftVersion.startsWith("26.");
    }
}

