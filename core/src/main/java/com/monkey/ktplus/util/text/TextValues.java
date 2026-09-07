package com.monkey.ktplus.util.text;

import org.jspecify.annotations.Nullable;

public final class TextValues {
    private TextValues() {}

    public static boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    public static @Nullable String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String orElseIfBlank(@Nullable String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
