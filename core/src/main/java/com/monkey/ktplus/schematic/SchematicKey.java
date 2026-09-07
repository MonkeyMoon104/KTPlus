package com.monkey.ktplus.schematic;

import java.util.Locale;
import java.util.Objects;

public final class SchematicKey {
    private final SchematicFormat format;
    private final String id;

    public SchematicKey(SchematicFormat format, String id) {
        this.format = Objects.requireNonNull(format, "format");
        this.id = normalizeId(id);
    }

    public static SchematicKey modern(String id) {
        return new SchematicKey(SchematicFormat.MODERN, id);
    }

    public SchematicFormat format() {
        return format;
    }

    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SchematicKey)) {
            return false;
        }
        SchematicKey that = (SchematicKey) other;
        return format == that.format && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, id);
    }

    @Override
    public String toString() {
        return format.name().toLowerCase(Locale.ROOT) + ":" + id;
    }

    private static String normalizeId(String raw) {
        Objects.requireNonNull(raw, "id");
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("id");
        }
        return trimmed;
    }
}
