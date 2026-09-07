package com.monkey.ktplus.lib.download;

import com.monkey.ktplus.lib.cache.RelocatedLibraryCache;
import com.monkey.ktplus.lib.model.LibraryArtifact;
import com.monkey.ktplus.lib.model.LibraryDefinition;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import com.monkey.ktplus.lib.LibraryLoader;

public final class FallbackArtifactFetcher {
    private final LibraryDownloader downloader;
    private final Logger logger;

    public FallbackArtifactFetcher(LibraryDownloader downloader, Logger logger) {
        this.downloader = Objects.requireNonNull(downloader, "downloader");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void download(LibraryDefinition definition, LibraryArtifact artifact, Path destination) throws IOException {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(destination, "destination");
        IOException lastFailure = null;
        List<URI> uris = artifact.downloadUris();
        for (int i = 0; i < uris.size(); i++) {
            URI uri = uris.get(i);
            try {
                Files.deleteIfExists(destination);
                downloader.download(uri, destination, "KTPlus-LibraryLoader/" + definition.id());
                if (RelocatedLibraryCache.isOriginalValid(destination, artifact)) {
                    if (i > 0) {
                        logger.info(
                                "[Libs] "
                                        + artifact.fileName()
                                        + " fetched via fallback repo #"
                                        + (i + 1));
                    }
                    return;
                }
                lastFailure = new IOException(
                        "integrity check failed for " + artifact.fileName() + " from " + uri);
            } catch (IOException error) {
                lastFailure = error;
                logger.warning(
                        "[Libs] download failed for "
                                + artifact.fileName()
                                + " from "
                                + uri
                                + ": "
                                + error.getMessage());
            }
        }
        throw lastFailure != null
                ? new IOException(
                        "Unable to download "
                                + artifact.fileName()
                                + " from any configured repository. "
                                + "Place the original jar under plugins/KTPlus/libs/<track>/ "
                                + "or restore network access.",
                        lastFailure)
                : new IOException("No download URLs for " + artifact.fileName());
    }
}
