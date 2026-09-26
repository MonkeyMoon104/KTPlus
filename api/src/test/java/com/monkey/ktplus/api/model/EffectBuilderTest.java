package com.monkey.ktplus.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.api.spi.EffectRegistration;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class EffectBuilderTest {
    @Test
    void stagedBuilderRequiresDisplayNameAndIcon() {
        Effect effect = Effect.builder("MyFx")
                .displayName("My FX")
                .icon(Material.DIAMOND)
                .category(EffectCategory.EPIC)
                .price(500)
                .heavy()
                .maxDurationTicks(240L)
                .build();

        assertEquals("MyFx", effect.id());
        assertEquals("My FX", effect.displayName());
        assertEquals(EffectCategory.EPIC, effect.category());
        assertEquals(500, effect.price());
        assertEquals(500, effect.premiumPrice());
        assertTrue(effect.heavy());
        assertEquals(240L, effect.maxDurationTicks());
        assertEquals("DIAMOND", effect.iconMaterial());
    }

    @Test
    void toBuilderPreservesValues() {
        Effect original = Effect.builder("cloud")
                .displayName("Cloud")
                .iconMaterial("WHITE_WOOL")
                .price(100)
                .build();
        Effect copy = original.toBuilder().premiumPrice(250).build();
        assertEquals(100, copy.price());
        assertEquals(250, copy.premiumPrice());
        assertEquals("WHITE_WOOL", copy.iconMaterial());
    }

    @Test
    void registrationBuilderCopiesAliases() {
        Effect effect = Effect.builder("x").displayName("X").icon(Material.STONE).free().build();
        EffectRegistration registration = EffectRegistration.builder()
                .effect(effect)
                .executor(context -> {})
                .aliases("alias-a", "alias-b")
                .build();
        assertEquals(2, registration.aliases().size());
        assertEquals("alias-a", registration.aliases().get(0));
    }
}
