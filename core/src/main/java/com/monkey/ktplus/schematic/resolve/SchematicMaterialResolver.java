package com.monkey.ktplus.schematic.resolve;

import com.monkey.ktplus.util.compat.MaterialResolver;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

public final class SchematicMaterialResolver {
    private static final Map<String, String> SCHEMATIC_ALIASES = buildAliases();

    private final SchematicFallbackPolicy policy;
    private final @Nullable Logger logger;

    public SchematicMaterialResolver(SchematicFallbackPolicy policy) {
        this(policy, null);
    }

    public SchematicMaterialResolver(SchematicFallbackPolicy policy, @Nullable Logger logger) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.logger = logger;
    }

    public ResolvedSchematicBlock resolve(String paletteKey) {
        Objects.requireNonNull(paletteKey, "paletteKey");
        String baseId = SchematicPaletteKeys.baseBlockId(paletteKey);
        Material material = lookupMaterial(baseId);
        if (material != null && !material.isAir()) {
            return ResolvedSchematicBlock.resolved(material);
        }
        return applyFallback(paletteKey, baseId);
    }

    private @Nullable Material lookupMaterial(String baseId) {
        Material direct = MaterialResolver.find(baseId);
        if (direct != null) {
            return direct;
        }
        String alias = SCHEMATIC_ALIASES.get(baseId);
        if (alias != null) {
            return MaterialResolver.find(alias);
        }
        String underscored = baseId.replace('-', '_').toUpperCase(Locale.ROOT);
        return MaterialResolver.find(underscored);
    }

    private ResolvedSchematicBlock applyFallback(String paletteKey, String detail) {
        Material fallback = policy.fallbackMaterial();
        if (fallback != null && !fallback.isAir()) {
            logUnresolved(paletteKey, detail + " -> fallback " + fallback.name());
            return ResolvedSchematicBlock.fallback(fallback, detail);
        }
        if (policy.skipUnknown()) {
            logUnresolved(paletteKey, detail + " -> skipped");
            return ResolvedSchematicBlock.skipped(detail);
        }
        Material stone = MaterialResolver.find("STONE");
        if (stone != null) {
            return ResolvedSchematicBlock.fallback(stone, detail);
        }
        return ResolvedSchematicBlock.skipped(detail);
    }

    private void logUnresolved(String paletteKey, String message) {
        if (logger != null && policy.logUnresolved()) {
            logger.log(Level.FINE, "[Schematic] unresolved " + paletteKey + ": " + message);
        }
    }

    private static Map<String, String> buildAliases() {
        Map<String, String> map = new HashMap<>();
        map.put("grass", "GRASS_BLOCK");
        map.put("grass_block", "GRASS");
        map.put("dirt", "DIRT");
        map.put("sign", "OAK_SIGN");
        map.put("wall_sign", "OAK_WALL_SIGN");
        map.put("wooden_door", "OAK_DOOR");
        map.put("wooden_button", "OAK_BUTTON");
        map.put("wooden_pressure_plate", "OAK_PRESSURE_PLATE");
        map.put("wooden_slab", "OAK_SLAB");
        map.put("double_wooden_slab", "OAK_PLANKS");
        map.put("wood", "OAK_PLANKS");
        map.put("log", "OAK_LOG");
        map.put("log2", "OAK_LOG");
        map.put("leaves", "OAK_LEAVES");
        map.put("leaves2", "OAK_LEAVES");
        map.put("fence", "OAK_FENCE");
        map.put("fence_gate", "OAK_FENCE_GATE");
        map.put("deepslate", "STONE");
        map.put("cobbled_deepslate", "COBBLESTONE");
        map.put("copper_block", "IRON_BLOCK");
        map.put("oxidized_copper", "IRON_BLOCK");
        map.put("weathered_copper", "IRON_BLOCK");
        map.put("exposed_copper", "IRON_BLOCK");
        map.put("cut_copper", "IRON_BLOCK");
        map.put("amethyst_block", "IRON_BLOCK");
        map.put("sculk", "OBSIDIAN");
        map.put("sculk_catalyst", "OBSIDIAN");
        map.put("sculk_shrieker", "OBSIDIAN");
        map.put("sculk_sensor", "OBSIDIAN");
        map.put("tuff", "STONE");
        map.put("calcite", "QUARTZ_BLOCK");
        map.put("smooth_basalt", "OBSIDIAN");
        map.put("nether_brick_fence", "NETHER_BRICK_FENCE");
        map.put("iron_bars", "IRON_BARS");
        return Collections.unmodifiableMap(map);
    }
}
