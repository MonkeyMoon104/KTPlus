package com.monkey.ktplus.util.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class TextFormatter {
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private TextFormatter() {}

    public static String color(String value) {
        if (value == null) {
            return "";
        }
        return LEGACY_SECTION.serialize(component(value));
    }

    public static String strip(String value) {
        if (value == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(component(value));
    }

    public static Component component(String value) {
        if (value == null || value.isEmpty()) {
            return Component.empty();
        }
        if (looksLikeMiniMessage(value)) {
            try {
                return MINI.deserialize(value);
            } catch (RuntimeException ignored) {
            }
        }
        return LEGACY_AMPERSAND.deserialize(value);
    }

    static boolean looksLikeMiniMessage(String value) {
        int open = value.indexOf('<');
        if (open < 0) {
            return false;
        }
        int close = value.indexOf('>', open + 1);
        if (close < 0) {
            return false;
        }
        String tag = value.substring(open + 1, close).trim();
        if (tag.isEmpty()) {
            return false;
        }
        char first = tag.charAt(0);
        if (first == '/' || first == '#' || first == '!') {
            return true;
        }
        return Character.isLetter(first);
    }
}
