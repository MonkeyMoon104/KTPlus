package com.monkey.ktplus.util.net;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public final class HttpRedirectSupport {
    private HttpRedirectSupport() {}

    public static boolean isRedirect(int statusCode) {
        return statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }

    public static URI resolveRedirect(URI current, String location) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(location, "location");
        URI next = current.resolve(location.trim());
        String scheme = next.getScheme();
        if (scheme == null || !"https".equals(scheme.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("redirect must stay on https");
        }
        if (next.getHost() == null || next.getHost().isEmpty()) {
            throw new IllegalArgumentException("redirect host required");
        }
        return next;
    }
}
