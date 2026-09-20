package com.monkey.ktplus.effects.runtime;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import com.monkey.ktplus.util.compat.EntityCompat;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public final class EffectGameplay {
    private static final Set<EntityType> VISUAL_ENTITY_TYPES = EnumSet.of(
            EntityType.ITEM_DISPLAY,
            EntityType.TEXT_DISPLAY,
            EntityType.BLOCK_DISPLAY,
            EntityType.ARMOR_STAND);

    private EffectGameplay() {}

    public static boolean isVisualEntityType(EntityType type) {
        return type != null && VISUAL_ENTITY_TYPES.contains(type);
    }

    public static void damage(
            EffectSession session, LivingEntity target, double amount, @Nullable Player damager) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(target, "target");
        if (amount <= 0.0D || !session.allowsGameplayMutation(damager, target.getLocation())) {
            return;
        }
        if (damager != null) {
            target.damage(amount, damager);
        } else {
            target.damage(amount);
        }
    }

    public static void velocity(EffectSession session, LivingEntity target, Vector velocity, @Nullable Player actor) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(velocity, "velocity");
        if (!session.allowsGameplayMutation(actor, target.getLocation())) {
            return;
        }
        target.setVelocity(velocity);
    }

    public static void potion(
            EffectSession session, LivingEntity target, PotionEffect effect, @Nullable Player actor) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(effect, "effect");
        if (!session.allowsGameplayMutation(actor, target.getLocation())) {
            return;
        }
        target.addPotionEffect(effect);
    }

    public static void fireTicks(EffectSession session, LivingEntity target, int ticks, @Nullable Player actor) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(target, "target");
        if (ticks <= 0 || !session.allowsGameplayMutation(actor, target.getLocation())) {
            return;
        }
        target.setFireTicks(ticks);
    }

    public static void heal(EffectSession session, LivingEntity target, double amount, @Nullable Player actor) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(target, "target");
        if (amount <= 0.0D || !session.allowsGameplayMutation(actor, target.getLocation())) {
            return;
        }
        EntityCompat.trySetHealth(target, target.getHealth() + amount);
    }

    public static void setHealth(EffectSession session, LivingEntity target, double health, @Nullable Player actor) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(target, "target");
        if (!session.allowsGameplayMutation(actor, target.getLocation())) {
            return;
        }
        EntityCompat.trySetHealth(target, health);
    }
}
