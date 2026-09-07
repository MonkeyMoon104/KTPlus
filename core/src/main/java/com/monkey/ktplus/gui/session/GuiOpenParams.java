package com.monkey.ktplus.gui.session;

import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import com.monkey.ktplus.gui.window.GuiChestSlotWriter;

public final class GuiOpenParams {
    private final Player player;
    private final String title;
    private final int rows;
    private final Consumer<GuiChestSlotWriter> populate;
    private final Consumer<Player> onClose;

    public GuiOpenParams(
            Player player,
            String title,
            int rows,
            Consumer<GuiChestSlotWriter> populate,
            Consumer<Player> onClose) {
        this.player = Objects.requireNonNull(player, "player");
        this.title = Objects.requireNonNull(title, "title");
        this.rows = Math.max(1, rows);
        this.populate = Objects.requireNonNull(populate, "populate");
        this.onClose = Objects.requireNonNull(onClose, "onClose");
    }

    public Player player() {
        return player;
    }

    public String title() {
        return title;
    }

    public int rows() {
        return rows;
    }

    public Consumer<GuiChestSlotWriter> populate() {
        return populate;
    }

    public Consumer<Player> onClose() {
        return onClose;
    }
}
