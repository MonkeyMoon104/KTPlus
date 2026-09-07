package com.monkey.ktplus.bridge;

public final class DefaultVersionBridge extends AbstractVersionBridge {
    @Override
    public String id() {
        return "default";
    }

    @Override
    public boolean supports(String minecraftVersion) {
        return true;
    }
}
