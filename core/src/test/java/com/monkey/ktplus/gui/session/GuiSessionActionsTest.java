package com.monkey.ktplus.gui.session;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.gui.action.GuiAction;
import com.monkey.ktplus.gui.inventory.PlayerInventorySnapshot;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class GuiSessionActionsTest {
    @Test
    void clearTopActionsPreservesCategorySlots() {
        PlayerInventorySnapshot snapshot =
                PlayerInventorySnapshot.fromStored(new ItemStack[36], new ItemStack[4], null);
        GuiSession session = new GuiSession(UUID.randomUUID(), UUID.randomUUID(), EffectCategory.COMMON, 0, snapshot);
        session.put(10, GuiAction.effect("cloud", EffectCategory.COMMON, 0));
        session.put(64, GuiAction.category(EffectCategory.RARE));
        session.clearTopActions(54);
        assertFalse(session.action(10).isPresent());
        assertTrue(session.action(64).isPresent());
    }
}
