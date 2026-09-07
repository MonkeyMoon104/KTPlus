package com.monkey.ktplus.effects.list.judgment.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.judgment.animation.util.JudgmentDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class JudgmentAnimation {
    private static final int TOTAL_TICKS = 300;
    private static final int DEFAULT_WEIGH = 50;
    private static final int DEFAULT_MAX_TARGETS = 4;
    private static final double DEFAULT_CRUSH = 10.0;
    private static final double DEFAULT_KNOCK = 1.8;

    private static final Color GOLD = Color.fromRGB(255, 210, 70);
    private static final Color GUILTY = Color.fromRGB(180, 30, 30);
    private static final Color PURE = Color.fromRGB(240, 240, 255);

    private static final String[] EFFECT_SOUNDS = {
        "BLOCK_BEACON_AMBIENT",
        "BLOCK_ANVIL_LAND",
        "BLOCK_BELL_USE",
        "ENTITY_LIGHTNING_BOLT_THUNDER",
        "ENTITY_IRON_GOLEM_ATTACK",
        "ENTITY_GENERIC_EXPLODE",
        "BLOCK_ENCHANTMENT_TABLE_USE"
    };

    private JudgmentAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.1, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("judgment");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int weighTicks = perks == null
                ? DEFAULT_WEIGH
                : Math.max(20, perks.getInt("weigh-ticks", DEFAULT_WEIGH));
        int maxTargets = perks == null
                ? DEFAULT_MAX_TARGETS
                : Math.max(1, Math.min(8, perks.getInt("max-targets", DEFAULT_MAX_TARGETS)));
        double crushDamage = perks == null
                ? DEFAULT_CRUSH
                : Math.max(3.0, perks.getDouble("crush-damage", DEFAULT_CRUSH));
        double knockback = perks == null
                ? DEFAULT_KNOCK
                : Math.max(0.5, perks.getDouble("knockback", DEFAULT_KNOCK));

        EffectDamageConfig damageCfg = context.config().effectDamage("judgment");
        double damageValue = damageCfg.enabled() ? Math.max(3.0, damageCfg.value()) : crushDamage;
        double damageRadius = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 5.0;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        BlockDisplay leftPlate = JudgmentDisplays.spawnPlate(origin.clone().add(-3.5, 6, 0), Material.GOLD_BLOCK, 3.2f);
        BlockDisplay rightPlate = JudgmentDisplays.spawnPlate(origin.clone().add(3.5, 6, 0), Material.GOLD_BLOCK, 3.2f);
        BlockDisplay beam = JudgmentDisplays.spawnPlate(origin.clone().add(0, 8.5, 0), Material.RAW_GOLD_BLOCK, 4.5f);
        ItemDisplay pillar = JudgmentDisplays.spawnBeam(origin.clone().add(0, 0.2, 0));
        if (leftPlate != null) {
            session.trackEntity(leftPlate);
        }
        if (rightPlate != null) {
            session.trackEntity(rightPlate);
        }
        if (beam != null) {
            session.trackEntity(beam);
        }
        if (pillar != null) {
            session.trackEntity(pillar);
        }

        List<Accused> accused = pickTargets(world, origin, killer, victimId, maxTargets, 16.0);
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        boolean[] crushed = {false};

        session.onCleanup(() -> {
            stopSounds(origin);
            JudgmentDisplays.removeBlock(leftPlate);
            JudgmentDisplays.removeBlock(rightPlate);
            JudgmentDisplays.removeBlock(beam);
            JudgmentDisplays.removeItem(pillar);
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_BELL_USE", origin, 1.2f, 0.7f);
        visuals.sound("BLOCK_BEACON_AMBIENT", origin, 0.9f, 0.85f);
        session.resetDeadline(TOTAL_TICKS + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= TOTAL_TICKS) {
                cleanup(visuals, origin, leftPlate, rightPlate, beam, pillar, killer);
                finished[0] = true;
                return false;
            }

            double tip = Math.sin(current * 0.07) * 1.2;
            if (current < weighTicks) {
                tip = Math.sin(current * 0.12) * (0.4 + current / (double) weighTicks * 1.4);
            } else {
                tip = 2.2;
            }

            JudgmentDisplays.placePlate(leftPlate, origin.clone().add(-3.5, 6.0 - tip * 0.5, 0), 3.4f, 0.35f, 3.4f, 0.05f);
            JudgmentDisplays.placePlate(rightPlate, origin.clone().add(3.5, 6.0 + tip * 0.5, 0), 3.4f, 0.35f, 3.4f, -0.05f);
            JudgmentDisplays.placePlate(beam, origin.clone().add(0, 8.8, 0), 5.0f, 0.4f, 1.2f, current * 0.01f);
            JudgmentDisplays.placeBeam(pillar, origin.clone().add(0, 0.2, 0), 8.5f, current * 0.02f);

            drawScalesAura(visuals, origin, current, tip);

            if (current < weighTicks) {
                for (Accused a : accused) {
                    Player p = org.bukkit.Bukkit.getPlayer(a.id);
                    if (p == null || !p.isOnline() || p.isDead()) {
                        continue;
                    }
                    p.setVelocity(new Vector(0, Math.min(0.05, p.getVelocity().getY()), 0));
                    visuals.dust(p.getLocation().add(0, 2.2, 0), GOLD, 1.3f, 3, 0.15, 0.1, 0.15, 0.0);
                }
                if (current % 8 == 0) {
                    visuals.sound("BLOCK_ENCHANTMENT_TABLE_USE", origin, 0.5f, 1.1f);
                }
                if (killer != null && killer.isOnline() && current % 3 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&6⚖ JUDGMENT &8| &eWEIGHING &f%d&8/&f%d &8| &7ACCUSED &f%d",
                                    current,
                                    weighTicks,
                                    accused.size()));
                }
                return true;
            }

            if (!crushed[0]) {
                crushed[0] = true;
                crushGuilty(
                        session,
                        visuals,
                        world,
                        origin,
                        killer,
                        accused,
                        leftPlate,
                        damageValue,
                        damageRadius,
                        knockback);
            }

            if (current > weighTicks + 90) {
                cleanup(visuals, origin, leftPlate, rightPlate, beam, pillar, killer);
                finished[0] = true;
                return false;
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(killer, "&6⚖ JUDGMENT &8| &cGUILTY &8| &4CRUSH");
            }
            return true;
        });
    }

    private static List<Accused> pickTargets(
            World world, Location origin, Player killer, UUID victimId, int max, double range) {
        List<Accused> list = new ArrayList<>();
        double rangeSq = range * range;
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
            double d = player.getLocation().distanceSquared(origin);
            if (d > rangeSq) {
                continue;
            }
            list.add(new Accused(player.getUniqueId(), d));
        }
        list.sort(Comparator.comparingDouble(a -> a.distSq));
        if (list.size() > max) {
            return new ArrayList<>(list.subList(0, max));
        }
        return list;
    }

    private static void drawScalesAura(VisualEffectService visuals, Location origin, int tick, double tip) {
        int pts = ParticleScale.scale(16);
        for (int i = 0; i < pts; i++) {
            double a = (Math.PI * 2.0 * i) / pts + tick * 0.03;
            Location p = origin.clone().add(Math.cos(a) * 4.5, 0.2 + Math.abs(tip) * 0.1, Math.sin(a) * 4.5);
            visuals.dust(p, GOLD, 1.2f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        visuals.dust(origin.clone().add(0, 8, 0), PURE, 1.6f, ParticleScale.scale(5), 0.4, 0.4, 0.4, 0.0);
    }

    private static void crushGuilty(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            List<Accused> accused,
            BlockDisplay leftPlate,
            double damage,
            double radius,
            double knockback) {
        visuals.sound("BLOCK_ANVIL_LAND", origin, 1.5f, 0.45f);
        visuals.sound("ENTITY_IRON_GOLEM_ATTACK", origin, 1.2f, 0.5f);
        visuals.sound("ENTITY_LIGHTNING_BOLT_THUNDER", origin, 0.7f, 0.8f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.9f, 0.7f);

        for (Accused a : accused) {
            Player player = org.bukkit.Bukkit.getPlayer(a.id);
            if (player == null || !player.isOnline() || player.isDead()) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            Location plateAt = player.getLocation().clone().add(0, 4.5, 0);
            JudgmentDisplays.placePlate(leftPlate, plateAt, 3.8f, 0.5f, 3.8f, 0.0f);
            visuals.dust(player.getLocation().add(0, 1, 0), GUILTY, 2.0f, 25, 0.6, 0.5, 0.6, 0.0);
            visuals.dust(player.getLocation(), GOLD, 1.7f, 15, 0.4, 0.2, 0.4, 0.0);

            if (killer != null) {
                player.damage(damage, killer);
            } else {
                player.damage(damage);
            }
            Vector down = new Vector(0, -0.2, 0);
            Vector away = player.getLocation().toVector().subtract(origin.toVector());
            away.setY(0);
            if (away.lengthSquared() > 0.01) {
                away.normalize().multiply(knockback * 0.35);
            } else {
                away = new Vector(0, 0, 0);
            }
            player.setVelocity(away.add(down).setY(-0.15));
        }

        double radiusSq = radius * radius;
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            boolean already = false;
            for (Accused a : accused) {
                if (a.id.equals(player.getUniqueId())) {
                    already = true;
                    break;
                }
            }
            if (already) {
                continue;
            }
            if (player.getLocation().distanceSquared(origin) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (killer != null) {
                player.damage(damage * 0.5, killer);
            } else {
                player.damage(damage * 0.5);
            }
        }
    }

    private static void cleanup(
            VisualEffectService visuals,
            Location origin,
            BlockDisplay left,
            BlockDisplay right,
            BlockDisplay beam,
            ItemDisplay pillar,
            Player killer) {
        visuals.sound("BLOCK_BELL_USE", origin, 0.7f, 1.2f);
        JudgmentDisplays.removeBlock(left);
        JudgmentDisplays.removeBlock(right);
        JudgmentDisplays.removeBlock(beam);
        JudgmentDisplays.removeItem(pillar);
        stopSounds(origin);
        PerkActionBar.clear(killer);
    }

    private static void stopSounds(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 72.0 * 72.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : EFFECT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private static final class Accused {
        private final UUID id;
        private final double distSq;

        private Accused(UUID id, double distSq) {
            this.id = id;
            this.distSq = distSq;
        }
    }
}
