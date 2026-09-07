package com.monkey.ktplus.effects.support.entity;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class OwnerDamageRedirect {
    private static final Set<UUID> APPLYING = ConcurrentHashMap.newKeySet();

    private OwnerDamageRedirect() {}

    public static boolean isRedirecting(UUID victimId) {
        return APPLYING.contains(victimId);
    }

    public static void redirect(
            JavaPlugin plugin,
            LivingEntity victim,
            @Nullable Player owner,
            double damage,
            Runnable cancelAndSchedule) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(victim, "victim");
        Objects.requireNonNull(cancelAndSchedule, "cancelAndSchedule");
        if (owner == null || !owner.isOnline() || damage <= 0.0D) {
            return;
        }
        if (APPLYING.contains(victim.getUniqueId())) {
            return;
        }
        cancelAndSchedule.run();
        plugin.getServer().getScheduler().runTask(plugin, () -> apply(victim, owner, damage));
    }

    private static void apply(LivingEntity victim, Player owner, double damage) {
        if (!victim.isValid() || victim.isDead() || !owner.isOnline()) {
            return;
        }
        APPLYING.add(victim.getUniqueId());
        try {
            victim.damage(damage, owner);
        } finally {
            APPLYING.remove(victim.getUniqueId());
        }
    }
}
