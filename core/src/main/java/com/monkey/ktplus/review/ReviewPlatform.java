package com.monkey.ktplus.review;

import java.util.Locale;

public enum ReviewPlatform {
    GITHUB("github"),
    SPIGOT("spigot");

    private final String id;

    ReviewPlatform(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ReviewPlatform parse(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        if ("github".equals(key) || "gh".equals(key) || "star".equals(key)) {
            return GITHUB;
        }
        if ("spigotmc".equals(key) || "spigot".equals(key) || "review".equals(key)) {
            return SPIGOT;
        }
        return null;
    }
}
