package com.monkey.ktplus.lib;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class LibraryRequest {
    private final String id;
    private final String displayName;
    private final String descriptorResource;
    private final String overrideUrlPropertyPrefix;
    private final @Nullable LibraryTrack trackOverride;
    private final boolean optional;

    public LibraryRequest(
            String id, String displayName, String descriptorResource, String overrideUrlPropertyPrefix) {
        this(id, displayName, descriptorResource, overrideUrlPropertyPrefix, null, false);
    }

    public LibraryRequest(
            String id,
            String displayName,
            String descriptorResource,
            String overrideUrlPropertyPrefix,
            @Nullable LibraryTrack trackOverride) {
        this(id, displayName, descriptorResource, overrideUrlPropertyPrefix, trackOverride, false);
    }

    public LibraryRequest(
            String id,
            String displayName,
            String descriptorResource,
            String overrideUrlPropertyPrefix,
            @Nullable LibraryTrack trackOverride,
            boolean optional) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.descriptorResource = Objects.requireNonNull(descriptorResource, "descriptorResource");
        this.overrideUrlPropertyPrefix =
                Objects.requireNonNull(overrideUrlPropertyPrefix, "overrideUrlPropertyPrefix");
        this.trackOverride = trackOverride;
        this.optional = optional;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String descriptorResource() {
        return descriptorResource;
    }

    public String overrideUrlPropertyPrefix() {
        return overrideUrlPropertyPrefix;
    }

    public @Nullable LibraryTrack trackOverride() {
        return trackOverride;
    }

    public boolean optional() {
        return optional;
    }
}
