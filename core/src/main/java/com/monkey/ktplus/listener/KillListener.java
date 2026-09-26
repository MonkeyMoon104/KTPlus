package com.monkey.ktplus.listener;

import com.monkey.ktplus.bootstrap.PluginBootstrap;
import com.monkey.ktplus.effects.api.KillEffect;
import java.util.Objects;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class KillListener implements Listener {
    private final PluginBootstrap bootstrap;

    public KillListener(PluginBootstrap bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        Entity victim = event.getEntity();
        bootstrap.economy().reward(killer, victim instanceof Player);
        String selected = bootstrap.users().selectedEffect(killer).orElse(null);
        if (selected == null) {
            return;
        }
        KillEffect effect = bootstrap.registry().find(selected).orElse(null);
        if (effect == null) {
            return;
        }
        bootstrap.playPipeline().playGated(killer, victim, victim.getLocation(), effect);
    }
}
