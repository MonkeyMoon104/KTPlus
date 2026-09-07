package com.monkey.ktplus.update;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

final class UpdateCheckerTest {
    @Test
    void requireHttpsEndpoint_acceptsValidHttps() {
        org.junit.jupiter.api.Assertions.assertEquals(
                "https://example.com/version.txt",
                UpdateChecker.requireHttpsEndpoint(" https://example.com/version.txt "));
    }

    @Test
    void requireHttpsEndpoint_rejectsNonHttps() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> UpdateChecker.requireHttpsEndpoint(""));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> UpdateChecker.requireHttpsEndpoint("http://example.com/v"));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> UpdateChecker.requireHttpsEndpoint("ftp://example.com/v"));
    }

    @Test
    void spigotLegacyEndpointIsHttps() {
        org.junit.jupiter.api.Assertions.assertEquals(
                UpdateChecker.SPIGOT_LEGACY_UPDATE_URL,
                UpdateChecker.requireHttpsEndpoint(UpdateChecker.SPIGOT_LEGACY_UPDATE_URL));
    }

    @Test
    void olderSpigotResourceVersionIsNotNewer() {
        assertFalse(PluginVersion.isNewer("3.0.3", "4.0.0"));
    }
}
