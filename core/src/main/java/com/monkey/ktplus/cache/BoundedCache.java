package com.monkey.ktplus.cache;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public class BoundedCache<K, V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> values = new ConcurrentHashMap<K, CacheEntry<V>>();
    private final int maxSize;
    private final long ttlMillis;

    public BoundedCache(int maxSize, long ttlSeconds) {
        this.maxSize = Math.max(16, maxSize);
        this.ttlMillis = Math.max(1L, ttlSeconds) * 1000L;
    }

    public static <K, V> BoundedCache<K, V> create(int maxSize, long ttlSeconds) {
        BoundedCache<K, V> caffeine = tryCaffeine(maxSize, ttlSeconds);
        return caffeine != null ? caffeine : new BoundedCache<K, V>(maxSize, ttlSeconds);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> @Nullable BoundedCache<K, V> tryCaffeine(int maxSize, long ttlSeconds) {
        try {
            Class<?> caffeineClass = Class.forName("com.github.benmanes.caffeine.cache.Caffeine");
            Object builder = caffeineClass.getMethod("newBuilder").invoke(null);
            builder.getClass().getMethod("maximumSize", long.class).invoke(builder, (long) maxSize);
            builder.getClass()
                    .getMethod("expireAfterWrite", long.class, TimeUnit.class)
                    .invoke(builder, ttlSeconds, TimeUnit.SECONDS);
            Object cache = builder.getClass().getMethod("build").invoke(builder);
            return new CaffeineBackedCache<K, V>(cache, maxSize, ttlSeconds);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    public V computeIfAbsent(K key, Function<K, V> loader) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(loader, "loader");
        prune();
        CacheEntry<V> existing = values.get(key);
        if (existing != null && !existing.expired(ttlMillis)) {
            return existing.value;
        }
        V loaded = loader.apply(key);
        put(key, loaded);
        return loaded;
    }

    public void put(K key, V value) {
        Objects.requireNonNull(key, "key");
        prune();
        if (values.size() >= maxSize) {
            values.clear();
        }
        values.put(key, new CacheEntry<V>(value, System.currentTimeMillis()));
    }

    public Optional<V> get(K key) {
        CacheEntry<V> entry = values.get(key);
        if (entry == null || entry.expired(ttlMillis)) {
            values.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(entry.value);
    }

    public void invalidate(K key) {
        values.remove(key);
    }

    public void clear() {
        values.clear();
    }

    private void prune() {
        long now = System.currentTimeMillis();
        for (java.util.Map.Entry<K, CacheEntry<V>> entry : values.entrySet()) {
            if (entry.getValue().expired(ttlMillis, now)) {
                values.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private static final class CacheEntry<V> {
        private final V value;
        private final long createdAt;

        private CacheEntry(V value, long createdAt) {
            this.value = value;
            this.createdAt = createdAt;
        }

        private boolean expired(long ttlMillis) {
            return expired(ttlMillis, System.currentTimeMillis());
        }

        private boolean expired(long ttlMillis, long now) {
            return now - createdAt > ttlMillis;
        }
    }

    private static final class CaffeineBackedCache<K, V> extends BoundedCache<K, V> {
        private final Object caffeineCache;

        private CaffeineBackedCache(Object caffeineCache, int maxSize, long ttlSeconds) {
            super(maxSize, ttlSeconds);
            this.caffeineCache = caffeineCache;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V computeIfAbsent(K key, Function<K, V> loader) {
            try {
                return (V) caffeineCache
                        .getClass()
                        .getMethod("get", Object.class, Function.class)
                        .invoke(caffeineCache, key, loader);
            } catch (ReflectiveOperationException error) {
                return super.computeIfAbsent(key, loader);
            }
        }

        @Override
        public void put(K key, V value) {
            try {
                caffeineCache.getClass().getMethod("put", Object.class, Object.class).invoke(caffeineCache, key, value);
            } catch (ReflectiveOperationException error) {
                super.put(key, value);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public Optional<V> get(K key) {
            try {
                Object value = caffeineCache.getClass().getMethod("getIfPresent", Object.class).invoke(caffeineCache, key);
                return Optional.ofNullable((V) value);
            } catch (ReflectiveOperationException error) {
                return super.get(key);
            }
        }

        @Override
        public void invalidate(K key) {
            try {
                caffeineCache.getClass().getMethod("invalidate", Object.class).invoke(caffeineCache, key);
            } catch (ReflectiveOperationException error) {
                super.invalidate(key);
            }
        }

        @Override
        public void clear() {
            try {
                caffeineCache.getClass().getMethod("invalidateAll").invoke(caffeineCache);
            } catch (ReflectiveOperationException error) {
                super.clear();
            }
        }
    }
}
