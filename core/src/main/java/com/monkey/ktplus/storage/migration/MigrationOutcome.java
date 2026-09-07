package com.monkey.ktplus.storage.migration;

import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class MigrationOutcome {
    private final boolean success;
    private final String message;
    private final @Nullable Path reportPath;
    private final MigrationContext context;

    public MigrationOutcome(
            boolean success, String message, @Nullable Path reportPath, MigrationContext context) {
        this.success = success;
        this.message = Objects.requireNonNull(message, "message");
        this.reportPath = reportPath;
        this.context = Objects.requireNonNull(context, "context");
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public @Nullable Path reportPath() {
        if (reportPath != null) {
            return reportPath;
        }
        return context.reportPath();
    }

    public MigrationContext context() {
        return context;
    }
}
