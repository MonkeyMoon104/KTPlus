package com.monkey.ktplus.storage.migration.snapshot;

import java.nio.file.Path;
import java.util.Objects;

public final class SnapshotResult {
    private final Path directory;
    private final SnapshotManifest manifest;

    public SnapshotResult(Path directory, SnapshotManifest manifest) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.manifest = Objects.requireNonNull(manifest, "manifest");
    }

    public Path directory() {
        return directory;
    }

    public SnapshotManifest manifest() {
        return manifest;
    }
}
