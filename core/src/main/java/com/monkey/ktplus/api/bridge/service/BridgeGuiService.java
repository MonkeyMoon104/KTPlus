package com.monkey.ktplus.api.bridge.service;

import com.monkey.ktplus.api.service.GuiService;
import com.monkey.ktplus.gui.EffectGuiService;
import java.util.Objects;
import org.bukkit.entity.Player;

public final class BridgeGuiService implements GuiService {
    private final EffectGuiService gui;

    public BridgeGuiService(EffectGuiService gui) {
        this.gui = Objects.requireNonNull(gui, "gui");
    }

    @Override
    public void open(Player player) {
        gui.open(player);
    }

    @Override
    public void close(Player player) {
        Objects.requireNonNull(player, "player");
        gui.finishSession(player, null);
        player.closeInventory();
    }

    @Override
    public boolean isOpen(Player player) {
        return gui.sessions().get(player).isPresent();
    }
}
