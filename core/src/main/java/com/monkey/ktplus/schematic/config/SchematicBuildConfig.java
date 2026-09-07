package com.monkey.ktplus.schematic.config;

import com.monkey.ktplus.schematic.resolve.SchematicFallbackPolicy;
import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;

public final class SchematicBuildConfig {
    private final String schematicId;
    private final int blocksPerTick;
    private final long tickInterval;
    private final SchematicFallbackPolicy fallbackPolicy;
    private final long restoreDelayTicks;

    public SchematicBuildConfig(
            String schematicId,
            int blocksPerTick,
            long tickInterval,
            SchematicFallbackPolicy fallbackPolicy,
            long restoreDelayTicks) {
        this.schematicId = Objects.requireNonNull(schematicId, "schematicId");
        this.blocksPerTick = Math.max(1, blocksPerTick);
        this.tickInterval = Math.max(1L, tickInterval);
        this.fallbackPolicy = Objects.requireNonNull(fallbackPolicy, "fallbackPolicy");
        this.restoreDelayTicks = Math.max(1L, restoreDelayTicks);
    }

    public String schematicId() {
        return schematicId;
    }

    public int blocksPerTick() {
        return blocksPerTick;
    }

    public long tickInterval() {
        return tickInterval;
    }

    public SchematicFallbackPolicy fallbackPolicy() {
        return fallbackPolicy;
    }

    public long restoreDelayTicks() {
        return restoreDelayTicks;
    }

    public static SchematicBuildConfig fromEffectSection(
            ConfigurationSection section, SchematicFallbackPolicy globalPolicy, String defaultSchematicId) {
        Objects.requireNonNull(section, "section");
        String schematicId = section.getString("schematic", defaultSchematicId);
        if (schematicId == null || schematicId.trim().isEmpty()) {
            schematicId = defaultSchematicId;
        }
        ConfigurationSection build = section.getConfigurationSection("build");
        int blocksPerTick = build == null ? 6 : build.getInt("blocks-per-tick", 6);
        long tickInterval = build == null ? 1L : build.getLong("tick-interval", 1L);
        long restoreDelay = build == null ? 200L : build.getLong("restore-delay-ticks", 200L);
        Material fallback = resolveMaterial(section.getString("schematic-fallback"));
        boolean skipUnknown = section.getBoolean("schematic-skip-unknown", true);
        SchematicFallbackPolicy effectPolicy =
                new SchematicFallbackPolicy(fallback, skipUnknown, true);
        return new SchematicBuildConfig(
                schematicId.trim().toLowerCase(),
                blocksPerTick,
                tickInterval,
                SchematicFallbackPolicy.merge(globalPolicy, effectPolicy),
                restoreDelay);
    }

    private static @Nullable Material resolveMaterial(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return MaterialResolver.find(raw.trim());
    }
}
