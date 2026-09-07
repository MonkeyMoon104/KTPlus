package com.monkey.ktplus.util.item;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

public final class SoundKeys {
    private SoundKeys() {}

    public static String normalize(@Nullable String sound) {
        if (sound == null || sound.isEmpty()) {
            return "";
        }
        String trimmed = sound.trim();
        if (trimmed.contains(":")) {
            int separator = trimmed.indexOf(':');
            String namespace = trimmed.substring(0, separator).toLowerCase(Locale.ROOT);
            String path = normalizePath(trimmed.substring(separator + 1));
            return namespace + ":" + path;
        }
        return normalizePath(trimmed);
    }

    private static String normalizePath(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        if (upper.startsWith("MUSIC_DISC_")) {
            return "music_disc." + upper.substring("MUSIC_DISC_".length()).toLowerCase(Locale.ROOT);
        }
        if (value.contains(".")) {
            return value.toLowerCase(Locale.ROOT);
        }
        return value.toLowerCase(Locale.ROOT).replace('_', '.');
    }
}
