package com.monkey.ktplus.gui.window;

import com.monkey.ktplus.common.gui.GuiBackend;
import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.KillEffect;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import com.monkey.ktplus.gui.effect.EffectGuiPagePopulator;
import com.monkey.ktplus.gui.layout.GuiLayout;
import com.monkey.ktplus.gui.session.GuiOpenParams;
import com.monkey.ktplus.gui.session.GuiOpenResult;
import com.monkey.ktplus.gui.session.GuiSession;
import com.monkey.ktplus.gui.session.GuiSessionRegistry;

public final class GuiWindowBridge {
    private GuiWindowBridge() {}

    public static GuiOpenResult open(
            GuiBackend backend,
            Player player,
            GuiSession session,
            GuiSessionRegistry sessions,
            ConfigSnapshot config,
            EffectGuiPagePopulator populator,
            List<KillEffect> visibleEffects,
            int totalInCategory,
            int scrollOffset,
            GuiLayout layout,
            BiConsumer<Player, UUID> onClose) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(sessions, "sessions");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(populator, "populator");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(onClose, "onClose");
        sessions.put(session);

        String title = GuiTitleCompat.fusedTitle(config, session.category());

        Consumer<GuiChestSlotWriter> populate = writer -> populator.populateTop(
                session,
                player,
                visibleEffects,
                scrollOffset,
                totalInCategory,
                layout,
                writer);

        GuiOpenParams params = new GuiOpenParams(
                player,
                title,
                config.guiRows(),
                populate,
                closingPlayer -> onClose.accept(closingPlayer, session.id()));

        GuiOpenResult opened = GuiWindowOpener.forBackend(backend).open(params);
        if (!opened.opened()) {
            sessions.remove(player);
        }
        return opened;
    }
}
