package com.monkey.ktplus.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LangServiceTest {
    @Test
    void acceptsTwoLetterUppercasePackNames() {
        assertTrue(LangService.isValidPackFileName("EN.yml"));
        assertTrue(LangService.isValidPackFileName("IT.yml"));
        assertTrue(LangService.isValidPackFileName("ES.yml"));
    }

    @Test
    void rejectsInvalidPackNames() {
        assertFalse(LangService.isValidPackFileName("en.yml"));
        assertFalse(LangService.isValidPackFileName("en_US.yml"));
        assertFalse(LangService.isValidPackFileName("messages.yml"));
        assertFalse(LangService.isValidPackFileName("EN.yaml"));
        assertFalse(LangService.isValidPackFileName("ENG.yml"));
    }

    @Test
    void flattensNestedYamlAndLists() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("prefix", "&7");
        yaml.set("gui.categories.common.display-name", "&fCommon");
        yaml.set("gui.texts.effect-item.lore", java.util.List.of("{description}", "{category}"));
        Map<String, String> flat = LangService.flattenYaml(yaml);
        assertEquals("&7", flat.get("prefix"));
        assertEquals("&fCommon", flat.get("gui.categories.common.display-name"));
        assertEquals("{description}", flat.get("gui.texts.effect-item.lore.0"));
        assertEquals("{category}", flat.get("gui.texts.effect-item.lore.1"));
    }
}
