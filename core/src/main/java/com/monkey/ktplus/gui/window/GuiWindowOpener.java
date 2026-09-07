package com.monkey.ktplus.gui.window;

import java.util.Objects;
import org.bukkit.entity.Player;
import com.monkey.ktplus.gui.backend.invui.InventoryFrameworkWindowOpener;
import com.monkey.ktplus.gui.session.GuiOpenParams;
import com.monkey.ktplus.gui.session.GuiOpenResult;

public interface GuiWindowOpener {
    GuiOpenResult open(GuiOpenParams params);

    static GuiWindowOpener forBackend(com.monkey.ktplus.common.gui.GuiBackend backend) {
        Objects.requireNonNull(backend, "backend");
        if (backend == com.monkey.ktplus.common.gui.GuiBackend.IF_MODERN) {
            return InventoryFrameworkWindowOpener.INSTANCE;
        }
        throw new IllegalArgumentException("unsupported GUI backend: " + backend);
    }
}
