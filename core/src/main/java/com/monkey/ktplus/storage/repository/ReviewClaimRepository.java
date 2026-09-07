package com.monkey.ktplus.storage.repository;

import com.monkey.ktplus.cache.BoundedCache;
import com.monkey.ktplus.storage.DatabaseService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ReviewClaimRepository {
    private final DatabaseService database;
    private final BoundedCache<String, Boolean> claimCache = BoundedCache.create(2048, 300);

    public ReviewClaimRepository(DatabaseService database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public boolean hasClaim(UUID uuid, String platform) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(platform, "platform");
        return claimCache.computeIfAbsent(playerKey(uuid, platform), ignored -> loadPlayerClaim(uuid, platform));
    }

    public boolean isAccountClaimed(String platform, String accountKey) {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(accountKey, "accountKey");
        return claimCache.computeIfAbsent(
                accountCacheKey(platform, accountKey), ignored -> loadAccountClaim(platform, accountKey));
    }

    public boolean claimIfAbsent(UUID uuid, String platform, String accountKey) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(accountKey, "accountKey");
        String normalizedPlatform = normalize(platform);
        String normalizedAccount = normalize(accountKey);
        if (hasClaim(uuid, normalizedPlatform) || isAccountClaimed(normalizedPlatform, normalizedAccount)) {
            return false;
        }
        boolean[] claimed = new boolean[] {false};
        database.transaction(connection -> {
            claimed[0] = tryClaimOn(connection, uuid, normalizedPlatform, normalizedAccount);
        });
        if (claimed[0]) {
            claimCache.put(playerKey(uuid, normalizedPlatform), Boolean.TRUE);
            claimCache.put(accountCacheKey(normalizedPlatform, normalizedAccount), Boolean.TRUE);
            return true;
        }
        claimCache.invalidate(playerKey(uuid, normalizedPlatform));
        claimCache.invalidate(accountCacheKey(normalizedPlatform, normalizedAccount));
        return false;
    }

    private boolean tryClaimOn(Connection connection, UUID uuid, String platform, String accountKey)
            throws SQLException {
        if (existsPlayerOn(connection, uuid, platform) || existsAccountOn(connection, platform, accountKey)) {
            return false;
        }
        String sql = sqlite()
                ? "INSERT OR IGNORE INTO kt_review_claims (uuid, platform, account_key, claimed_at) VALUES (?, ?, ?, ?)"
                : "INSERT INTO kt_review_claims (uuid, platform, account_key, claimed_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            insert.setString(1, uuid.toString());
            insert.setString(2, platform);
            insert.setString(3, accountKey);
            insert.setLong(4, System.currentTimeMillis());
            return insert.executeUpdate() == 1;
        }
    }

    private boolean existsPlayerOn(Connection connection, UUID uuid, String platform) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM kt_review_claims WHERE uuid = ? AND platform = ?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, platform);
            try (java.sql.ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean existsAccountOn(Connection connection, String platform, String accountKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM kt_review_claims WHERE platform = ? AND account_key = ?")) {
            statement.setString(1, platform);
            statement.setString(2, accountKey);
            try (java.sql.ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean loadPlayerClaim(UUID uuid, String platform) {
        Optional<Boolean> found = database.queryOne(
                "SELECT 1 FROM kt_review_claims WHERE uuid = ? AND platform = ?",
                statement -> {
                    statement.setString(1, uuid.toString());
                    statement.setString(2, normalize(platform));
                },
                resultSet -> Boolean.TRUE);
        return found.isPresent();
    }

    private boolean loadAccountClaim(String platform, String accountKey) {
        Optional<Boolean> found = database.queryOne(
                "SELECT 1 FROM kt_review_claims WHERE platform = ? AND account_key = ?",
                statement -> {
                    statement.setString(1, normalize(platform));
                    statement.setString(2, normalize(accountKey));
                },
                resultSet -> Boolean.TRUE);
        return found.isPresent();
    }

    private boolean sqlite() {
        return "sqlite".equalsIgnoreCase(database.dialect());
    }

    private static String playerKey(UUID uuid, String platform) {
        return "p:" + uuid + ":" + normalize(platform);
    }

    private static String accountCacheKey(String platform, String accountKey) {
        return "a:" + normalize(platform) + ":" + normalize(accountKey);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
