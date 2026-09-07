package com.monkey.ktplus.effects.list.alchemy.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.alchemy.animation.util.AlchemyDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class AlchemyAnimation {
    private static final int DEFAULT_BREW_TICKS = 40;
    private static final int DEFAULT_SPLASH_COUNT = 5;
    private static final double DEFAULT_RADIUS = 5.0;
    private static final int SPLASH_INTERVAL = 22;
    private static final int FINALE = 30;

    private static final Color[] TIER_COLORS = {
        Color.fromRGB(120, 220, 90),
        Color.fromRGB(90, 160, 255),
        Color.fromRGB(200, 80, 255),
        Color.fromRGB(255, 70, 70),
        Color.fromRGB(255, 200, 60)
    };

    private AlchemyAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location ground = context.location().clone().add(0.5, 0.05, 0.5);
        World world = ground.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("alchemy");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int brewTicks = perks == null
                ? DEFAULT_BREW_TICKS
                : Math.max(15, perks.getInt("brew-ticks", DEFAULT_BREW_TICKS));
        int splashCount = perks == null
                ? DEFAULT_SPLASH_COUNT
                : Math.max(1, perks.getInt("splash-count", DEFAULT_SPLASH_COUNT));
        double radius = perks == null ? DEFAULT_RADIUS : Math.max(2.0, perks.getDouble("radius", DEFAULT_RADIUS));
        boolean bonusUpgrade = perks == null || perks.getBoolean("bonus-hit-upgrade", true);

        EffectDamageConfig damageCfg = context.config().effectDamage("alchemy");
        double splashDamage = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 3.0;
        if (damageCfg.enabled()) {
            radius = Math.max(radius, damageCfg.radius());
        }

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        ItemDisplay cauldron = AlchemyDisplays.spawnCauldron(ground.clone().add(0, 0.2, 0));
        if (cauldron == null) {
            return;
        }
        session.trackEntity(cauldron);

        AtomicInteger tick = new AtomicInteger();
        AtomicInteger tier = new AtomicInteger(0);
        AtomicInteger splashesDone = new AtomicInteger();
        int total = brewTicks + splashCount * SPLASH_INTERVAL + FINALE;
        float[] yaw = {0.0f};

        PotionEffectType harm = PotionTypes.resolve("INSTANT_DAMAGE", "HARM");
        PotionEffectType slow = PotionTypes.resolve("SLOWNESS", "SLOW");
        PotionEffectType nausea = PotionTypes.resolve("NAUSEA", "CONFUSION");

        session.onCleanup(() -> {
            AlchemyDisplays.remove(cauldron);
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(total + 20L);

        visuals.sound("BLOCK_BREWING_STAND_BREW", ground, 1.0f, 0.9f);
        visuals.sound("BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT", ground, 0.5f, 1.1f);

        double finalRadius = radius;
        double finalDamage = splashDamage;

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                AlchemyDisplays.remove(cauldron);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                AlchemyDisplays.remove(cauldron);
                PerkActionBar.clear(killer);
                return false;
            }

            int t = Math.min(TIER_COLORS.length - 1, tier.get());
            Color brewColor = TIER_COLORS[t];
            yaw[0] += 0.06f;
            float scale = 1.15f + t * 0.12f + (float) Math.sin(current * 0.2) * 0.05f;
            AlchemyDisplays.place(cauldron, ground.clone().add(0, 0.15, 0), yaw[0], scale);

            drawBrewBubbles(visuals, ground, brewColor, current, t);

            if (current < brewTicks) {
                double progress = (current + 1) / (double) brewTicks;
                if (current % 8 == 0) {
                    visuals.sound("BLOCK_BUBBLE_COLUMN_BUBBLE_POP", ground, 0.4f, 0.8f + (float) progress * 0.5f);
                }
                if (killer != null && killer.isOnline() && current % 3 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&a⚗ ALCHEMY &8| &fBREW &a%d%% &8| &dTIER &f%d",
                                    (int) (progress * 100),
                                    t + 1));
                }
            } else {
                int afterBrew = current - brewTicks;
                int splashIndex = afterBrew / SPLASH_INTERVAL;
                int splashLocal = afterBrew % SPLASH_INTERVAL;

                if (splashIndex < splashCount && splashLocal == 0) {
                    splashesDone.incrementAndGet();
                    boolean hit = fireSplash(
                            session,
                            visuals,
                            world,
                            ground,
                            killer,
                            victimId,
                            finalRadius * (1.0 + t * 0.12),
                            finalDamage * (1.0 + t * 0.2),
                            brewColor,
                            harm,
                            slow,
                            nausea,
                            t);
                    visuals.sound("ENTITY_SPLASH_POTION_THROW", ground, 1.0f, 0.85f + t * 0.08f);
                    visuals.sound("ENTITY_WITCH_THROW", ground, 0.7f, 1.1f);
                    if (hit && bonusUpgrade && tier.get() < TIER_COLORS.length - 1) {
                        tier.incrementAndGet();
                        visuals.sound("ENTITY_PLAYER_LEVELUP", ground, 0.7f, 1.4f);
                        visuals.sound("BLOCK_BREWING_STAND_BREW", ground, 1.0f, 1.3f);
                        visuals.particle("TOTEM_OF_UNDYING", ground.clone().add(0, 1.2, 0), 12, 0.3, 0.4, 0.3, 0.1, null);
                    }
                }

                if (killer != null && killer.isOnline() && current % 3 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&a⚗ ALCHEMY &8| &dTIER &f%d &8| &bSPLASH &f%d&8/&f%d",
                                    tier.get() + 1,
                                    Math.min(splashCount, splashesDone.get()),
                                    splashCount));
                }

                if (splashIndex >= splashCount) {
                    
                    visuals.dust(ground.clone().add(0, 1.5, 0), brewColor, 1.5f, 6, 0.4, 0.5, 0.4, 0.0);
                    if (afterBrew == splashCount * SPLASH_INTERVAL) {
                        visuals.sound("ENTITY_GENERIC_EXPLODE", ground, 0.55f, 1.4f);
                        visuals.particle("EXPLOSION", ground.clone().add(0, 1, 0), 1, 0.1, 0.1, 0.1, 0.0, null);
                    }
                }
            }
            return true;
        });
    }

    private static void drawBrewBubbles(
            VisualEffectService visuals, Location ground, Color color, int tick, int tier) {
        Location mouth = ground.clone().add(0, 0.95 + tier * 0.05, 0);
        visuals.dust(mouth, color, 1.2f, 4, 0.25, 0.1, 0.25, 0.0);
        int bubbles = ParticleScale.scale(5 + tier);
        for (int i = 0; i < bubbles; i++) {
            double a = Math.random() * Math.PI * 2.0;
            double r = Math.random() * 0.45;
            Location p = mouth.clone().add(Math.cos(a) * r, Math.random() * (0.6 + tier * 0.15), Math.sin(a) * r);
            visuals.dust(p, color, 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
            if (i % 2 == 0) {
                visuals.particle("BUBBLE_POP", p, 1, 0.0, 0.0, 0.0, 0.0, null);
            }
        }
        if (tick % 3 == 0) {
            visuals.particle("WITCH", mouth, ParticleScale.scale(3), 0.2, 0.15, 0.2, 0.01, null);
        }
    }

    private static boolean fireSplash(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location ground,
            Player killer,
            UUID victimId,
            double radius,
            double damage,
            Color color,
            PotionEffectType harm,
            PotionEffectType slow,
            PotionEffectType nausea,
            int tier) {
        Location center = ground.clone().add(0, 0.8, 0);
        int ring = ParticleScale.scale(20 + tier * 4);
        for (int i = 0; i < ring; i++) {
            double a = (Math.PI * 2.0 * i) / ring;
            for (double r = 0.4; r <= radius; r += 0.7) {
                Location p = center.clone().add(Math.cos(a) * r, Math.sin(i + r) * 0.15, Math.sin(a) * r);
                visuals.dust(p, color, 1.15f, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        visuals.particle("SPLASH", center, ParticleScale.scale(18), radius * 0.4, 0.3, radius * 0.4, 0.05, null);
        visuals.particle("ENTITY_EFFECT", center, ParticleScale.scale(10), radius * 0.35, 0.25, radius * 0.35, 1.0, color);

        boolean hit = false;
        Set<UUID> once = new HashSet<>();
        double radiusSq = radius * radius;
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (!once.add(player.getUniqueId())) {
                continue;
            }
            hit = true;
            if (damage > 0.0) {
                if (killer != null) {
                    player.damage(damage, killer);
                } else {
                    player.damage(damage);
                }
            }
            int amp = Math.min(2, tier);
            if (harm != null) {
                
                player.addPotionEffect(new PotionEffect(harm, 1, Math.min(1, amp), false, true, true));
            }
            if (slow != null) {
                player.addPotionEffect(new PotionEffect(slow, 40 + tier * 15, amp, false, true, true));
            }
            if (nausea != null) {
                player.addPotionEffect(new PotionEffect(nausea, 50 + tier * 10, 0, false, true, true));
            }
            Location at = player.getLocation().add(0, 1, 0);
            visuals.dust(at, color, 1.4f, 8, 0.25, 0.25, 0.25, 0.0);
            visuals.sound("ENTITY_SPLASH_POTION_BREAK", at, 0.7f, 1.0f);
        }
        return hit;
    }
}
