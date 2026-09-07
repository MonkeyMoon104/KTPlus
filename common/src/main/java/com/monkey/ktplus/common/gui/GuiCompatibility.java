package com.monkey.ktplus.common.gui;

import org.jspecify.annotations.Nullable;

public final class GuiCompatibility {
    private GuiCompatibility() {}

    public static boolean supportsIfModern(@Nullable String minecraftVersion) {
        if (minecraftVersion == null || minecraftVersion.isEmpty() || "unknown".equalsIgnoreCase(minecraftVersion)) {
            return false;
        }
        if (minecraftVersion.startsWith("26.")) {
            return true;
        }
        return minecraftVersion.startsWith("1.21");
    }

    public static GuiBackend resolveBackend(@Nullable String minecraftVersion, GuiBackend bridgePreference) {
        return bridgePreference;
    }

    public static String formatOpenFailure(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String detail = root.getMessage();
        if (detail == null || detail.isEmpty()) {
            detail = root.toString();
        }
        return root.getClass().getSimpleName() + ": " + detail;
    }
}
