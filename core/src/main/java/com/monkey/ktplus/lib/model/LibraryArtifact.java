package com.monkey.ktplus.lib.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class LibraryArtifact {
    private final String fileName;
    private final List<URI> downloadUris;
    private final String sha256;
    private final long size;

    public LibraryArtifact(String fileName, URI downloadUri, String sha256, long size) {
        this(fileName, Collections.singletonList(downloadUri), sha256, size);
    }

    public LibraryArtifact(String fileName, List<URI> downloadUris, String sha256, long size) {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(downloadUris, "downloadUris");
        if (downloadUris.isEmpty()) {
            throw new IllegalArgumentException("downloadUris must not be empty");
        }
        List<URI> copy = new ArrayList<URI>(downloadUris.size());
        for (URI uri : downloadUris) {
            copy.add(Objects.requireNonNull(uri, "downloadUri"));
        }
        this.downloadUris = Collections.unmodifiableList(copy);
        this.sha256 = Objects.requireNonNull(sha256, "sha256");
        this.size = size;
    }

    public String fileName() {
        return fileName;
    }

    public URI downloadUri() {
        return downloadUris.get(0);
    }

    public List<URI> downloadUris() {
        return downloadUris;
    }

    public String sha256() {
        return sha256;
    }

    public long size() {
        return size;
    }
}
