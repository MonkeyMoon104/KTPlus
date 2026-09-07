package com.monkey.ktplus.review;

import java.util.Objects;
import java.util.UUID;

final class ReviewSession {
    private final UUID playerId;
    private final ReviewPlatform platform;
    private final String accountName;
    private final String accountKey;
    private final long expiresAtMs;
    private final boolean presentAtStart;
    private boolean lowRatingNotified;

    ReviewSession(
            UUID playerId,
            ReviewPlatform platform,
            String accountName,
            String accountKey,
            long expiresAtMs,
            boolean presentAtStart) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.platform = Objects.requireNonNull(platform, "platform");
        this.accountName = Objects.requireNonNull(accountName, "accountName");
        this.accountKey = Objects.requireNonNull(accountKey, "accountKey");
        this.expiresAtMs = expiresAtMs;
        this.presentAtStart = presentAtStart;
    }

    UUID playerId() {
        return playerId;
    }

    ReviewPlatform platform() {
        return platform;
    }

    String accountName() {
        return accountName;
    }

    String accountKey() {
        return accountKey;
    }

    long expiresAtMs() {
        return expiresAtMs;
    }

    boolean presentAtStart() {
        return presentAtStart;
    }

    boolean expired(long nowMs) {
        return nowMs >= expiresAtMs;
    }

    long remainingSeconds(long nowMs) {
        return Math.max(0L, (expiresAtMs - nowMs + 999L) / 1000L);
    }

    boolean markLowRatingNotified() {
        if (lowRatingNotified) {
            return false;
        }
        lowRatingNotified = true;
        return true;
    }
}
