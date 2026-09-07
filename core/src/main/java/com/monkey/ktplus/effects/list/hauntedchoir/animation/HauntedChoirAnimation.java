package com.monkey.ktplus.effects.list.hauntedchoir.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.hauntedchoir.animation.util.HauntedChoirDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.bukkit.util.Vector;

public final class HauntedChoirAnimation {
    private static final int DEFAULT_SINGERS = 6;
    private static final int DEFAULT_PULSE = 12;
    private static final double DEFAULT_DAMAGE = 3.0;
    private static final double DEFAULT_FEAR = 8.0;
    private static final int TOTAL_TICKS = 200;
    private static final String[] SOUNDS = {
        "ENTITY_SKELETON_AMBIENT",
        "ENTITY_WITHER_SKELETON_AMBIENT",
        "ENTITY_GHAST_AMBIENT",
        "ENTITY_VEX_AMBIENT",
        "ENTITY_WARDEN_HEARTBEAT",
        "BLOCK_SCULK_SHRIEKER_SHRIEK"
    };

    private static final Color GHOST = Color.fromRGB(200, 220, 230);
    private static final Color SOUL = Color.fromRGB(70, 180, 200);
    private static final Color FEAR = Color.fromRGB(90, 40, 120);
    private static final Color PULSE = Color.fromRGB(255, 255, 255);

    private HauntedChoirAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.3, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("hauntedchoir");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int singerCount = perks == null
                ? DEFAULT_SINGERS
                : Math.max(4, Math.min(10, perks.getInt("singer-count", DEFAULT_SINGERS)));
        int pulseInterval = perks == null
                ? DEFAULT_PULSE
                : Math.max(6, perks.getInt("pulse-interval", DEFAULT_PULSE));
        double damage = perks == null
                ? DEFAULT_DAMAGE
                : Math.max(0.5, perks.getDouble("damage", DEFAULT_DAMAGE));
        double fearRadius = perks == null
                ? DEFAULT_FEAR
                : Math.max(3.0, perks.getDouble("fear-radius", DEFAULT_FEAR));

        EffectDamageConfig damageCfg = context.config().effectDamage("hauntedchoir");
        if (damageCfg.enabled()) {
            damage = Math.max(damage, damageCfg.value());
            fearRadius = Math.max(fearRadius, damageCfg.radius());
        }

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        PotionEffectType slow = PotionTypes.resolve("SLOWNESS", "SLOW");

        List<Singer> singers = new ArrayList<>(singerCount);
        for (int i = 0; i < singerCount; i++) {
            double a = (Math.PI * 2.0 * i) / singerCount;
            Location spawn = origin.clone().add(Math.cos(a) * 2.2, 0.0, Math.sin(a) * 2.2);
            ItemDisplay skull = HauntedChoirDisplays.spawnSkull(spawn);
            if (skull != null) {
                session.trackEntity(skull);
                singers.add(new Singer(skull, a, i));
            }
        }

        AtomicInteger tick = new AtomicInteger();
        Set<UUID> pulseCooldown = new HashSet<>();
        boolean[] finished = {false};
        int[] pulses = {0};

