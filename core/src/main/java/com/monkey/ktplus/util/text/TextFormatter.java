package com.monkey.ktplus.util.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class TextFormatter {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private TextFormatter() {}

    public static String color(String value) {
        if (value == null) {
            return "";
        }
        return LEGACY.serialize(LEGACY.deserialize(value.replace('&', '§')));
    }

    public static String strip(String value) {
        if (value == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(LEGACY.deserialize(value.replace('&', '§')));
    }

    public static Component component(String value) {
        if (value == null) {
            return Component.empty();
        }
        return LEGACY.deserialize(value.replace('&', '§'));
    }
}
