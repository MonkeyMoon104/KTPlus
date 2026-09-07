package com.monkey.ktplus.bridge;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractVersionBridge implements VersionBridge {
    private final Map<String, String> particles = new HashMap<>();
    private final Map<String, String> sounds = new HashMap<>();
    private final Map<String, String> materials = new HashMap<>();
    private final Map<String, String> entities = new HashMap<>();

    protected void particle(String key, String value) {
        particles.put(normalize(key), Objects.requireNonNull(value, "value"));
    }

    protected void sound(String key, String value) {
        sounds.put(normalize(key), Objects.requireNonNull(value, "value"));
    }

    protected void material(String key, String value) {
        materials.put(normalize(key), Objects.requireNonNull(value, "value"));
    }

    protected void entity(String key, String value) {
        entities.put(normalize(key), Objects.requireNonNull(value, "value"));
    }

    @Override
    public String particle(String key) {
        String normalized = normalize(key);
        return particles.containsKey(normalized) ? particles.get(normalized) : key;
    }

    @Override
    public String sound(String key) {
        String normalized = normalize(key);
        return sounds.containsKey(normalized) ? sounds.get(normalized) : key;
    }

    @Override
    public String material(String key) {
        String normalized = normalize(key);
        return materials.containsKey(normalized) ? materials.get(normalized) : key;
    }

    @Override
    public String entity(String key) {
        String normalized = normalize(key);
        return entities.containsKey(normalized) ? entities.get(normalized) : key;
    }

    private String normalize(String key) {
        return Objects.requireNonNull(key, "key").toUpperCase(Locale.ROOT);
    }
}
