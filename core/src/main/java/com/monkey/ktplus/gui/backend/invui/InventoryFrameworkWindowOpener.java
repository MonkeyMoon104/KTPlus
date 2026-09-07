package com.monkey.ktplus.gui.backend.invui;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import com.monkey.ktplus.util.text.TextFormatter;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.monkey.ktplus.gui.session.GuiOpenParams;
import com.monkey.ktplus.gui.session.GuiOpenResult;
import com.monkey.ktplus.gui.window.GuiWindowOpener;

public final class InventoryFrameworkWindowOpener implements GuiWindowOpener {
    public static final InventoryFrameworkWindowOpener INSTANCE = new InventoryFrameworkWindowOpener();

    private InventoryFrameworkWindowOpener() {}

    @Override
    public GuiOpenResult open(GuiOpenParams params) {
        Objects.requireNonNull(params, "params");
        try {
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(getClass());
            ChestGui gui = new ChestGui(params.rows(), TextFormatter.color(params.title()), plugin);
            StaticPane borderPane = new StaticPane(9, params.rows());
            borderPane.setPriority(Pane.Priority.LOW);
            StaticPane contentPane = new StaticPane(9, params.rows());
            contentPane.setPriority(Pane.Priority.NORMAL);
            InventoryFrameworkWindowHandle handle =
                    new InventoryFrameworkWindowHandle(gui, borderPane, contentPane, plugin, params.rows());
            gui.addPane(Slot.fromXY(0, 0), borderPane);
            gui.addPane(Slot.fromXY(0, 0), contentPane);
            handle.refreshTop(params.player(), params.populate());
            gui.setOnClose(event -> {
                if (event.getPlayer() instanceof Player closing) {
                    params.onClose().accept(closing);
                }
            });
            gui.show(params.player());
            return GuiOpenResult.success(handle);
        } catch (Throwable failure) {
            return GuiOpenResult.failure(failure);
        }
    }
}
