package com.monkey.ktplus.lib.model;

import com.monkey.ktplus.lib.LibraryTrack;
import com.monkey.ktplus.lib.download.ArtifactDigests;
import com.monkey.ktplus.util.text.TextValues;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class LibraryDescriptorReader {
    private final ClassLoader resourceLoader;

    public LibraryDescriptorReader(ClassLoader resourceLoader) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
    }

    public LibraryDefinition read(
            String id,
            String displayName,
            String descriptorResource,
            String overrideUrlPrefix,
            @Nullable LibraryTrack trackOverride)
            throws IOException {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(descriptorResource, "descriptorResource");
        Objects.requireNonNull(overrideUrlPrefix, "overrideUrlPrefix");
        Properties properties = new Properties();
        try (InputStream stream = resourceLoader.getResourceAsStream(descriptorResource)) {
            if (stream == null) {
                throw new IOException("Embedded library descriptor missing: " + descriptorResource);
            }
            properties.load(stream);
        }

        LibraryTrack track = trackOverride != null
                ? trackOverride
                : LibraryTrack.fromFolderName(required(properties, "track", id));
        String keyClass = required(properties, "key-class", id);
        int jarCount;
        try {
            jarCount = Integer.parseInt(required(properties, "jar-count", id));
        } catch (NumberFormatException error) {
            throw new IOException(id + " jar-count is invalid", error);
        }
        if (jarCount <= 0) {
            throw new IOException(id + " jar-count must be positive");
        }

        List<LibraryArtifact> artifacts = new ArrayList<LibraryArtifact>(jarCount);
        for (int index = 0; index < jarCount; index++) {
            String prefix = "jar." + index + ".";
            String fileName = required(properties, prefix + "file", id);
            List<URI> uris = readDownloadUris(properties, prefix, overrideUrlPrefix, id, index);
            String sha256 = required(properties, prefix + "sha256", id).toLowerCase(Locale.ROOT);
            if (sha256.length() != 64 || !ArtifactDigests.allHexDigits(sha256)) {
                throw new IOException(id + " SHA-256 is invalid for jar " + index);
            }
            long size;
            try {
                size = Long.parseLong(required(properties, prefix + "size", id));
            } catch (NumberFormatException error) {
                throw new IOException(id + " size is invalid for jar " + index, error);
            }
            if (size <= 0L) {
                throw new IOException(id + " size must be positive for jar " + index);
            }
            artifacts.add(new LibraryArtifact(fileName, uris, sha256, size));
        }

        return new LibraryDefinition(
                id, displayName, track, keyClass, descriptorResource, overrideUrlPrefix, artifacts);
    }

    private static List<URI> readDownloadUris(
            Properties properties,
            String prefix,
            String overrideUrlPrefix,
            String id,
            int index)
            throws IOException {
        String override = System.getProperty(overrideUrlPrefix + "." + index + ".url");
        if (TextValues.isBlank(override)) {
            override = System.getProperty(overrideUrlPrefix + ".url");
        }
        if (!TextValues.isBlank(override)) {
            List<URI> single = new ArrayList<URI>(1);
            single.add(parseHttpsUri(override.trim(), id, index, true));
            return single;
        }

        Set<URI> uris = new LinkedHashSet<URI>();
        String urlCountRaw = properties.getProperty(prefix + "url-count");
        if (!TextValues.isBlank(urlCountRaw)) {
            int urlCount;
            try {
                urlCount = Integer.parseInt(urlCountRaw.trim());
            } catch (NumberFormatException error) {
                throw new IOException(id + " url-count is invalid for jar " + index, error);
            }
            if (urlCount <= 0) {
                throw new IOException(id + " url-count must be positive for jar " + index);
            }
            for (int urlIndex = 0; urlIndex < urlCount; urlIndex++) {
                uris.add(parseHttpsUri(required(properties, prefix + "url." + urlIndex, id), id, index, false));
            }
        } else {
            uris.add(parseHttpsUri(required(properties, prefix + "url", id), id, index, false));
            for (int urlIndex = 1; ; urlIndex++) {
                String extra = properties.getProperty(prefix + "url." + urlIndex);
                if (TextValues.isBlank(extra)) {
                    break;
                }
                uris.add(parseHttpsUri(extra.trim(), id, index, false));
            }
        }
        if (uris.isEmpty()) {
            throw new IOException(id + " has no download URLs for jar " + index);
        }
        return new ArrayList<URI>(uris);
    }

    private static URI parseHttpsUri(String raw, String id, int index, boolean allowOverrideSchemes)
            throws IOException {
        URI uri;
        try {
            uri = URI.create(raw);
        } catch (IllegalArgumentException error) {
            throw new IOException(id + " URL is invalid for jar " + index, error);
        }
        if (!allowOverrideSchemes && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException(id + " URL must use HTTPS for jar " + index);
        }
        return uri;
    }

    private static String required(Properties properties, String key, String id) throws IOException {
        String value = properties.getProperty(key);
        if (TextValues.isBlank(value)) {
            throw new IOException(id + " descriptor is missing " + key);
        }
        return value.trim();
    }
}
