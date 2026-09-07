package com.monkey.ktplus.listener.effect;

import com.monkey.ktplus.effects.runtime.CustomProjectileHitPolicy;
import com.monkey.ktplus.effects.runtime.EffectEntityRegistry;
import com.monkey.ktplus.effects.support.entity.OwnerDamageRedirect;
import com.monkey.ktplus.hook.HookManager;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomProjectileHitListener implements Listener {
    private final JavaPlugin plugin;
    private final HookManager hooks;
    private final EffectEntityRegistry entities;

    public CustomProjectileHitListener(JavaPlugin plugin, HookManager hooks, EffectEntityRegistry entities) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.hooks = Objects.requireNonNull(hooks, "hooks");
        this.entities = Objects.requireNonNull(entities, "entities");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Objects.requireNonNull(event, "event");
        Entity damager = event.getDamager();
        if (!(damager instanceof Projectile)) {
            return;
        }
        CustomProjectileHitPolicy policy = entities.projectile(damager.getUniqueId());
        if (policy == null || policy.damage() <= 0.0D) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            event.setCancelled(true);
            return;
        }
        if (OwnerDamageRedirect.isRedirecting(victim.getUniqueId())) {
            return;
        }
        Player owner = Bukkit.getPlayer(policy.ownerId());
        if (!hooks.worldGuard().allowsProtectedAction(owner, victim.getLocation())) {
            event.setCancelled(true);
            cleanupProjectile(damager, policy);
            return;
        }
        entities.registerFireCredit(victim.getUniqueId(), policy.ownerId());
        double damage = policy.damage();
        OwnerDamageRedirect.redirect(plugin, victim, owner, damage, () -> {
            event.setCancelled(true);
            cleanupProjectile(damager, policy);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent event) {
        Objects.requireNonNull(event, "event");
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE
                && cause != EntityDamageEvent.DamageCause.FIRE_TICK
                && cause != EntityDamageEvent.DamageCause.HOT_FLOOR) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (OwnerDamageRedirect.isRedirecting(victim.getUniqueId())) {
            return;
        }
        UUID ownerId = entities.fireCreditOwner(victim.getUniqueId());
        if (ownerId == null) {
            return;
        }
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner == null || !owner.isOnline()) {
            return;
        }
        if (victim.getUniqueId().equals(owner.getUniqueId())) {
            return;
        }
        if (!hooks.worldGuard().allowsProtectedAction(owner, victim.getLocation())) {
            event.setCancelled(true);
            return;
        }
        double damage = event.getFinalDamage();
        if (damage <= 0.0D) {
            damage = event.getDamage();
        }
        OwnerDamageRedirect.redirect(plugin, victim, owner, damage, () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Objects.requireNonNull(event, "event");
        Entity entity = event.getEntity();
        CustomProjectileHitPolicy policy = entities.projectile(entity.getUniqueId());
        if (policy == null || !policy.removeOnHit()) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity living) {
            entities.registerFireCredit(living.getUniqueId(), policy.ownerId());
        }
        entities.forget(entity.getUniqueId());
        entity.remove();
    }

    private void cleanupProjectile(Entity damager, CustomProjectileHitPolicy policy) {
        UUID id = damager.getUniqueId();
        if (policy.removeOnHit()) {
            entities.forget(id);
            if (damager.isValid()) {
                damager.remove();
            }
        }
    }
}
