package com.monkey.ktplus.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class GuiStructureTest {
    @Test
    void parsesInvUiLikeStructure() {
        YamlConfiguration gui = new YamlConfiguration();
        gui.set(
                "structure",
                java.util.List.of(
                        "#########",
                        "#EEEEEEEU",
                        "#EEEEEEE#",
                        "#EEEEEEED",
                        "#########",
                        "F###C###Q"));
        gui.set("ingredients.#.type", "border");
        gui.set("ingredients.E.type", "effect");
        gui.set("ingredients.U.type", "button");
        gui.set("ingredients.U.button", "scroll-up");
        gui.set("ingredients.D.type", "button");
        gui.set("ingredients.D.button", "scroll-down");
        gui.set("ingredients.F.type", "button");
        gui.set("ingredients.F.button", "filter");
        gui.set("ingredients.C.type", "button");
        gui.set("ingredients.C.button", "clear");
        gui.set("ingredients.Q.type", "button");
        gui.set("ingredients.Q.button", "close");
        gui.set("buttons.preview.slot", 81);
        gui.set("buttons.search.slot", 84);
        gui.set("buttons.favorites.slot", 86);
        gui.set("buttons.balance.slot", 89);

        GuiStructure structure = GuiStructure.parse(gui, 6);
        assertEquals(21, structure.effectSlots().size());
        assertEquals(Integer.valueOf(17), structure.buttonSlots().get("scroll-up"));
        assertEquals(Integer.valueOf(35), structure.buttonSlots().get("scroll-down"));
        assertEquals(Integer.valueOf(45), structure.buttonSlots().get("filter"));
        assertEquals(Integer.valueOf(49), structure.buttonSlots().get("clear"));
        assertEquals(Integer.valueOf(53), structure.buttonSlots().get("close"));
        assertEquals(Integer.valueOf(81), structure.buttonSlots().get("preview"));
        assertEquals(Integer.valueOf(84), structure.buttonSlots().get("search"));
        assertEquals(Integer.valueOf(86), structure.buttonSlots().get("favorites"));
        assertEquals(Integer.valueOf(89), structure.buttonSlots().get("balance"));
        assertTrue(structure.hasExplicitBorders());
        assertTrue(structure.borderSlots().contains(0));
        assertTrue(structure.borderSlots().contains(8));
    }
}
