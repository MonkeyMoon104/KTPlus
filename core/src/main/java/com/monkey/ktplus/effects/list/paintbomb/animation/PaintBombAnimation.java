package com.monkey.ktplus.effects.list.paintbomb.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.paintbomb.animation.util.PaintBombDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.item.PotionTypes;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class PaintBombAnimation {
    private static final double DEFAULT_BLAST_RADIUS = 5.0;
    private static final int DEFAULT_BLIND_TICKS = 35;
    private static final int DEFAULT_SHARD_COUNT = 20;
    private static final int DEFAULT_DURATION = 120;
    private static final int CHARGE_TICKS = 18;
    private static final int SHARD_LIFE = 55;
    private static final double GRAVITY = 0.032;

    private static final Material[] POWDERS = {
        Material.PINK_CONCRETE_POWDER,
        Material.MAGENTA_CONCRETE_POWDER,
        Material.RED_CONCRETE_POWDER,
        Material.ORANGE_CONCRETE_POWDER,
        Material.YELLOW_CONCRETE_POWDER,
        Material.LIME_CONCRETE_POWDER,
        Material.CYAN_CONCRETE_POWDER,
        Material.LIGHT_BLUE_CONCRETE_POWDER,
        Material.BLUE_CONCRETE_POWDER,
        Material.PURPLE_CONCRETE_POWDER,
        Material.WHITE_CONCRETE_POWDER
    };

    private static final Color[] DUSTS = {
        Color.fromRGB(255, 105, 180),
        Color.fromRGB(255, 50, 150),
        Color.fromRGB(255, 70, 70),
        Color.fromRGB(255, 150, 40),
        Color.fromRGB(255, 220, 60),
        Color.fromRGB(100, 220, 80),
        Color.fromRGB(60, 220, 220),
        Color.fromRGB(80, 170, 255),
        Color.fromRGB(70, 100, 255),
        Color.fromRGB(170, 80, 255),
        Color.fromRGB(245, 245, 245)
    };

    private PaintBombAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("paintbomb");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double blastRadius = perks == null
                ? DEFAULT_BLAST_RADIUS
                : Math.max(2.0, perks.getDouble("blast-radius", DEFAULT_BLAST_RADIUS));
        int blindTicks = perks == null
                ? DEFAULT_BLIND_TICKS
                : Math.max(0, perks.getInt("blind-ticks", DEFAULT_BLIND_TICKS));
        int shardCount = perks == null
                ? DEFAULT_SHARD_COUNT
                : Math.max(8, Math.min(40, perks.getInt("shard-count", DEFAULT_SHARD_COUNT)));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(60, perks.getInt("duration-ticks", DEFAULT_DURATION));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("paintbomb");
        double blastDamage = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 3.0;
        double damageRadius = damageCfg.enabled() && damageCfg.radius() > 0.0 ? damageCfg.radius() : blastRadius;
        PotionEffectType blindness = PotionTypes.resolve("BLINDNESS", "BLINDNESS");

        ItemDisplay core = PaintBombDisplays.spawn(origin, Material.PINK_CONCRETE_POWDER, 0.85f);
        if (core != null) {
            session.trackEntity(core);
        }

        List<Shard> shards = new ArrayList<>(shardCount);
        List<ItemDisplay> displays = new ArrayList<>(shardCount + 1);
        if (core != null) {
            displays.add(core);
        }

        AtomicInteger tick = new AtomicInteger();
        boolean[] exploded = {false};
        boolean[] finished = {false};
        Set<UUID> hit = new HashSet<>();

        session.onCleanup(() -> {
            PaintBombDisplays.removeAll(displays);
            displays.clear();
            shards.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_NOTE_BLOCK_BIT", origin, 0.8f, 1.4f);
        visuals.sound("ENTITY_SLIME_SQUISH", origin, 0.55f, 1.2f);
        visuals.dust(origin, DUSTS[0], 1.3f, ParticleScale.scale(10), 0.3, 0.25, 0.3, 0.0);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks || (exploded[0] && shards.stream().allMatch(s -> s.done))) {
                finish(visuals, origin, core, shards, displays);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            if (!exploded[0]) {
                charge(visuals, origin, core, current);
                if (current >= CHARGE_TICKS) {
                    exploded[0] = true;
                    detonate(
                            session,
                            visuals,
                            world,
                            killer,
                            victimId,
                            origin,
                            core,
                            shards,
                            displays,
                            shardCount,
                            blastRadius,
                            damageRadius,
                            blastDamage,
                            blindTicks,
                            blindness,
                            hit);
                }
            } else {
                int alive = 0;
                for (Shard shard : shards) {
                    if (shard.done) {
                        continue;
                    }
                    alive++;
                    updateShard(visuals, shard, current);
                }
                if (killer != null && killer.isOnline() && current % 4 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format("&d🎨 PAINTBOMB &8| &f%d &8shards", alive));
                }
            }
            return true;
        });
    }

    private static void charge(VisualEffectService visuals, Location origin, ItemDisplay core, int tick) {
        float pulse = 0.75f + (float) Math.sin(tick * 0.45) * 0.18f + tick * 0.012f;
        float spin = tick * 0.22f;
        if (core != null) {
            PaintBombDisplays.place(core, origin.clone().add(0, Math.sin(tick * 0.25) * 0.08, 0), pulse, spin, 0.2f, spin * 0.5f);
        }
        Color color = DUSTS[tick % DUSTS.length];
        visuals.dust(origin, color, 1.1f, ParticleScale.scale(3), 0.25, 0.25, 0.25, 0.0);
        if (tick % 2 == 0) {
            double a = tick * 0.35;
            Location rim = origin.clone().add(Math.cos(a) * 0.9, 0.1, Math.sin(a) * 0.9);
            visuals.dust(rim, DUSTS[(tick + 3) % DUSTS.length], 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (tick % 5 == 0) {
            visuals.sound("BLOCK_NOTE_BLOCK_PLING", origin, 0.3f, 1.2f + tick * 0.04f);
        }
    }

    private static void detonate(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location origin,
            ItemDisplay core,
            List<Shard> shards,
            List<ItemDisplay> displays,
            int shardCount,
            double blastRadius,
            double damageRadius,
            double blastDamage,
            int blindTicks,
            PotionEffectType blindness,
            Set<UUID> hit) {
        visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.85f, 1.25f);
        visuals.sound("ENTITY_FIREWORK_ROCKET_BLAST", origin, 0.7f, 1.1f);
        visuals.sound("ENTITY_SLIME_SQUISH_SMALL", origin, 0.8f, 0.7f);
        visuals.particle("FLASH", origin, 1, 0.0, 0.0, 0.0, 0.0, null);
        visuals.particle("CLOUD", origin, ParticleScale.scale(8), 0.4, 0.3, 0.4, 0.02, null);

        for (int i = 0; i < DUSTS.length; i++) {
            visuals.dust(origin, DUSTS[i], 1.5f, ParticleScale.scale(6), 0.55, 0.4, 0.55, 0.0);
        }

        PaintBombDisplays.remove(core);
        displays.remove(core);

        for (int i = 0; i < shardCount; i++) {
            double yaw = (Math.PI * 2.0 * i) / shardCount + Math.random() * 0.2;
            double pitch = 0.25 + Math.random() * 0.55;
            Vector vel = new Vector(
                            Math.cos(yaw) * Math.cos(pitch),
                            Math.sin(pitch),
                            Math.sin(yaw) * Math.cos(pitch))
                    .normalize()
                    .multiply(0.35 + Math.random() * 0.35);
            Material mat = POWDERS[i % POWDERS.length];
            float scale = 0.28f + (i % 4) * 0.06f;
            ItemDisplay display = PaintBombDisplays.spawn(origin, mat, scale);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            displays.add(display);
            shards.add(new Shard(display, origin.clone(), vel, scale, DUSTS[i % DUSTS.length], i));
        }

        double blastSq = blastRadius * blastRadius;
        double damageSq = damageRadius * damageRadius;
        for (Player player : findNearby(world, origin, killer, victimId, blastRadius, session)) {
            if (player.getLocation().distanceSquared(origin) > blastSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (blindness != null && blindTicks > 0) {
                player.addPotionEffect(new PotionEffect(blindness, blindTicks, 0, true, true, true));
            }
            if (blastDamage > 0.0
                    && killer != null
                    && player.getLocation().distanceSquared(origin) <= damageSq
                    && hit.add(player.getUniqueId())) {
                player.damage(blastDamage, killer);
            }
            Vector knock = player.getLocation().toVector().subtract(origin.toVector());
            if (knock.lengthSquared() > 1.0e-4) {
                knock.normalize().multiply(0.45).setY(0.28);
                player.setVelocity(player.getVelocity().multiply(0.4).add(knock));
            }
            visuals.dust(
                    player.getLocation().clone().add(0, 1, 0),
                    DUSTS[player.getEntityId() % DUSTS.length],
                    1.3f,
                    6,
                    0.2,
                    0.25,
                    0.2,
                    0.0);
        }
    }

    private static void updateShard(VisualEffectService visuals, Shard shard, int tick) {
        shard.age++;
        if (shard.age >= SHARD_LIFE) {
            splat(visuals, shard);
            return;
        }
        shard.vel.setY(shard.vel.getY() - GRAVITY);
        shard.loc.add(shard.vel);
        if (shard.loc.getBlock().getType().isSolid()) {
            splat(visuals, shard);
            return;
        }
        float spin = tick * 0.35f + shard.index * 0.4f;
        float scale = shard.scale * (0.9f + (float) Math.sin(tick * 0.3 + shard.index) * 0.1f);
        PaintBombDisplays.place(shard.display, shard.loc, scale, spin, spin * 0.5f, spin * 0.7f);
        if (tick % 2 == shard.index % 2) {
            visuals.dust(shard.loc, shard.color, 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (tick % 3 == shard.index % 3) {
            visuals.dust(shard.loc.clone().add(shard.vel.clone().multiply(-0.3)), shard.color, 0.65f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void splat(VisualEffectService visuals, Shard shard) {
        if (shard.done) {
            return;
        }
        shard.done = true;
        visuals.dust(shard.loc, shard.color, 1.3f, ParticleScale.scale(7), 0.25, 0.15, 0.25, 0.0);
        visuals.particle("CLOUD", shard.loc, 2, 0.1, 0.08, 0.1, 0.01, null);
        visuals.sound("BLOCK_SLIME_BLOCK_BREAK", shard.loc, 0.25f, 1.4f);
        PaintBombDisplays.remove(shard.display);
    }

    private static void finish(
            VisualEffectService visuals,
            Location origin,
            ItemDisplay core,
            List<Shard> shards,
            List<ItemDisplay> displays) {
        visuals.sound("ENTITY_SLIME_DEATH", origin, 0.5f, 1.1f);
        visuals.dust(origin, DUSTS[0], 1.2f, ParticleScale.scale(8), 0.4, 0.3, 0.4, 0.0);
        PaintBombDisplays.remove(core);
        for (Shard shard : shards) {
            PaintBombDisplays.remove(shard.display);
        }
        shards.clear();
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

    private static final class Shard {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector vel;
        private final float scale;
        private final Color color;
        private final int index;
        private int age;
        private boolean done;

        private Shard(ItemDisplay display, Location loc, Vector vel, float scale, Color color, int index) {
            this.display = display;
            this.loc = loc.clone();
            this.vel = vel.clone();
            this.scale = scale;
            this.color = color;
            this.index = index;
        }
    }
}
