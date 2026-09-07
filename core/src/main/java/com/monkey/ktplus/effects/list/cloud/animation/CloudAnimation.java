package com.monkey.ktplus.effects.list.cloud.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class CloudAnimation {
    private static final int DEFAULT_OPEN_TICKS = 50;
    private static final int DEFAULT_LINK_TICKS = 120;
    private static final double DEFAULT_RADIUS = 12.0;
    private static final double CLOUD_HEIGHT = 6.5;
    private static final double FOLLOW_HEIGHT = 3.1;

    private CloudAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location ground = context.location().clone().add(0.5, 0.0, 0.5);
        World world = ground.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("cloud");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int openTicks = perks == null ? DEFAULT_OPEN_TICKS : perks.getInt("open-ticks", DEFAULT_OPEN_TICKS);
        int linkTicks = perks == null ? DEFAULT_LINK_TICKS : perks.getInt("link-ticks", DEFAULT_LINK_TICKS);
        double radius = perks == null ? DEFAULT_RADIUS : perks.getDouble("radius", DEFAULT_RADIUS);
        int slowAmplifier = 1;
        if (perks != null) {
            ConfigurationSection slow = perks.getConfigurationSection("slow");
            if (slow != null) {
                slowAmplifier = Math.max(0, slow.getInt("amplifier", 1));
            }
        }

        Location centerCloud = ground.clone().add(0.0, CLOUD_HEIGHT, 0.0);
        Player killer = context.killer();
        PotionEffectType slowType = PotionTypes.resolve("SLOWNESS", "SLOW");
        List<UUID> linked = new ArrayList<>();
        List<UUID> previouslyLinked = new ArrayList<>();
        AtomicInteger tick = new AtomicInteger();
        int totalTicks = Math.max(1, openTicks + linkTicks);
        int amp = slowAmplifier;

        session.onCleanup(() -> PerkActionBar.clear(killer));
        visuals.sound("WEATHER_RAIN", centerCloud, 0.55f, 1.1f);

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= totalTicks) {
                BuiltInDamageService.apply(
                        session, killer, ground, context.config().effectDamage("cloud"));
                PerkActionBar.clear(killer);
                return false;
            }

            if (current % 22 == 0) {
                visuals.sound("WEATHER_RAIN", centerCloud, 0.5f, 1.05f + (current % 44 == 0 ? 0.08f : 0.0f));
                visuals.sound("WEATHER_RAIN_ABOVE", centerCloud, 0.28f, 0.95f);
            }

            boolean linking = current >= openTicks;
            double form = Math.min(1.0, (current + 1) / (double) Math.max(1, openTicks));
            spawnCentralCloud(visuals, centerCloud, form, current);
            spawnRain(visuals, centerCloud, 2.8 * form, current);

            if (linking) {
                refreshLinkedPlayers(world, ground, killer, radius, linked);
                for (UUID id : linked) {
                    if (!previouslyLinked.contains(id)) {
                        Player joined = Bukkit.getPlayer(id);
                        Location at = joined != null
                                ? joined.getLocation().clone().add(0.0, FOLLOW_HEIGHT, 0.0)
                                : centerCloud;
                        visuals.sound("BLOCK_NOTE_BLOCK_CHIME", at, 0.7f, 1.35f);
                        visuals.sound("ENTITY_EXPERIENCE_ORB_PICKUP", at, 0.45f, 0.7f);
                    }
                }
                previouslyLinked.clear();
                previouslyLinked.addAll(linked);

                int linkLocal = current - openTicks;
                for (UUID id : linked) {
                    Player target = Bukkit.getPlayer(id);
                    if (target == null || !target.isOnline() || target.isDead()) {
                        continue;
                    }
                    if (target.getWorld() == null || !target.getWorld().equals(world)) {
                        continue;
                    }
                    Location headCloud = target.getLocation().clone().add(0.0, FOLLOW_HEIGHT, 0.0);
                    spawnFollowCloud(visuals, headCloud, current);
                    spawnRain(visuals, headCloud, 1.35, current);
                    spawnWaveLink(visuals, centerCloud, headCloud, linkLocal);
                    applySlow(target, slowType, amp, totalTicks - current + 15);
                }
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&f☁ CLOUD &8| &bLINKED &f%d &8| &7%s",
                                linked.size(),
                                linking ? "RAINING" : "FORMING"));
            }
            return true;
        });
    }

    private static void refreshLinkedPlayers(
            World world, Location center, Player killer, double radius, List<UUID> linked) {
        linked.clear();
        double radiusSq = radius * radius;
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (!player.isValid() || player.isDead()) {
                continue;
            }
            if (player.getLocation().distanceSquared(center) <= radiusSq) {
                linked.add(player.getUniqueId());
            }
        }
    }

    private static void applySlow(Player target, PotionEffectType type, int amplifier, int ticksLeft) {
        if (type == null || ticksLeft <= 0) {
            return;
        }
        target.addPotionEffect(new PotionEffect(type, Math.max(20, ticksLeft), amplifier, true, true, true));
    }

    private static void spawnCentralCloud(
            VisualEffectService visuals, Location center, double form, int tick) {
        int count = ParticleScale.scale(10 + (int) (form * 18));
        double spread = 1.1 + form * 1.6;
        visuals.particle("CLOUD", center, count, spread, 0.45, spread, 0.01, null);
        visuals.particle("WHITE_ASH", center, ParticleScale.scale(6), spread * 0.7, 0.25, spread * 0.7, 0.0, null);
        if (tick % 3 == 0) {
            visuals.particle("SPIT", center, ParticleScale.scale(4), spread * 0.5, 0.2, spread * 0.5, 0.0, null);
        }
    }

    private static void spawnFollowCloud(VisualEffectService visuals, Location at, int tick) {
        visuals.particle("CLOUD", at, ParticleScale.scale(8), 0.55, 0.22, 0.55, 0.01, null);
        if (tick % 2 == 0) {
            visuals.particle("WHITE_ASH", at, ParticleScale.scale(3), 0.35, 0.12, 0.35, 0.0, null);
        }
    }

    private static void spawnRain(VisualEffectService visuals, Location cloudCenter, double radius, int tick) {
        int drops = ParticleScale.scale(8 + (int) (radius * 3));
        for (int i = 0; i < drops; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double dist = Math.random() * radius;
            Location drop = cloudCenter.clone().add(
                    Math.cos(angle) * dist,
                    -0.2 - Math.random() * 0.4,
                    Math.sin(angle) * dist);
            visuals.particle("RAIN", drop, 2, 0.05, 0.35, 0.05, 0.0, null);
            if ((tick + i) % 3 == 0) {
                visuals.particle("DRIPPING_WATER", drop, 1, 0.02, 0.15, 0.02, 0.0, null);
            }
        }
    }

    private static void spawnWaveLink(
            VisualEffectService visuals, Location from, Location to, int linkTick) {
        Vector delta = to.toVector().subtract(from.toVector());
        double length = delta.length();
        if (length < 0.2) {
            return;
        }
        Vector dir = delta.clone().multiply(1.0 / length);
        Vector sideways = new Vector(-dir.getZ(), 0.0, dir.getX());
        if (sideways.lengthSquared() < 1.0E-4) {
            sideways = new Vector(1, 0, 0);
        }
        sideways.normalize();
        Vector up = dir.clone().crossProduct(sideways).normalize();

        int points = Math.max(10, ParticleScale.scale((int) (length * 2.2)));
        double phase = linkTick * 0.35;
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            Location point = from.clone().add(delta.clone().multiply(t));
            double wave = Math.sin(t * Math.PI * 3.0 + phase) * (0.55 + 0.25 * Math.sin(phase * 0.5));
            double bob = Math.cos(t * Math.PI * 2.0 + phase * 0.7) * 0.22;
            point.add(sideways.clone().multiply(wave)).add(up.clone().multiply(bob));
            visuals.particle("CLOUD", point, 1, 0.02, 0.02, 0.02, 0.0, null);
            if (i % 2 == 0) {
                visuals.particle("DRIPPING_WATER", point, 1, 0.0, 0.05, 0.0, 0.0, null);
            }
            if (i % 3 == 0) {
                visuals.particle("END_ROD", point, 1, 0.0, 0.0, 0.0, 0.0, null);
            }
        }
    }
}
