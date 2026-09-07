package com.monkey.ktplus.listener;

import com.monkey.ktplus.bootstrap.PluginBootstrap;
import com.monkey.ktplus.cooldown.CooldownService;
import com.monkey.ktplus.effects.api.KillEffect;
import java.time.Duration;
import java.util.Objects;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class KillListener implements Listener {
    private final PluginBootstrap bootstrap;
    private final CooldownService cooldowns;

    public KillListener(PluginBootstrap bootstrap, CooldownService cooldowns) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (!bootstrap.conditions().allowsWorld(killer) || !bootstrap.conditions().allowsGameMode(killer)) {
            return;
        }
        Entity victim = event.getEntity();
        bootstrap.economy().reward(killer, victim instanceof Player);
        if (!cooldowns.ready(killer.getUniqueId(), "effect")) {
            return;
        }
        String selected = bootstrap.users().selectedEffect(killer).orElse(null);
        if (selected == null) {
            return;
        }
        KillEffect effect = bootstrap.registry().find(selected).orElse(null);
        if (effect == null) {
            return;
        }
        if (!bootstrap.conditions().allowsTrigger(killer, effect.definition())) {
            return;
        }
        if (!bootstrap.access().canActivate(killer, effect.definition())) {
            return;
        }
        cooldowns.set(killer.getUniqueId(), "effect", Duration.ofMillis(bootstrap.config().effectCooldownMillis()));
        if (!bootstrap.runtime().start(killer, victim, victim.getLocation(), effect)) {
            cooldowns.clearKey(killer.getUniqueId(), "effect");
            return;
        }
        bootstrap.randomEvents().tryTrigger(killer, victim.getLocation());
    }
}
