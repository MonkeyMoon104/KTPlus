package com.monkey.ktplus.bridge.compat;

import com.monkey.ktplus.bridge.AbstractVersionBridge;

public final class CompatBridge_v1_21 extends AbstractVersionBridge {
    public CompatBridge_v1_21() {
        particle("DUST", "DUST");
        entity("WARDEN", "WARDEN");
        material("MACE", "MACE");
    }

    @Override
    public String id() {
        return "v1_21";
    }

    @Override
    public boolean supports(String minecraftVersion) {
        if (minecraftVersion == null || minecraftVersion.startsWith("26.")) {
            return false;
        }
        return minecraftVersion.startsWith("1.21")
                || minecraftVersion.startsWith("1.22")
                || minecraftVersion.startsWith("1.23")
                || minecraftVersion.startsWith("1.24")
                || minecraftVersion.startsWith("1.25")
                || minecraftVersion.startsWith("1.26");
    }
}

