package com.monkey.ktplus.listener;

import com.monkey.ktplus.cooldown.CooldownService;
import com.monkey.ktplus.effects.runtime.EffectRuntime;
import com.monkey.ktplus.effects.runtime.block.TemporaryBlockService;
import com.monkey.ktplus.gui.EffectGuiService;
import com.monkey.ktplus.task.CancellationReason;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public final class LifecycleListener implements Listener {
    private final EffectRuntime runtime;
    private final EffectGuiService gui;
    private final TemporaryBlockService temporaryBlocks;
    private final CooldownService cooldowns;

    public LifecycleListener(
            EffectRuntime runtime,
            EffectGuiService gui,
            TemporaryBlockService temporaryBlocks,
            CooldownService cooldowns) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.gui = Objects.requireNonNull(gui, "gui");
        this.temporaryBlocks = Objects.requireNonNull(temporaryBlocks, "temporaryBlocks");
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        runtime.cancelPlayer(event.getPlayer().getUniqueId(), CancellationReason.PLAYER_QUIT);
        if (gui.sessions().get(event.getPlayer()).isPresent()) {
            gui.finishSession(event.getPlayer(), null);
        } else {
            gui.sessions().remove(event.getPlayer());
        }
        cooldowns.clear(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        runtime.onFinalePlayerChangedWorld(event.getPlayer(), event.getFrom());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        runtime.cancelWorld(event.getWorld().getName(), CancellationReason.WORLD_UNLOAD);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        temporaryBlocks.restoreWorld(event.getWorld());
    }
}
