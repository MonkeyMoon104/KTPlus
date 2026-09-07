package com.monkey.ktplus.util.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import com.monkey.ktplus.util.compat.MaterialResolver;

class MaterialResolverFindTest {
    @Test
    void findReturnsNullForBlank() {
        assertNull(MaterialResolver.find(null));
        assertNull(MaterialResolver.find(""));
        assertNull(MaterialResolver.find("   "));
    }

    @Test
    @EnabledIf("stonePresent")
    void findResolvesStone() {
        Material stone = MaterialResolver.find("STONE");
        assertNotNull(stone);
        assertEquals("STONE", stone.name());
    }

    static boolean stonePresent() {
        return Material.matchMaterial("STONE") != null;
    }
}
