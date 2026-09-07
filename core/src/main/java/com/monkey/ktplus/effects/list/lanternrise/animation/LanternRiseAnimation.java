package com.monkey.ktplus.effects.list.lanternrise.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.lanternrise.animation.util.LanternRiseDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class LanternRiseAnimation {
    private static final int DEFAULT_LANTERN_COUNT = 8;
    private static final int DEFAULT_RISE_TICKS = 35;
    private static final double DEFAULT_FALL_DAMAGE = 2.5;
    private static final double DEFAULT_GLOW_RADIUS = 3.0;
    private static final int DEFAULT_DURATION = 150;
    private static final double FALL_SPEED = 0.38;

    private static final Color WARM = Color.fromRGB(255, 190, 80);
    private static final Color AMBER = Color.fromRGB(255, 140, 40);
    private static final Color GLOW = Color.fromRGB(255, 230, 150);
    private static final Color CORE = Color.fromRGB(255, 210, 90);

    private enum Phase {
        RISE,
        FALL,
        DONE
    }

    private LanternRiseAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.2, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("lanternrise");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int lanternCount = perks == null
                ? DEFAULT_LANTERN_COUNT
                : Math.max(3, Math.min(16, perks.getInt("lantern-count", DEFAULT_LANTERN_COUNT)));
        int riseTicks = perks == null
                ? DEFAULT_RISE_TICKS
                : Math.max(15, perks.getInt("rise-ticks", DEFAULT_RISE_TICKS));
        double fallDamage = perks == null
                ? DEFAULT_FALL_DAMAGE
                : Math.max(0.0, perks.getDouble("fall-damage", DEFAULT_FALL_DAMAGE));
        double glowRadius = perks == null
                ? DEFAULT_GLOW_RADIUS
                : Math.max(1.0, perks.getDouble("glow-radius", DEFAULT_GLOW_RADIUS));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(80, perks.getInt("duration-ticks", DEFAULT_DURATION));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("lanternrise");
        double impactDamage = damageCfg.enabled()
                ? Math.max(fallDamage, damageCfg.value())
                : fallDamage;
        double damageRadius = damageCfg.enabled() && damageCfg.radius() > 0.0 ? damageCfg.radius() : glowRadius;

        List<Player> targets = findNearby(world, origin, killer, victimId, 14.0, session);
        List<Lantern> lanterns = new ArrayList<>(lanternCount);
        List<ItemDisplay> displays = new ArrayList<>(lanternCount);
        Material[] mats = {Material.LANTERN, Material.SOUL_LANTERN, Material.LANTERN, Material.TORCH};

        for (int i = 0; i < lanternCount; i++) {
            double angle = (Math.PI * 2.0 * i) / lanternCount + Math.random() * 0.2;
            double radius = 1.1 + (i % 3) * 0.55;
            Location spawn = origin.clone().add(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius);
            Location aim;
            if (!targets.isEmpty()) {
                Player t = targets.get(i % targets.size());
                aim = t.getLocation().clone().add(
                        (Math.random() - 0.5) * 1.2,
                        0.0,
                        (Math.random() - 0.5) * 1.2);
            } else {
                double a2 = angle + Math.PI * 0.35;
                aim = origin.clone().add(Math.cos(a2) * (2.5 + i % 3), 0.0, Math.sin(a2) * (2.5 + i % 3));
            }
            float scale = 0.55f + (i % 3) * 0.08f;
            ItemDisplay display = LanternRiseDisplays.spawn(spawn, mats[i % mats.length], scale);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            displays.add(display);
            double peakY = 4.2 + (i % 4) * 0.55 + Math.random() * 0.4;
            lanterns.add(new Lantern(display, spawn, aim, scale, peakY, riseTicks + (i % 5) * 2, i));
        }

        if (lanterns.isEmpty()) {
            return;
        }

        Set<UUID> bombed = new HashSet<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};

        session.onCleanup(() -> {
            LanternRiseDisplays.removeAll(displays);
            displays.clear();
            lanterns.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_LANTERN_PLACE", origin, 0.9f, 1.1f);
        visuals.sound("BLOCK_FIRE_AMBIENT", origin, 0.45f, 1.5f);
        visuals.dust(origin, WARM, 1.4f, ParticleScale.scale(12), 0.45, 0.3, 0.45, 0.0);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks || lanterns.stream().allMatch(l -> l.phase == Phase.DONE)) {
                finish(visuals, origin, lanterns, displays);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            int rising = 0;
            int falling = 0;
            for (Lantern lantern : lanterns) {
                if (lantern.phase == Phase.DONE) {
                    continue;
                }
                if (lantern.phase == Phase.RISE) {
                    rising++;
                    updateRise(visuals, lantern, current);
                } else if (lantern.phase == Phase.FALL) {
                    falling++;
                    updateFall(
                            session,
                            visuals,
                            world,
                            killer,
                            victimId,
                            lantern,
                            impactDamage,
                            damageRadius,
                            glowRadius,
                            bombed,
                            current);
                }
            }

            if (current % 7 == 0) {
                visuals.sound("BLOCK_FIRE_AMBIENT", origin, 0.22f, 1.55f);
            }
            if (current % 11 == 0) {
                visuals.sound("BLOCK_NOTE_BLOCK_BELL", origin, 0.18f, 1.4f + (float) (Math.random() * 0.2));
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format("&6🏮 LANTERNRISE &8| &e↑%d &8| &c↓%d", rising, falling));
            }
            return true;
        });
    }

    private static void updateRise(VisualEffectService visuals, Lantern lantern, int tick) {
        lantern.age++;
        double progress = Math.min(1.0, lantern.age / (double) lantern.riseTicks);
        double ease = 1.0 - Math.pow(1.0 - progress, 2.2);
        double bob = Math.sin(tick * 0.18 + lantern.index) * 0.12;
        double drift = Math.sin(tick * 0.09 + lantern.index * 0.7) * 0.15;
        lantern.loc.setY(lantern.startY + (lantern.peakY - lantern.startY) * ease + bob);
        lantern.loc.setX(lantern.startX + drift);
        lantern.loc.setZ(lantern.startZ + Math.cos(tick * 0.09 + lantern.index) * 0.12);

        float sway = (float) Math.sin(tick * 0.14 + lantern.index) * 0.25f;
        float scale = lantern.scale * (0.92f + (float) Math.sin(tick * 0.2 + lantern.index) * 0.08f);
        LanternRiseDisplays.place(lantern.display, lantern.loc, scale, tick * 0.04f, sway, sway * 0.5f);

        if (tick % 2 == lantern.index % 2) {
            visuals.dust(lantern.loc.clone().add(0, -0.2, 0), WARM, 0.9f, 1, 0.0, 0.0, 0.0, 0.0);
            visuals.dust(lantern.loc, GLOW, 0.7f, 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (tick % 4 == lantern.index % 4) {
            visuals.dust(lantern.loc.clone().add(0, 0.15, 0), CORE, 1.1f, 1, 0.0, 0.0, 0.0, 0.0);
        }

        if (progress >= 1.0) {
            lantern.phase = Phase.FALL;
            lantern.age = 0;
            Vector to = lantern.aim.clone().add(0, 0.1, 0).toVector().subtract(lantern.loc.toVector());
            if (to.lengthSquared() < 1.0e-4) {
                to = new Vector(0, -1, 0);
            }
            lantern.fallDir = to.normalize().multiply(FALL_SPEED);
            lantern.fallDir.setY(Math.min(-0.22, lantern.fallDir.getY()));
            visuals.sound("ENTITY_FIREWORK_ROCKET_LAUNCH", lantern.loc, 0.35f, 1.6f);
        }
    }

    private static void updateFall(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Lantern lantern,
            double impactDamage,
            double damageRadius,
            double glowRadius,
            Set<UUID> bombed,
            int tick) {
        lantern.age++;
        lantern.fallDir.setY(lantern.fallDir.getY() - 0.03);
        lantern.loc.add(lantern.fallDir);

        float spin = tick * 0.18f + lantern.index;
        LanternRiseDisplays.place(lantern.display, lantern.loc, lantern.scale * 1.05f, spin, 0.35f, spin * 0.4f);

        if (tick % 2 == 0) {
            visuals.dust(lantern.loc, AMBER, 1.0f, 2, 0.05, 0.05, 0.05, 0.0);
            visuals.dust(lantern.loc.clone().add(0, 0.2, 0), GLOW, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);
        }

        boolean impact = lantern.loc.getY() <= lantern.aim.getY() + 0.35
                || lantern.loc.getBlock().getType().isSolid()
                || lantern.age > 55;
        if (!impact) {
            return;
        }

        Location boom = lantern.loc.clone();
        visuals.sound("ENTITY_GENERIC_EXPLODE", boom, 0.55f, 1.55f);
        visuals.sound("BLOCK_FIRE_EXTINGUISH", boom, 0.4f, 1.3f);
        visuals.dust(boom, WARM, 1.6f, ParticleScale.scale(16), 0.45, 0.35, 0.45, 0.0);
        visuals.dust(boom, AMBER, 1.3f, ParticleScale.scale(10), 0.55, 0.4, 0.55, 0.0);
        visuals.dust(boom, GLOW, 1.1f, ParticleScale.scale(8), 0.35, 0.25, 0.35, 0.0);
        visuals.particle("FLASH", boom, 1, 0.0, 0.0, 0.0, 0.0, null);
        visuals.particle("CLOUD", boom, ParticleScale.scale(5), 0.25, 0.2, 0.25, 0.02, null);

        double radiusSq = damageRadius * damageRadius;
        for (Player player : findNearby(world, boom, killer, victimId, Math.max(damageRadius, glowRadius), session)) {
            if (player.getLocation().distanceSquared(boom) > radiusSq) {
                continue;
            }
            if (killer == null || !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (impactDamage > 0.0 && bombed.add(player.getUniqueId())) {
                player.damage(impactDamage, killer);
                visuals.dust(player.getLocation().clone().add(0, 1, 0), CORE, 1.2f, 5, 0.2, 0.25, 0.2, 0.0);
            }
        }

        LanternRiseDisplays.remove(lantern.display);
        lantern.phase = Phase.DONE;
    }

    private static void finish(
            VisualEffectService visuals, Location origin, List<Lantern> lanterns, List<ItemDisplay> displays) {
        visuals.sound("BLOCK_LANTERN_BREAK", origin, 0.7f, 1.0f);
        visuals.dust(origin, WARM, 1.3f, ParticleScale.scale(10), 0.5, 0.4, 0.5, 0.0);
        for (Lantern lantern : lanterns) {
            LanternRiseDisplays.remove(lantern.display);
        }
        lanterns.clear();
        displays.clear();
    }

    private static List<Player> findNearby(
            World world, Location origin, Player killer, UUID victimId, double radius, EffectSession session) {
        double radiusSq = radius * radius;
        List<Player> found = new ArrayList<>();
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
            found.add(player);
        }
        found.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(origin)));
        return found;
    }

    private static final class Lantern {
        private final ItemDisplay display;
        private final Location loc;
        private final Location aim;
        private final float scale;
        private final double peakY;
        private final int riseTicks;
        private final int index;
        private final double startX;
        private final double startY;
        private final double startZ;
        private Phase phase = Phase.RISE;
        private int age;
        private Vector fallDir = new Vector(0, -0.3, 0);

        private Lantern(
                ItemDisplay display,
                Location spawn,
                Location aim,
                float scale,
                double peakY,
                int riseTicks,
                int index) {
            this.display = display;
            this.loc = spawn.clone();
            this.aim = aim.clone();
            this.scale = scale;
            this.peakY = spawn.getY() + peakY;
            this.riseTicks = riseTicks;
            this.index = index;
            this.startX = spawn.getX();
            this.startY = spawn.getY();
            this.startZ = spawn.getZ();
        }
    }
}
