package com.monkey.ktplus.lib.model;

import com.monkey.ktplus.lib.LibraryTrack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class LibraryDefinition {
    private final String id;
    private final String displayName;
    private final LibraryTrack track;
    private final String keyClass;
    private final String descriptorResource;
    private final String overrideUrlPropertyPrefix;
    private final List<LibraryArtifact> artifacts;

    public LibraryDefinition(
            String id,
            String displayName,
            LibraryTrack track,
            String keyClass,
            String descriptorResource,
            String overrideUrlPropertyPrefix,
            List<LibraryArtifact> artifacts) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.track = Objects.requireNonNull(track, "track");
        this.keyClass = Objects.requireNonNull(keyClass, "keyClass");
        this.descriptorResource = Objects.requireNonNull(descriptorResource, "descriptorResource");
        this.overrideUrlPropertyPrefix =
                Objects.requireNonNull(overrideUrlPropertyPrefix, "overrideUrlPropertyPrefix");
        this.artifacts =
                Collections.unmodifiableList(new ArrayList<LibraryArtifact>(Objects.requireNonNull(artifacts, "artifacts")));
        if (this.artifacts.isEmpty()) {
            throw new IllegalArgumentException(id + " requires at least one artifact");
        }
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public LibraryTrack track() {
        return track;
    }

    public String keyClass() {
        return keyClass;
    }

    public String descriptorResource() {
        return descriptorResource;
    }

    public String overrideUrlPropertyPrefix() {
        return overrideUrlPropertyPrefix;
    }

    public List<LibraryArtifact> artifacts() {
        return artifacts;
    }
}
