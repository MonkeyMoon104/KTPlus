package com.monkey.ktplus.lib;

import com.monkey.ktplus.util.text.TextValues;
import java.util.Locale;

public enum LibraryTrack {
    MODERN;

    public String folderName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LibraryTrack fromFolderName(String name) {
        if (TextValues.isBlank(name)) {
            throw new IllegalArgumentException("track folder name is blank");
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        if ("LEGACY".equals(normalized)) {
            return MODERN;
        }
        return valueOf(normalized);
    }
}
