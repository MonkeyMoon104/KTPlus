package com.monkey.ktplus.storage.importkt;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class ClassicKtImportRequest {
    private final Path ktFolder;
    private final boolean dryRun;
    private final boolean overwriteBalances;
    private final @Nullable String typedConfirmation;

    private ClassicKtImportRequest(
            Path ktFolder, boolean dryRun, boolean overwriteBalances, @Nullable String typedConfirmation) {
        this.ktFolder = Objects.requireNonNull(ktFolder, "ktFolder");
        this.dryRun = dryRun;
        this.overwriteBalances = overwriteBalances;
        this.typedConfirmation = typedConfirmation;
    }

    public Path ktFolder() {
        return ktFolder;
    }

    public boolean dryRun() {
        return dryRun;
    }

    public boolean overwriteBalances() {
        return overwriteBalances;
    }

    public @Nullable String typedConfirmation() {
        return typedConfirmation;
    }

    public static ClassicKtImportRequest parse(Path ktFolder, String[] args) {
        Objects.requireNonNull(ktFolder, "ktFolder");
        Objects.requireNonNull(args, "args");
        boolean dryRun = false;
        boolean overwriteBalances = false;
        String token = null;
        for (String arg : args) {
            if (arg == null || arg.isBlank()) {
                continue;
            }
            String lower = arg.toLowerCase(Locale.ROOT);
            if ("--dry-run".equals(lower)) {
                dryRun = true;
                continue;
            }
            if ("--overwrite-balances".equals(lower)) {
                overwriteBalances = true;
                continue;
            }
            if (arg.startsWith("--")) {
                throw new IllegalArgumentException("Unknown flag: " + arg);
            }
            token = arg;
        }
        return new ClassicKtImportRequest(ktFolder, dryRun, overwriteBalances, token);
    }

    public static String confirmationTokenFor(Path ktFolder) {
        return ktFolder.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
