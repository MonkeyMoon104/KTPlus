package com.monkey.ktplus.effects.list.crystalfracture.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectGameplay;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.effects.visual.display.DisplayFactory;
import com.monkey.ktplus.effects.visual.display.FxGeometry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class CrystalFractureAnimation {
    private static final int GROW_TICKS = 50;
    private static final int SEEK_TICKS = 45;
    private static final int BURST_TICKS = 22;
    private static final int SHARD_COUNT = 18;
    private static final Color TEAL = Color.fromRGB(55, 215, 195);
    private static final Color MINT = Color.fromRGB(150, 255, 235);
    private static final Color GLOW = Color.fromRGB(200, 120, 255);

    private CrystalFractureAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.35, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("crystalfracture");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int growTicks = perks == null ? GROW_TICKS : Math.max(30, perks.getInt("crack-ticks", GROW_TICKS));
        int shardCount = perks == null ? SHARD_COUNT : Math.max(10, perks.getInt("shard-count", SHARD_COUNT));

        EffectDamageConfig damageCfg = context.config().effectDamage("crystalfracture");
        double damage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 5.0;
        double hitRadius = 1.8;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        int total = growTicks + SEEK_TICKS + BURST_TICKS;

        Material[] geode = {
            Material.AMETHYST_BLOCK,
            Material.BUDDING_AMETHYST,
            Material.CALCITE,
            Material.AMETHYST_CLUSTER,
            Material.SEA_LANTERN
        };
        List<BlockDisplay> cores = new ArrayList<>();
        for (int i = 0; i < geode.length; i++) {
            double angle = (Math.PI * 2.0 * i) / geode.length;
            Location at = origin.clone().add(Math.cos(angle) * 0.25, i * 0.12, Math.sin(angle) * 0.25);
            BlockDisplay block = DisplayFactory.block(at, geode[i], 0.32f, 0.38f, 0.32f);
            if (block == null) {
                continue;
            }
            DisplayFactory.glow(block, i % 2 == 0 ? TEAL : GLOW);
            session.trackEntity(block);
            cores.add(block);
        }

        List<SeekingShard> shards = new ArrayList<>();
        session.onCleanup(() -> {
            for (BlockDisplay c : cores) {
                DisplayFactory.remove(c);
            }
            for (SeekingShard s : shards) {
                DisplayFactory.remove(s.display);
            }
        });
        session.resetDeadline(total + 30L);
        visuals.sound("BLOCK_AMETHYST_BLOCK_RESONATE", origin, 1.0f, 1.2f);

        AtomicInteger tick = new AtomicInteger();
        boolean[] shattered = {false};
        boolean[] hitApplied = new boolean[64];

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total && shards.isEmpty()) {
                return false;
            }

            if (current < growTicks) {
                double progress = easeOut((current + 1) / (double) growTicks);
                double drop = easeIn(progress);
                float spin = current * 0.048f;
                for (int i = 0; i < cores.size(); i++) {
                    BlockDisplay core = cores.get(i);
                    double angle = (Math.PI * 2.0 * i) / Math.max(1, cores.size()) + spin;
                    double endR = 0.2 + progress * (0.9 + i * 0.12);
                    double endY = progress * 1.35;
                    double startR = 2.6 + i * 0.38;
                    double startY = 6.0 + (i % 4) * 0.55;
                    double r = startR * (1.0 - progress) + endR * progress;
                    double y = startY * (1.0 - drop) + endY * drop;
                    float sx = 0.32f + (float) progress * 0.48f;
                    float h = 0.38f + (float) Math.sin(progress * Math.PI) * 0.55f;
                    Location at = origin.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                    DisplayFactory.place(core, at, sx, h, sx, (float) (angle + current * 0.035));
                }
                if (current % 3 == 0) {
                    double shellY = 4.0 * (1.0 - progress) + progress * 1.2;
                    visuals.dust(origin.clone().add(0, shellY, 0), MINT, 0.9f, 2, 0.12, 0.12, 0.12, 0);
                    visuals.particle("END_ROD", origin.clone().add(0, shellY, 0), 2, 0.35, 0.15, 0.35, 0.01, null);
                }
                if (current % 9 == 0) {
                    visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", origin, 0.5f, 1.0f + (float) progress * 0.35f);
                    crackLines(visuals, origin, progress);
                }
                return true;
            }

            int seekStart = growTicks;
            int burstStart = growTicks + SEEK_TICKS;

            if (current == seekStart && !shattered[0]) {
                shattered[0] = true;
                for (BlockDisplay c : cores) {
                    DisplayFactory.remove(c);
                }
                cores.clear();
                visuals.sound("BLOCK_GLASS_BREAK", origin, 1.15f, 0.85f);
                visuals.particle("BLOCK", origin, ParticleScale.scale(12), 0.35, 0.35, 0.35, 0.04, null,
                        Material.AMETHYST_BLOCK.createBlockData());
                for (int i = 0; i < shardCount; i++) {
                    double angle = (Math.PI * 2.0 * i) / shardCount;
                    Location at = origin.clone().add(Math.cos(angle) * 0.4, 1.1, Math.sin(angle) * 0.4);
                    ItemDisplay shard = DisplayFactory.item(at, Material.AMETHYST_SHARD, 0.38f, 0.38f, 0.38f);
                    if (shard == null) {
                        continue;
                    }
                    DisplayFactory.glow(shard, TEAL);
                    session.trackEntity(shard);
                    shards.add(new SeekingShard(shard, angle, 0.85 + (i % 4) * 0.2));
                }
            }

            if (current < burstStart) {
                int local = current - seekStart;
                double hunt = (local + 1) / (double) SEEK_TICKS;
                Player target = nearestPlayer(world, origin, killer, victimId, 14.0);
                for (int i = 0; i < shards.size(); i++) {
                    SeekingShard shard = shards.get(i);
                    double angle = shard.phase + local * 0.1;
                    double r = shard.radius * (0.6 + hunt * 0.7);
                    double y = 1.0 + Math.sin(local * 0.22 + i) * 0.35;
                    Location at = origin.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                    if (target != null && hunt > 0.25) {
                        Location aim = target.getLocation().clone().add(0, 1.0, 0);
                        Vector lerp = aim.toVector().subtract(at.toVector()).multiply(0.08 * hunt);
                        at.add(lerp.getX(), lerp.getY(), lerp.getZ());
                    }
                    DisplayFactory.place(shard.display, at, 0.38f, 0.38f, 0.38f, (float) (local * 0.4 + i));
                    if (local % 2 == 0) {
                        visuals.particle("END_ROD", at, 1, 0.02, 0.02, 0.02, 0.005, null);
                    }
                    if (target != null && at.distanceSquared(target.getLocation()) < hitRadius * hitRadius) {
                        if (i < hitApplied.length && !hitApplied[i]) {
                            hitApplied[i] = true;
                            EffectGameplay.damage(session, target, damage * 0.35, killer);
                        }
                    }
                }
                if (local % 4 == 0) {
                    visuals.sphereShell("DUST", origin.clone().add(0, 1.2, 0), 1.2 + hunt * 1.5, 4, 10, MINT);
                }
                return true;
            }

            int burst = current - burstStart;
            double burstT = (burst + 1) / (double) BURST_TICKS;
            for (int i = 0; i < shards.size(); i++) {
                SeekingShard shard = shards.get(i);
                double angle = shard.phase + burst * 0.35;
                double r = shard.radius + burstT * 3.5;
                Location at = origin.clone().add(Math.cos(angle) * r, 1.2 + burstT * 2.0, Math.sin(angle) * r);
                float sc = (float) Math.max(0.05, 1.0 - burstT);
                DisplayFactory.place(shard.display, at, 0.35f * sc, 0.35f * sc, 0.35f * sc, (float) angle);
            }
            if (burst == 0) {
                visuals.sound("BLOCK_AMETHYST_BLOCK_HIT", origin, 1.0f, 1.3f);
                FxGeometry.sphereShell(origin.clone().add(0, 1, 0), 2.5, 5, 14, at -> visuals.particle("END_ROD", at, 1, 0, 0, 0, 0, null));
            }
            if (burst >= BURST_TICKS - 1) {
                for (SeekingShard s : shards) {
                    DisplayFactory.remove(s.display);
                }
                shards.clear();
            }
            return burst < BURST_TICKS;
        });
    }

    private static void crackLines(VisualEffectService visuals, Location origin, double progress) {
        int lines = 6;
        double reach = 0.5 + progress * 1.6;
        for (int i = 0; i < lines; i++) {
            double a = (Math.PI * 2.0 * i) / lines + progress * 0.4;
            Location tip = origin.clone().add(Math.cos(a) * reach, progress * 0.9, Math.sin(a) * reach);
            visuals.dust(tip, GLOW, 0.7f, 2, 0.03, 0.03, 0.03, 0);
        }
    }

    private static Player nearestPlayer(World world, Location origin, Player killer, UUID victimId, double range) {
        double rangeSq = range * range;
        return world.getPlayers().stream()
                .filter(p -> p.isOnline() && !p.isDead() && p.getWorld().equals(world))
                .filter(p -> killer == null || !p.getUniqueId().equals(killer.getUniqueId()))
                .filter(p -> victimId == null || !p.getUniqueId().equals(victimId))
                .filter(p -> p.getLocation().distanceSquared(origin) <= rangeSq)
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(origin)))
                .orElse(null);
    }

    private static double easeOut(double t) {
        return 1.0 - (1.0 - t) * (1.0 - t);
    }

    private static double easeIn(double t) {
        return t * t;
    }

    private static final class SeekingShard {
        private final ItemDisplay display;
        private final double phase;
        private final double radius;

        private SeekingShard(ItemDisplay display, double phase, double radius) {
            this.display = display;
            this.phase = phase;
            this.radius = radius;
        }
    }
}