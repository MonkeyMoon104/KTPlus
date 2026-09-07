package com.monkey.ktplus.effects.list.shockwave.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.util.item.PotionTypes;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ShockwaveAnimation {
    private static final Color RISE_COLOR = Color.fromRGB(56, 189, 255);

    private ShockwaveAnimation() {}

    public static void start(
            EffectSession session, VisualEffectService visuals, EffectContext context, Location center) {
        Player killer = context.killer();
        Location ground = center.clone();
        AtomicInteger ticks = new AtomicInteger();
        Location current = ground.clone();
        session.runTimer(0L, 1L, () -> {
            if (ticks.getAndIncrement() > 20) {
                spawnSphere(session, visuals, context, killer, ground, current.clone().add(0, 0.5, 0));
                return false;
            }
            current.add(0, 0.5, 0);
            visuals.dust(current, RISE_COLOR, 1.5f, 5, 0.05, 0.05, 0.05, 0.01);
            return true;
        });
    }

    private static void spawnSphere(
            EffectSession session,
            VisualEffectService visuals,
            EffectContext context,
            Player killer,
            Location ground,
            Location top) {
        AtomicInteger ticks = new AtomicInteger();
        session.runTimer(0L, 2L, () -> {
            if (ticks.getAndIncrement() > 20) {
                fallDown(session, visuals, context, killer, ground, top);
                return false;
            }
            double radius = 0.5;
            for (double phi = 0; phi < Math.PI; phi += Math.PI / 8) {
                for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 8) {
                    double x = radius * Math.sin(phi) * Math.cos(theta);
                    double y = radius * Math.cos(phi);
                    double z = radius * Math.sin(phi) * Math.sin(theta);
                    Location loc = top.clone().add(x, y, z);
                    visuals.dust(loc, RISE_COLOR, 2.0f, 1, 0, 0, 0, 0);
                }
            }
            return true;
        });
    }

    private static void fallDown(
            EffectSession session,
            VisualEffectService visuals,
            EffectContext context,
            Player killer,
            Location ground,
            Location start) {
        Location current = start.clone();
        session.runTimer(0L, 1L, () -> {
            if (current.getY() <= ground.getY()) {
                playShockwave(session, visuals, context, killer, ground);
                return false;
            }
            current.subtract(0, 0.5, 0);
            visuals.dust(current, RISE_COLOR, 1.7f, 6, 0.1, 0.1, 0.1, 0.02);
            return true;
        });
    }

    private static void playShockwave(
            EffectSession session,
            VisualEffectService visuals,
            EffectContext context,
            Player killer,
            Location center) {
        EffectDamageConfig damageConfig = context.config().effectDamage("shockwave");
        ConfigurationSection section = context.config().effectSection("shockwave");
        ConfigurationSection explosion = section == null ? null : section.getConfigurationSection("effectexplosion");
        boolean effectEnabled = explosion == null || explosion.getBoolean("enabled", true);
        String typeName = explosion == null ? "BLINDNESS" : explosion.getString("type", "BLINDNESS");
        int amplifier = explosion == null ? 1 : explosion.getInt("amplifier", 1);
        int duration = explosion == null ? 3 : explosion.getInt("duration", 3);
        PotionEffectType effectType = PotionTypes.byKey(typeName);
        if (effectType == null) {
            effectType = PotionEffectType.BLINDNESS;
        }
        visuals.sound("ENTITY_GENERIC_EXPLODE", center, 1.5f, 1.2f);
        PotionEffectType finalEffectType = effectType;
        double[] radius = {0};
        session.runTimer(0L, 2L, () -> {
            if (radius[0] > damageConfig.radius()) {
                return false;
            }
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                double x = Math.cos(angle) * radius[0];
                double z = Math.sin(angle) * radius[0];
                Location loc = center.clone().add(x, 0.1, z);
                visuals.dust(loc, Color.WHITE, 2.0f, 2, 0.05, 0.05, 0.05, 0.01);
            }
            double innerRadius = radius[0] * 0.7;
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                double x = Math.cos(angle) * innerRadius;
                double z = Math.sin(angle) * innerRadius;
                Location loc = center.clone().add(x, 0.1, z);
                visuals.dust(loc, RISE_COLOR, 2.0f, 2, 0.05, 0.05, 0.05, 0.01);
            }
            if (effectEnabled && center.getWorld() != null) {
                for (Player player : center.getWorld().getPlayers()) {
                    if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                        continue;
                    }
                    if (player.getLocation().distance(center) <= radius[0]) {
                        player.addPotionEffect(new PotionEffect(finalEffectType, duration * 20, amplifier - 1));
                    }
                }
            }
            if (damageConfig.enabled() && center.getWorld() != null) {
                for (Player player : center.getWorld().getPlayers()) {
                    if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                        continue;
                    }
                    if (player.getLocation().distance(center) <= damageConfig.radius()) {
                        player.damage(damageConfig.value(), killer);
                    }
                }
            }
            radius[0] += 0.5;
            return true;
        });
    }
}
