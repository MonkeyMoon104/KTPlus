package com.monkey.ktplus.listener.effect;

import com.monkey.ktplus.effects.list.headcollector.HeadCollectorService;
import com.monkey.ktplus.user.UserService;
import java.util.Objects;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class HeadCollectorListener implements Listener {
    private final HeadCollectorService collector;
    private final UserService users;

    public HeadCollectorListener(HeadCollectorService collector, UserService users) {
        this.collector = Objects.requireNonNull(collector, "collector");
        this.users = Objects.requireNonNull(users, "users");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onKillerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player killer)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (!users.selectedEffect(killer).map("headcollector"::equalsIgnoreCase).orElse(false)) {
            return;
        }
        collector.tryLaunchHeads(killer, target);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        collector.clear(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        collector.clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        collector.clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        collector.stripPersistedBonuses(event.getPlayer());
    }
}
