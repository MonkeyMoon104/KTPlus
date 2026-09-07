package com.monkey.ktplus.listener.effect;

import com.monkey.ktplus.effects.list.sniper.SniperKillEffect;
import java.util.Objects;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class EffectSniperArrowListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Objects.requireNonNull(event, "event");
        Entity damager = event.getDamager();
        if (!(damager instanceof AbstractArrow arrow)) {
            return;
        }
        if (!arrow.getScoreboardTags().contains(SniperKillEffect.TAG)) {
            return;
        }
        ProjectileSource shooter = arrow.getShooter();
        if (!(shooter instanceof Entity owner)) {
            return;
        }
        if (!event.getEntity().getUniqueId().equals(owner.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        event.setDamage(0.0);
    }
}
