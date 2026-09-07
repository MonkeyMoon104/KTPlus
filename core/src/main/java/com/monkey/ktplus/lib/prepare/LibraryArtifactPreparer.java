package com.monkey.ktplus.lib.prepare;

import com.monkey.ktplus.lib.cache.RelocatedLibraryCache;
import com.monkey.ktplus.lib.download.FallbackArtifactFetcher;
import com.monkey.ktplus.lib.model.LibraryArtifact;
import com.monkey.ktplus.lib.model.LibraryDefinition;
import com.monkey.ktplus.lib.relocate.LibraryArtifactRelocator;
import com.monkey.ktplus.lib.relocate.LibraryRelocationRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

public final class LibraryArtifactPreparer {
    private final FallbackArtifactFetcher fetcher;
    private final LibraryArtifactRelocator relocator;
    private final Logger logger;
    private final boolean offlineOnly;

    public LibraryArtifactPreparer(
            FallbackArtifactFetcher fetcher,
            LibraryArtifactRelocator relocator,
            Logger logger,
            boolean offlineOnly) {
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher");
        this.relocator = Objects.requireNonNull(relocator, "relocator");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.offlineOnly = offlineOnly;
    }

    public LibraryPrepareMode prepare(
            LibraryDefinition definition,
            LibraryArtifact artifact,
            Path originalJar,
            Path relocatedJar,
            Path metaFile,
            int index)
            throws IOException {
        return prepareRelocated(definition, artifact, originalJar, relocatedJar, metaFile, index);
    }

    private LibraryPrepareMode prepareRelocated(
            LibraryDefinition definition,
            LibraryArtifact artifact,
            Path originalJar,
            Path relocatedJar,
            Path metaFile,
            int index)
            throws IOException {
        if (RelocatedLibraryCache.isOriginalValid(originalJar, artifact)) {
            logger.info("[Libs] Relocating local " + artifact.fileName() + " (no download)");
            relocateTo(originalJar, relocatedJar, metaFile, artifact.sha256());
            return LibraryPrepareMode.LOCAL;
        }

        if (offlineOnly) {
            throw offlineMissing(definition, artifact);
        }

        Path temporaryOriginal =
                Files.createTempFile(relocatedJar.getParent(), definition.id() + '-' + index + "-orig-", ".tmp");
        try {
            logger.info("[Libs] Downloading " + definition.displayName() + " / " + artifact.fileName() + "...");
            fetcher.download(definition, artifact, temporaryOriginal);
            if (!RelocatedLibraryCache.isOriginalValid(temporaryOriginal, artifact)) {
                throw new IOException(
                        "Downloaded "
                                + definition.id()
                                + " artifact failed integrity validation: "
                                + artifact.fileName());
            }
            RelocatedLibraryCache.atomicMove(temporaryOriginal, originalJar);
            logger.info("[Libs] Relocating " + artifact.fileName() + " -> " + LibraryRelocationRules.ROOT + ".*");
            relocateTo(originalJar, relocatedJar, metaFile, artifact.sha256());
            return LibraryPrepareMode.DOWNLOADED;
        } finally {
            Files.deleteIfExists(temporaryOriginal);
        }
    }

    private void relocateTo(Path originalJar, Path relocatedJar, Path metaFile, String originalSha256)
            throws IOException {
        Path temporaryRelocated =
                relocatedJar.resolveSibling(relocatedJar.getFileName().toString() + "." + System.nanoTime() + ".tmp");
        Files.deleteIfExists(temporaryRelocated);
        try {
            relocator.relocate(originalJar, temporaryRelocated);
            RelocatedLibraryCache.atomicMove(temporaryRelocated, relocatedJar);
            RelocatedLibraryCache.writeMeta(metaFile, originalSha256);
        } finally {
            Files.deleteIfExists(temporaryRelocated);
        }
    }

    private static IOException offlineMissing(LibraryDefinition definition, LibraryArtifact artifact) {
        return new IOException(
                "Offline libs mode: missing local artifact "
                        + artifact.fileName()
                        + " under libs/"
                        + definition.track().folderName()
                        + " (expected SHA-256 "
                        + artifact.sha256()
                        + ")");
    }
}
