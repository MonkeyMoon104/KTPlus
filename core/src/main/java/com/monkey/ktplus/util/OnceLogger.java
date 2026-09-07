package com.monkey.ktplus.util;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

public final class OnceLogger {
    private final Logger logger;
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    public OnceLogger(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void warnOnce(String key, String message) {
        warnOnce(key, message, null);
    }

    public void warnOnce(String key, String message, @Nullable Throwable cause) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(message, "message");
        if (warned.add(key)) {
            if (cause != null) {
                logger.log(Level.WARNING, message, cause);
            } else {
                logger.warning(message);
            }
        }
    }

    public void clear() {
        warned.clear();
    }
}
