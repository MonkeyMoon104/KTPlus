package com.monkey.ktplus.lib;

import com.monkey.ktplus.lib.cache.RelocatedLibraryCache;
import com.monkey.ktplus.lib.classpath.LibraryClasspathInjector;
import com.monkey.ktplus.lib.download.FallbackArtifactFetcher;
import com.monkey.ktplus.lib.download.HttpArtifactDownloader;
import com.monkey.ktplus.lib.download.LibraryDownloader;
import com.monkey.ktplus.lib.model.LibraryArtifact;
import com.monkey.ktplus.lib.model.LibraryDefinition;
import com.monkey.ktplus.lib.model.LibraryDescriptorReader;
import com.monkey.ktplus.lib.relocate.LibraryArtifactRelocator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import com.monkey.ktplus.lib.prepare.LibraryArtifactPreparer;
import com.monkey.ktplus.lib.prepare.LibraryPrepareMode;

public final class LibraryLoader {
    public static final String LIBS_DIRECTORY = "libs";
    public static final String OFFLINE_PROPERTY = "ktplus.libs.offline";

    private final Path pluginDataDirectory;
    private final ClassLoader pluginClassLoader;
    private final Logger logger;
    private final LibraryClasspathInjector injector;
    private final LibraryDescriptorReader descriptorReader;
    private final LibraryArtifactPreparer artifactPreparer;

    public LibraryLoader(Path pluginDataDirectory, ClassLoader pluginClassLoader, Logger logger) {
        this(
                pluginDataDirectory,
                pluginClassLoader,
                logger,
                new HttpArtifactDownloader(),
                new LibraryClasspathInjector(pluginClassLoader),
                new LibraryDescriptorReader(pluginClassLoader),
                new LibraryArtifactRelocator(),
                Boolean.getBoolean(OFFLINE_PROPERTY));
    }

    LibraryLoader(
            Path pluginDataDirectory,
            ClassLoader pluginClassLoader,
            Logger logger,
            LibraryDownloader downloader,
            LibraryClasspathInjector injector,
            LibraryDescriptorReader descriptorReader,
            LibraryArtifactRelocator relocator,
            boolean offlineOnly) {
        this.pluginDataDirectory = Objects.requireNonNull(pluginDataDirectory, "pluginDataDirectory");
        this.pluginClassLoader = Objects.requireNonNull(pluginClassLoader, "pluginClassLoader");
        this.logger = Objects.requireNonNull(logger, "logger");
        FallbackArtifactFetcher fetcher =
                new FallbackArtifactFetcher(Objects.requireNonNull(downloader, "downloader"), this.logger);
        this.injector = Objects.requireNonNull(injector, "injector");
        this.descriptorReader = Objects.requireNonNull(descriptorReader, "descriptorReader");
        this.artifactPreparer =
                new LibraryArtifactPreparer(fetcher, Objects.requireNonNull(relocator, "relocator"), logger, offlineOnly);
    }

    public List<LibraryLoadResult> loadAll(List<LibraryRequest> requests) throws Exception {
        Objects.requireNonNull(requests, "requests");
        List<LibraryLoadResult> results = new ArrayList<LibraryLoadResult>(requests.size());
        for (LibraryRequest request : requests) {
            if (request.optional()) {
                try {
                    results.add(load(request));
                } catch (Exception error) {
                    logger.warning("[Libs] Optional library '"
                            + request.displayName()
                            + "' failed: "
                            + error.getMessage()
                            + " (continuing)");
                }
                continue;
            }
            results.add(load(request));
        }
        return results;
    }

    public LibraryLoadResult load(LibraryRequest request) throws Exception {
        Objects.requireNonNull(request, "request");
        LibraryDefinition definition = descriptorReader.read(
                request.id(),
                request.displayName(),
                request.descriptorResource(),
                request.overrideUrlPropertyPrefix(),
                request.trackOverride());

        if (isKeyClassPresent(definition.keyClass())) {
            logger.info("[Libs] " + definition.displayName() + " -> reuse classpath (" + definition.keyClass() + ")");
            return LibraryLoadResult.reused(definition);
        }

        Path trackDirectory = pluginDataDirectory
                .resolve(LIBS_DIRECTORY)
                .resolve(definition.track().folderName());
        Files.createDirectories(trackDirectory);

        List<Path> jarPaths = new ArrayList<Path>(definition.artifacts().size());
        LibraryPrepareMode mode = LibraryPrepareMode.CACHED;
        for (int index = 0; index < definition.artifacts().size(); index++) {
            LibraryArtifact artifact = definition.artifacts().get(index);
            Path relocatedJar =
                    trackDirectory.resolve(RelocatedLibraryCache.relocatedFileName(artifact.fileName()));
            Path metaFile = RelocatedLibraryCache.metaFile(relocatedJar);
            Path originalJar = trackDirectory.resolve(artifact.fileName());
            if (!RelocatedLibraryCache.isRelocatedCacheValid(relocatedJar, metaFile, artifact)) {
                LibraryPrepareMode artifactMode = artifactPreparer.prepare(
                        definition, artifact, originalJar, relocatedJar, metaFile, index);
                if (artifactMode.ordinal() > mode.ordinal()) {
                    mode = artifactMode;
                }
            }
            jarPaths.add(relocatedJar);
        }

        for (Path jarPath : jarPaths) {
            injector.addJar(jarPath);
        }

        if (!isKeyClassPresent(definition.keyClass())) {
            throw new IllegalStateException(
                    "Library "
                            + definition.id()
                            + " injected but key class still missing: "
                            + definition.keyClass());
        }

        logger.info(
                "[Libs] "
                        + definition.displayName()
                        + " -> "
                        + mode.logLabel()
                        + " under libs/"
                        + definition.track().folderName()
                        + " ("
                        + jarPaths.size()
                        + " jar(s))");
        return mode.toResult(definition);
    }

    private boolean isKeyClassPresent(String keyClass) {
        try {
            Class.forName(keyClass, false, pluginClassLoader);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
