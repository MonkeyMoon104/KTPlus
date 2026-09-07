package com.monkey.ktplus.util.net;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class ResourcePackSender {
    private ResourcePackSender() {}

    public static void send(
            Player player,
            String url,
            @Nullable String hashHex,
            @Nullable UUID packId,
            @Nullable String prompt,
            boolean required,
            Logger logger) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(logger, "logger");
        String trimmedUrl = url.trim();
        if (trimmedUrl.isEmpty()) {
            return;
        }
        byte[] hash = null;
        if (hashHex != null && !hashHex.trim().isEmpty()) {
            try {
                hash = sha1Bytes(hashHex.trim());
            } catch (IllegalArgumentException error) {
                logger.warning("[ResourcePack] sha1 must be 40 hex SHA-1 characters; skipping send");
                return;
            }
        }
        UUID id = packId != null ? packId : UUID.randomUUID();
        byte[] payload = hash != null ? hash : new byte[0];
        player.addResourcePack(id, trimmedUrl, payload, prompt, required);
    }

    public static void remove(Player player, UUID packId, Logger logger) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packId, "packId");
        Objects.requireNonNull(logger, "logger");
        player.removeResourcePack(packId);
    }

    public static byte[] sha1Bytes(String hex) {
        Objects.requireNonNull(hex, "hex");
        String normalized = hex.toLowerCase(Locale.ROOT);
        if (normalized.length() != 40 || !normalized.chars().allMatch(ResourcePackSender::isHex)) {
            throw new IllegalArgumentException("sha1");
        }
        byte[] bytes = new byte[20];
        for (int index = 0; index < 20; index++) {
            bytes[index] = (byte) Integer.parseInt(normalized.substring(index * 2, index * 2 + 2), 16);
        }
        return bytes;
    }

    private static boolean isHex(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9') || (codePoint >= 'a' && codePoint <= 'f');
    }
}
