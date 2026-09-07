package com.monkey.ktplus.access.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.EffectDefinition;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class EffectSelectionFeedbackTest {
    @Test
    @EnabledIf("stonePresent")
    void deniedPermissionIsSingleMessage() {
        ConfigSnapshot config = snapshot();
        EffectDefinition definition = new EffectDefinition("cloud", "Cloud", Material.STONE, 0, false, 10L);
        List<String> messages =
                EffectSelectionFeedback.messages(config, EffectSelectionService.Outcome.DENIED_PERMISSION, definition);
        assertEquals(1, messages.size());
        assertEquals("no-permission", messages.get(0));
    }

    @Test
    @EnabledIf("stonePresent")
    void purchasedAddsTwoMessages() {
        ConfigSnapshot config = snapshot();
        EffectDefinition definition = new EffectDefinition("cloud", "Cloud", Material.STONE, 100, false, 10L);
        List<String> messages = EffectSelectionFeedback.messages(
                config, EffectSelectionService.Outcome.PURCHASED_AND_SELECTED, definition);
        assertEquals(2, messages.size());
        assertEquals("bought Cloud", messages.get(0));
        assertEquals("selected Cloud", messages.get(1));
    }

    private static ConfigSnapshot snapshot() {
        YamlConfiguration empty = new YamlConfiguration();
        YamlConfiguration messages = new YamlConfiguration();
        messages.set("prefix", "");
        messages.set("no-permission", "no-permission");
        messages.set("not-enough-coins", "need %price%");
        messages.set("effect-purchased", "bought %effect%");
        messages.set("effect-selected", "selected %effect%");
        return new ConfigSnapshot(empty, messages, empty, empty, empty, empty, empty, empty, empty);
    }

    static boolean stonePresent() {
        return Material.matchMaterial("STONE") != null;
    }
}
