package com.monkey.ktplus.schematic;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class SchematicLoadResult {
    private final @Nullable SchematicModel model;
    private final @Nullable String error;

    private SchematicLoadResult(@Nullable SchematicModel model, @Nullable String error) {
        this.model = model;
        this.error = error;
    }

    public static SchematicLoadResult success(SchematicModel model) {
        return new SchematicLoadResult(Objects.requireNonNull(model, "model"), null);
    }

    public static SchematicLoadResult failure(String error) {
        Objects.requireNonNull(error, "error");
        return new SchematicLoadResult(null, error);
    }

    public boolean success() {
        return model != null;
    }

    public @Nullable SchematicModel model() {
        return model;
    }

    public @Nullable String error() {
        return error;
    }
}
