package com.monkey.ktplus.listener.effect;

import com.monkey.ktplus.effects.list.skeleton.animation.SkeletonAnimation;
import java.util.Objects;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public final class EffectSkeletonGuardListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Objects.requireNonNull(event, "event");
        if (!(event.getEntity() instanceof Skeleton skeleton)) {
            return;
        }
        if (!skeleton.getScoreboardTags().contains(SkeletonAnimation.TAG)) {
            return;
        }
        event.setCancelled(true);
        skeleton.setFireTicks(0);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        Objects.requireNonNull(event, "event");
        if (!(event.getEntity() instanceof Skeleton skeleton)) {
            return;
        }
        if (!skeleton.getScoreboardTags().contains(SkeletonAnimation.TAG)) {
            return;
        }
        event.setCancelled(true);
        skeleton.setFireTicks(0);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        Objects.requireNonNull(event, "event");
        if (!(event.getEntity() instanceof Skeleton skeleton)) {
            return;
        }
        if (!skeleton.getScoreboardTags().contains(SkeletonAnimation.TAG)) {
            return;
        }
        event.setCancelled(true);
    }
}
