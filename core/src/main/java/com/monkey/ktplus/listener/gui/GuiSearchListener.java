package com.monkey.ktplus.listener.gui;

import com.monkey.ktplus.gui.EffectGuiService;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Objects;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuiSearchListener implements Listener {
    private final EffectGuiService gui;

    public GuiSearchListener(EffectGuiService gui) {
        this.gui = Objects.requireNonNull(gui, "gui");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!gui.isPendingSearch(player)) {
            return;
        }
        event.setCancelled(true);
        event.viewers().clear();
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GuiSearchListener.class);
        Bukkit.getScheduler().runTask(plugin, () -> gui.consumeSearchInput(player, plain));
    }
}
