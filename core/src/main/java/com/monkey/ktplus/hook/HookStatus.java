package com.monkey.ktplus.hook;

public final class HookStatus {
    private final String name;
    private final boolean enabled;

    public HookStatus(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    public String name() {
        return name;
    }

    public boolean enabled() {
        return enabled;
    }
}
