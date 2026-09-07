package com.monkey.ktplus.effects.list.enchantcolumn.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.list.enchantcolumn.animation.util.ParticleArm;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class FinalEffect {
    private FinalEffect() {}

    public static void apply(
            EffectSession session,
            VisualEffectService visuals,
            EffectContext context,
            Location center,
            Player killer,
            PotionEffectType effectType,
            int amplifier,
            int duration) {
        if (center.getWorld() == null) {
            return;
        }
        double radius = 3.0;
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            if (effectType != null) {
                player.addPotionEffect(new PotionEffect(effectType, duration * 20, amplifier - 1));
            }
        }
        triggerExplosion(session, visuals, context, center, killer);
    }

    private static void triggerExplosion(
            EffectSession session, VisualEffectService visuals, EffectContext context, Location center, Player killer) {
        BuiltInDamageService.apply(session, killer, center, context.config().effectDamage("enchantcolumn"));
        visuals.sound("ENTITY_GENERIC_EXPLODE", center, 3.0f, 0.8f);
        int arms = ParticleScale.scale(150);
        int effectDuration = 40;
        Random rand = new Random();
        List<ParticleArm> particleArms = new ArrayList<>();
        for (int i = 0; i < arms; i++) {
            Vector dir = new Vector(
                            (rand.nextDouble() - 0.5) * 6,
                            (rand.nextDouble() - 0.2) * 5,
                            (rand.nextDouble() - 0.5) * 6)
                    .normalize()
                    .multiply(1.2);
            particleArms.add(new ParticleArm(center.clone(), dir));
        }
        session.runLater(0L, () -> {
            if (center.getWorld() == null) {
                return;
            }
            for (Entity entity : center.getWorld().getNearbyEntities(center, 6, 6, 6)) {
                if (entity.equals(killer)) {
                    continue;
                }
                if (!session.allowsWorldMutation(killer, entity.getLocation())) {
                    continue;
                }
                Vector knockback = entity.getLocation()
                        .toVector()
                        .subtract(center.toVector())
                        .normalize()
                        .multiply(2.5)
                        .setY(1.0);
                entity.setVelocity(knockback);
            }
        });
        AtomicInteger tick = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            int current = tick.getAndIncrement();
            if (current > effectDuration) {
                return false;
            }
            for (ParticleArm arm : particleArms) {
                arm.location.add(arm.direction);
                visuals.particle("ENCHANT", arm.location, ParticleScale.scale(8), 0.2, 0.2, 0.2, 0.01, null);
            }
            return true;
        });
    }
}
