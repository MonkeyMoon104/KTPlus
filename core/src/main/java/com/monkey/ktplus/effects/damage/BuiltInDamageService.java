package com.monkey.ktplus.effects.damage;

import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectSession;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class BuiltInDamageService {
    private BuiltInDamageService() {}

    public static void apply(
            EffectSession session, Player killer, Location center, EffectDamageConfig damage) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(killer, "killer");
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(damage, "damage");
        if (!damage.enabled()) {
            return;
        }
        Runnable action = () -> deal(session, killer, center, damage);
        if (damage.delayTicks() <= 0L) {
            action.run();
            return;
        }
        session.runLater(damage.delayTicks(), action);
    }

    private static void deal(
            EffectSession session, Player killer, Location center, EffectDamageConfig damage) {
        if (center.getWorld() == null) {
            return;
        }
        double radiusSq = damage.radius() * damage.radius();
        for (LivingEntity entity : center.getWorld().getLivingEntities()) {
            if (entity.equals(killer)) {
                continue;
            }
            if (entity.getLocation().distanceSquared(center) > radiusSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, entity.getLocation())) {
                continue;
            }
            entity.damage(damage.value(), killer);
        }
    }
}
