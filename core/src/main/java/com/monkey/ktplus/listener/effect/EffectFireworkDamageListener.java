package com.monkey.ktplus.listener.effect;

import com.monkey.ktplus.effects.runtime.EffectEntityRegistry;
import com.monkey.ktplus.effects.support.entity.OwnerDamageRedirect;
import com.monkey.ktplus.hook.HookManager;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class EffectFireworkDamageListener implements Listener {
    private final JavaPlugin plugin;
    private final HookManager hooks;
    private final EffectEntityRegistry entities;

    public EffectFireworkDamageListener(JavaPlugin plugin, HookManager hooks, EffectEntityRegistry entities) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.entities = Objects.requireNonNull(entities, "entities");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Objects.requireNonNull(event, "event");
        Entity damager = event.getDamager();
        UUID ownedOwnerId = entities.ownedDamagerOwner(damager.getUniqueId());
        if (ownedOwnerId != null) {
            if (event.getEntity().getUniqueId().equals(ownedOwnerId)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!(damager instanceof Firework)) {
            return;
        }
        UUID ownerId = entities.fireworkOwner(damager.getUniqueId());
        if (ownerId == null) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            event.setCancelled(true);
            return;
        }
        if (OwnerDamageRedirect.isRedirecting(victim.getUniqueId())) {
            return;
        }
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null && victim.getUniqueId().equals(owner.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (!hooks.worldGuard().allowsProtectedAction(owner, victim.getLocation())) {
            event.setCancelled(true);
            return;
        }
        double damage = event.getDamage();
        OwnerDamageRedirect.redirect(plugin, victim, owner, damage, () -> event.setCancelled(true));
    }
}