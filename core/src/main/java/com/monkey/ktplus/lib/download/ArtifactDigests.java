package com.monkey.ktplus.lib.download;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ArtifactDigests {
    private ArtifactDigests() {}

    public static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream stream = Files.newInputStream(file)) {
                byte[] buffer = new byte[16_384];
                int read;
                while ((read = stream.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    public static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xFF;
            hex[index * 2] = Character.forDigit(value >>> 4, 16);
            hex[index * 2 + 1] = Character.forDigit(value & 0x0F, 16);
        }
        return new String(hex);
    }

    public static boolean allHexDigits(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!((character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f')
                    || (character >= 'A' && character <= 'F'))) {
                return false;
            }
        }
        return true;
    }
}
