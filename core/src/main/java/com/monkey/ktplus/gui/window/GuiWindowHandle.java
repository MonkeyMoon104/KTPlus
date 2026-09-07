package com.monkey.ktplus.gui.window;

import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public interface GuiWindowHandle {
    void refreshTop(Player player, Consumer<GuiChestSlotWriter> populate);

    void updateTitle(Player player, String title);

    static GuiWindowHandle vanilla(Player player) {
        Objects.requireNonNull(player, "player");
        return new GuiWindowHandle() {
            @Override
            public void refreshTop(Player viewer, Consumer<GuiChestSlotWriter> populate) {
                populate.accept((slot, item) ->
                        viewer.getOpenInventory().getTopInventory().setItem(slot, item));
            }

            @Override
            public void updateTitle(Player viewer, String title) {
                GuiTitleCompat.updateOpenViewTitle(viewer, title);
            }
        };
    }
}