        session.onCleanup(() -> {
            stopSounds(origin);
            clear(singers);
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(TOTAL_TICKS + 30L);

        visuals.sound("ENTITY_WITHER_SKELETON_AMBIENT", origin, 0.9f, 0.7f);
        visuals.sound("ENTITY_WARDEN_HEARTBEAT", origin, 0.55f, 0.85f);

        double finalDamage = damage;
        double finalFear = fearRadius;
        int finalPulse = pulseInterval;

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                clear(singers);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= TOTAL_TICKS || singers.isEmpty()) {
                finish(visuals, origin, singers, killer);
                finished[0] = true;
                return false;
            }

            double radius = 2.0 + Math.sin(current * 0.05) * 0.35;
            for (Singer singer : singers) {
                singer.angle += 0.07 + singer.index * 0.002;
                double bob = Math.sin(current * 0.15 + singer.index) * 0.35;
                Location at = origin.clone().add(
                        Math.cos(singer.angle) * radius,
                        bob,
                        Math.sin(singer.angle) * radius);
                float yaw = (float) (singer.angle + Math.PI / 2.0);
                float jaw = (float) Math.sin(current * 0.35 + singer.index) * 0.25f;
                HauntedChoirDisplays.place(singer.display, at, 0.8f + jaw * 0.15f, yaw, jaw);
                singer.loc = at;
                if (current % 3 == singer.index % 3) {
                    visuals.dust(at.clone().add(0, 0.2, 0), SOUL, 0.9f, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }

            if (current % 2 == 0) {
                drawChoirRing(visuals, origin, radius + 0.8, current);
            }

            if (current % finalPulse == 0) {
                pulses[0]++;
                emitPulse(session, visuals, world, origin, killer, victimId, finalDamage, finalFear, slow, pulseCooldown, current);
                visuals.sound("ENTITY_GHAST_AMBIENT", origin, 0.55f, 1.4f + (pulses[0] % 3) * 0.08f);
                visuals.sound("ENTITY_VEX_AMBIENT", origin, 0.45f, 0.7f);
                if (pulses[0] % 3 == 0) {
                    visuals.sound("BLOCK_SCULK_SHRIEKER_SHRIEK", origin, 0.35f, 1.5f);
                }
            }

            if (current % 14 == 0) {
                visuals.sound("ENTITY_SKELETON_AMBIENT", origin, 0.4f, 0.55f);
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&f☠ HAUNTEDCHOIR &8| &bPULSE &f%d &8| &7%d",
                                pulses[0],
                                Math.max(0, TOTAL_TICKS - current)));
            }
            return true;
        });
    }

    private static void drawChoirRing(VisualEffectService visuals, Location origin, double radius, int tick) {
        int pts = ParticleScale.scale(12);
        for (int i = 0; i < pts; i++) {
            double a = tick * 0.08 + (Math.PI * 2.0 * i) / pts;
            Location p = origin.clone().add(Math.cos(a) * radius, Math.sin(a * 2 + tick * 0.1) * 0.25, Math.sin(a) * radius);
            visuals.dust(p, i % 2 == 0 ? GHOST : SOUL, 1.05f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        visuals.particle("SOUL", origin, ParticleScale.scale(2), 0.4, 0.3, 0.4, 0.01, null);
        visuals.particle("SCULK_SOUL", origin.clone().add(0, -0.4, 0), ParticleScale.scale(1), 0.5, 0.1, 0.5, 0.0, null);
    }

    private static void emitPulse(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double damage,
            double fearRadius,
            PotionEffectType slow,
            Set<UUID> cooldown,
            int tick) {
        visuals.dust(origin, PULSE, 1.8f, ParticleScale.scale(16), fearRadius * 0.15, 0.35, fearRadius * 0.15, 0.0);
        visuals.dust(origin, FEAR, 1.3f, ParticleScale.scale(10), fearRadius * 0.2, 0.25, fearRadius * 0.2, 0.0);
        int pts = ParticleScale.scale(18);
        for (int i = 0; i < pts; i++) {
            double a = (Math.PI * 2.0 * i) / pts + tick * 0.05;
            Location p = origin.clone().add(Math.cos(a) * fearRadius * 0.85, 0.1, Math.sin(a) * fearRadius * 0.85);
            visuals.dust(p, SOUL, 1.3f, 1, 0.0, 0.0, 0.0, 0.0);
        }

        double radiusSq = fearRadius * fearRadius;
        cooldown.clear();
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
            if (player.getLocation().distanceSquared(origin) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (cooldown.contains(player.getUniqueId())) {
                continue;
            }
            cooldown.add(player.getUniqueId());
            if (killer != null) {
                player.damage(damage, killer);
            } else {
                player.damage(damage);
            }
            if (slow != null) {
                player.addPotionEffect(new PotionEffect(slow, 30, 0, false, true, true));
            }
            Vector away = player.getLocation().toVector().subtract(origin.toVector());
            away.setY(0);
            if (away.lengthSquared() > 0.01) {
                away.normalize().multiply(0.28).setY(0.12);
                player.setVelocity(away);
            }
            visuals.dust(player.getLocation().add(0, 1, 0), FEAR, 1.2f, 4, 0.15, 0.15, 0.15, 0.0);
        }
    }

    private static void finish(VisualEffectService visuals, Location origin, List<Singer> singers, Player killer) {
        visuals.particle("SOUL", origin, ParticleScale.scale(16), 0.8, 0.5, 0.8, 0.02, null);
        visuals.sound("ENTITY_WITHER_SKELETON_DEATH", origin, 0.7f, 0.8f);
        stopSounds(origin);
        clear(singers);
        PerkActionBar.clear(killer);
    }

    private static void clear(List<Singer> singers) {
        for (Singer singer : singers) {
            HauntedChoirDisplays.remove(singer.display);
        }
        singers.clear();
    }

    private static void stopSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 64.0 * 64.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static final class Singer {
        private final ItemDisplay display;
        private final int index;
        private double angle;
        private Location loc;

        private Singer(ItemDisplay display, double angle, int index) {
            this.display = display;
            this.angle = angle;
            this.index = index;
            this.loc = display.getLocation();
        }
    }
}
