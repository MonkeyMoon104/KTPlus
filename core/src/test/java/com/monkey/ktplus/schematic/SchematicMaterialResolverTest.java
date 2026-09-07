package com.monkey.ktplus.schematic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.schematic.resolve.ResolvedSchematicBlock;
import com.monkey.ktplus.schematic.resolve.SchematicFallbackPolicy;
import com.monkey.ktplus.schematic.resolve.SchematicMaterialResolver;
import org.junit.jupiter.api.Test;

final class SchematicMaterialResolverTest {
    @Test
    void skipsWhenConfigured() {
        SchematicMaterialResolver resolver = new SchematicMaterialResolver(
                new SchematicFallbackPolicy(null, true, false));
        ResolvedSchematicBlock resolved = resolver.resolve("minecraft:totally_unknown_block_xyz");
        assertTrue(resolved.skipped());
        assertFalse(resolved.isPlaceable());
    }
}
