package com.monkey.ktplus.schematic.resolve;

import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.Objects;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

public final class SchematicFallbackPolicy {
    private final @Nullable Material fallbackMaterial;
    private final boolean skipUnknown;
    private final boolean logUnresolved;

    public SchematicFallbackPolicy(
            @Nullable Material fallbackMaterial, boolean skipUnknown, boolean logUnresolved) {
        this.fallbackMaterial = fallbackMaterial;
        this.skipUnknown = skipUnknown;
        this.logUnresolved = logUnresolved;
    }

    public static SchematicFallbackPolicy defaults() {
        Material stone = MaterialResolver.find("STONE");
        return new SchematicFallbackPolicy(stone, true, true);
    }

    public @Nullable Material fallbackMaterial() {
        return fallbackMaterial;
    }

    public boolean skipUnknown() {
        return skipUnknown;
    }

    public boolean logUnresolved() {
        return logUnresolved;
    }

    public SchematicFallbackPolicy withFallback(@Nullable Material fallbackMaterial) {
        return new SchematicFallbackPolicy(fallbackMaterial, skipUnknown, logUnresolved);
    }

    public SchematicFallbackPolicy withSkipUnknown(boolean skipUnknown) {
        return new SchematicFallbackPolicy(fallbackMaterial, skipUnknown, logUnresolved);
    }

    public static SchematicFallbackPolicy merge(
            SchematicFallbackPolicy globalPolicy, SchematicFallbackPolicy effectPolicy) {
        Objects.requireNonNull(globalPolicy, "globalPolicy");
        Objects.requireNonNull(effectPolicy, "effectPolicy");
        Material fallback = effectPolicy.fallbackMaterial() != null
                ? effectPolicy.fallbackMaterial()
                : globalPolicy.fallbackMaterial();
        return new SchematicFallbackPolicy(
                fallback,
                effectPolicy.skipUnknown(),
                globalPolicy.logUnresolved() || effectPolicy.logUnresolved());
    }
}
