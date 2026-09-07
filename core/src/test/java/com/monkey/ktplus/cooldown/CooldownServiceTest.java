package com.monkey.ktplus.cooldown;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CooldownServiceTest {
    @Test
    void expiredEntriesArePrunedOnReady() throws InterruptedException {
        CooldownService service = new CooldownService();
        UUID player = UUID.randomUUID();
        service.set(player, "effect", Duration.ofMillis(20L));
        assertFalse(service.ready(player, "effect"));
        Thread.sleep(40L);
        assertTrue(service.ready(player, "effect"));
        assertTrue(service.ready(player, "effect"));
    }

    @Test
    void clearRemovesPlayerEntries() {
        CooldownService service = new CooldownService();
        UUID player = UUID.randomUUID();
        service.set(player, "effect", Duration.ofMinutes(5L));
        service.clear(player);
        assertTrue(service.ready(player, "effect"));
    }
}
