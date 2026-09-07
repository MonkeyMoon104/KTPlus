package com.monkey.ktplus.lib.cache;

import com.monkey.ktplus.lib.download.ArtifactDigests;
import com.monkey.ktplus.lib.model.LibraryArtifact;
import com.monkey.ktplus.lib.relocate.LibraryRelocationRules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

public final class RelocatedLibraryCache {
    private RelocatedLibraryCache() {}

    public static boolean isOriginalValid(Path jar, LibraryArtifact artifact) throws IOException {
        Objects.requireNonNull(jar, "jar");
        Objects.requireNonNull(artifact, "artifact");
        return Files.isRegularFile(jar)
                && Files.size(jar) == artifact.size()
                && artifact.sha256().equals(ArtifactDigests.sha256(jar));
    }

    public static boolean isRelocatedCacheValid(Path relocatedJar, Path metaFile, LibraryArtifact artifact)
            throws IOException {
        Objects.requireNonNull(relocatedJar, "relocatedJar");
        Objects.requireNonNull(metaFile, "metaFile");
        Objects.requireNonNull(artifact, "artifact");
        if (!Files.isRegularFile(relocatedJar) || !Files.isRegularFile(metaFile)) {
            return false;
        }
        String meta = new String(Files.readAllBytes(metaFile), StandardCharsets.UTF_8);
        return meta.contains("original-sha256=" + artifact.sha256().toLowerCase(Locale.ROOT))
                && meta.contains("rules-version=" + LibraryRelocationRules.RULES_VERSION);
    }

    public static void writeMeta(Path metaFile, String originalSha256) throws IOException {
        Objects.requireNonNull(metaFile, "metaFile");
        Objects.requireNonNull(originalSha256, "originalSha256");
        String content = "original-sha256="
                + originalSha256.toLowerCase(Locale.ROOT)
                + "\nrules-version="
                + LibraryRelocationRules.RULES_VERSION
                + "\n";
        Files.write(metaFile, content.getBytes(StandardCharsets.UTF_8));
    }

    public static Path metaFile(Path relocatedJar) {
        Objects.requireNonNull(relocatedJar, "relocatedJar");
        return relocatedJar.resolveSibling(relocatedJar.getFileName().toString() + ".meta");
    }

    public static String relocatedFileName(String originalFileName) {
        Objects.requireNonNull(originalFileName, "originalFileName");
        if (originalFileName.endsWith(".jar")) {
            return originalFileName.substring(0, originalFileName.length() - 4) + "-relocated.jar";
        }
        return originalFileName + "-relocated.jar";
    }

    public static void atomicMove(Path source, Path target) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
