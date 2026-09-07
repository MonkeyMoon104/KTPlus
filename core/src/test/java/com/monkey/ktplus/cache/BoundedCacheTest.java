package com.monkey.ktplus.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoundedCacheTest {
    @Test
    void computeIfAbsentLoadsOnce() {
        BoundedCache<String, Integer> cache = new BoundedCache<>(16, 300);
        assertEquals(1, cache.computeIfAbsent("key", ignored -> 1));
        assertEquals(1, cache.computeIfAbsent("key", ignored -> 2));
    }

    @Test
    void invalidateRemovesEntry() {
        BoundedCache<String, String> cache = new BoundedCache<>(16, 300);
        cache.put("a", "value");
        cache.invalidate("a");
        assertFalse(cache.get("a").isPresent());
    }

    @Test
    void clearRemovesAllEntries() {
        BoundedCache<String, String> cache = new BoundedCache<>(16, 300);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();
        assertFalse(cache.get("a").isPresent());
        assertFalse(cache.get("b").isPresent());
    }

    @Test
    void putClearsWhenMaxSizeReached() {
        BoundedCache<String, String> cache = new BoundedCache<>(16, 300);
        for (int index = 0; index < 16; index++) {
            cache.put("key-" + index, "value-" + index);
        }
        cache.put("overflow", "new");
        assertFalse(cache.get("key-0").isPresent());
        assertEquals("new", cache.get("overflow").orElse(null));
    }
}
