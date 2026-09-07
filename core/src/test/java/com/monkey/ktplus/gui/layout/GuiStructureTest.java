package com.monkey.ktplus.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class GuiStructureTest {
    @Test
    void parsesInvUiLikeStructure() {
        YamlConfiguration gui = new YamlConfiguration();
        gui.set("structure", java.util.List.of(
                "#########",
                "#EEEEEEEU",
                "#EEEEEEE#",
                "#EEEEEEE#",
                "#EEEEEEED",
                "F#C#Q#B#"));
        gui.set("ingredients.#.type", "border");
        gui.set("ingredients.E.type", "effect");
        gui.set("ingredients.U.type", "button");
        gui.set("ingredients.U.button", "scroll-up");
        gui.set("ingredients.D.type", "button");
        gui.set("ingredients.D.button", "scroll-down");
        gui.set("ingredients.F.type", "button");
        gui.set("ingredients.F.button", "filter");
        gui.set("ingredients.C.type", "button");
        gui.set("ingredients.C.button", "close");
        gui.set("ingredients.Q.type", "button");
        gui.set("ingredients.Q.button", "clear");
        gui.set("ingredients.B.type", "button");
        gui.set("ingredients.B.button", "balance");

        GuiStructure structure = GuiStructure.parse(gui, 6);
        assertEquals(28, structure.effectSlots().size());
        assertEquals(Integer.valueOf(17), structure.buttonSlots().get("scroll-up"));
        assertEquals(Integer.valueOf(44), structure.buttonSlots().get("scroll-down"));
        assertEquals(Integer.valueOf(45), structure.buttonSlots().get("filter"));
        assertEquals(Integer.valueOf(47), structure.buttonSlots().get("close"));
        assertEquals(Integer.valueOf(49), structure.buttonSlots().get("clear"));
        assertEquals(Integer.valueOf(51), structure.buttonSlots().get("balance"));
        assertTrue(structure.hasExplicitBorders());
        assertTrue(structure.borderSlots().contains(0));
        assertTrue(structure.borderSlots().contains(8));
    }
}
