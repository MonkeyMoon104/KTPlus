package com.monkey.ktplus.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimeLibraryBootstrapTest {
    @Test
    void onlyPacketEventsRemainsOnCustomLoader() {
        List<LibraryRequest> requests = RuntimeLibraryBootstrap.selectRequests();
        assertEquals(1, requests.size());
        assertEquals("packetevents", requests.get(0).id());
        assertTrue(requests.get(0).optional());
    }

    @Test
    void mavenCentralLibsAreNotCustomLoaded() {
        List<LibraryRequest> requests = RuntimeLibraryBootstrap.selectRequests();
        assertFalse(requests.stream().anyMatch(request -> "jackson".equals(request.id())));
        assertFalse(requests.stream().anyMatch(request -> "lamp".equals(request.id())));
        assertFalse(requests.stream().anyMatch(request -> "configurate".equals(request.id())));
        assertFalse(requests.stream().anyMatch(request -> "hikaricp".equals(request.id())));
        assertFalse(requests.stream().anyMatch(request -> "sqlite-jdbc".equals(request.id())));
        assertFalse(requests.stream().anyMatch(request -> "bstats".equals(request.id())));
        assertFalse(requests.stream().anyMatch(request -> "caffeine".equals(request.id())));
        assertFalse(requests.stream().anyMatch(request -> "mysql-connector".equals(request.id())));
        assertFalse(requests.stream().anyMatch(request -> "mariadb-connector".equals(request.id())));
        assertFalse(requests.stream().anyMatch(request -> "postgresql".equals(request.id())));
    }

    @Test
    void guiLibrariesAreNotRuntimeLoaded() {
        List<LibraryRequest> requests = RuntimeLibraryBootstrap.selectRequests();
        assertFalse(requests.stream().anyMatch(request -> request.id().contains("invui")));
        assertFalse(requests.stream().anyMatch(request -> request.id().contains("smart")));
        assertFalse(requests.stream().anyMatch(request -> request.id().contains("if")));
    }
}
