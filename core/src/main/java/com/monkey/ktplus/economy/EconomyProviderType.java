package com.monkey.ktplus.economy;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

public enum EconomyProviderType {
    KILLCOINS,
    VAULT;

    public static EconomyProviderType fromConfig(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return KILLCOINS;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("VAULT".equals(normalized) || "EXTERNAL".equals(normalized)) {
            return VAULT;
        }
        return KILLCOINS;
    }
}
