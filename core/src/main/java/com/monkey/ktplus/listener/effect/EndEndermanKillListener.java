package com.monkey.ktplus.listener.effect;

import com.monkey.ktplus.effects.runtime.EffectEntityRegistry;
import com.monkey.ktplus.hook.HookManager;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class EndEndermanKillListener implements Listener {
    private final JavaPlugin plugin;
    private final HookManager hooks;
    private final EffectEntityRegistry entities;
    private final Set<UUID> applyingOwnerDamage = ConcurrentHashMap.newKeySet();

    public EndEndermanKillListener(JavaPlugin plugin, HookManager hooks, EffectEntityRegistry entities) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.entities = Objects.requireNonNull(entities, "entities");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Enderman enderman)) {
            return;
        }
        UUID ownerId = entities.endermanOwner(enderman.getUniqueId());
        if (ownerId == null) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (applyingOwnerDamage.contains(victim.getUniqueId())) {
            return;
        }
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        if (!hooks.worldGuard().allowsProtectedAction(owner, event.getEntity().getLocation())) {
            event.setCancelled(true);
            return;
        }
        double damage = event.getDamage();
        event.setCancelled(true);
        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> applyOwnerDamage(victim, owner, damage));
    }

    private void applyOwnerDamage(LivingEntity victim, Player owner, double damage) {
        if (!victim.isValid() || victim.isDead() || damage <= 0.0D) {
            return;
        }
        applyingOwnerDamage.add(victim.getUniqueId());
        try {
            victim.damage(damage, owner);
        } finally {
            applyingOwnerDamage.remove(victim.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEndermanDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Enderman enderman) {
            entities.forget(enderman.getUniqueId());
        }
    }
}
