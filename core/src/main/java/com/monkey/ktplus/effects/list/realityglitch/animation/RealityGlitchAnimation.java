package com.monkey.ktplus.effects.list.realityglitch.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.realityglitch.animation.util.RealityGlitchDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class RealityGlitchAnimation {
    private static final int DEFAULT_DURATION_TICKS = 70;
    private static final int DEFAULT_COUNT = 20;
    private static final double DEFAULT_JITTER = 0.6;
    private static final int DEFAULT_CRASH = 12;
    private static final double DEFAULT_CRASH_DMG = 9.0;
    private static final double DEFAULT_SAMPLE = 5.0;

    private static final Color GLITCH_A = Color.fromRGB(220, 40, 255);
    private static final Color GLITCH_B = Color.fromRGB(40, 255, 200);
    private static final Color CRASH = Color.fromRGB(255, 255, 255);

    private static final Material[] FALLBACK = {
        Material.STONE, Material.DIRT, Material.COBBLESTONE, Material.ANDESITE, Material.DEEPSLATE
    };

    private static final String[] EFFECT_SOUNDS = {
        "BLOCK_PORTAL_AMBIENT",
        "BLOCK_NOTE_BLOCK_BIT",
        "ENTITY_ENDERMAN_TELEPORT",
        "BLOCK_BEACON_DEACTIVATE",
        "ENTITY_GENERIC_EXPLODE",
        "BLOCK_GLASS_BREAK",
        "UI_BUTTON_CLICK"
    };

    private RealityGlitchAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.1, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("realityglitch");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int glitchCount = perks == null
                ? DEFAULT_COUNT
                : Math.max(1, perks.getInt("glitch-count", DEFAULT_COUNT));
        double jitterAmp = perks == null
                ? DEFAULT_JITTER
                : Math.max(0.05, perks.getDouble("jitter-amp", DEFAULT_JITTER));
        int crashTicks = perks == null
                ? DEFAULT_CRASH
                : Math.max(1, perks.getInt("crash-ticks", DEFAULT_CRASH));
        double crashDamage = perks == null
                ? DEFAULT_CRASH_DMG
                : Math.max(0.0, perks.getDouble("crash-damage", DEFAULT_CRASH_DMG));
        double sampleRadius = perks == null
                ? DEFAULT_SAMPLE
                : Math.max(0.5, perks.getDouble("sample-radius", DEFAULT_SAMPLE));
        final int durationTicks = perks == null
                ? DEFAULT_DURATION_TICKS
                : Math.max(crashTicks + 8, perks.getInt("duration-ticks", DEFAULT_DURATION_TICKS));

        EffectDamageConfig damageCfg = context.config().effectDamage("realityglitch");
        double damageValue = damageCfg.enabled() ? Math.max(2.0, damageCfg.value()) : crashDamage;
        double damageRadius = damageCfg.enabled() ? Math.max(3.0, damageCfg.radius()) : 8.0;

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        List<Block> samples = RealityGlitchDisplays.sampleSurface(
                world, origin, killer, session, sampleRadius, glitchCount);
        List<Glitch> glitches = new ArrayList<>();
        for (int i = 0; i < glitchCount; i++) {
            Location base;
            org.bukkit.block.data.BlockData data;
            if (i < samples.size()) {
                Block b = samples.get(i);
                base = b.getLocation().add(0.5, 0.0, 0.5);
                data = b.getBlockData();
            } else {
                double a = Math.random() * Math.PI * 2.0;
                double d = Math.random() * sampleRadius;
                base = origin.clone().add(Math.cos(a) * d, 0.0, Math.sin(a) * d);
                data = FALLBACK[i % FALLBACK.length].createBlockData();
            }
            BlockDisplay display = RealityGlitchDisplays.spawn(base.clone().add(0, 0.05, 0), data);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            glitches.add(new Glitch(display, base, Math.random() * Math.PI * 2.0));
        }

        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        boolean[] crashed = {false};
        int jitterPhase = Math.max(8, durationTicks - crashTicks - 8);

        session.onCleanup(() -> {
            stopSounds(origin);
            for (Glitch g : glitches) {
                RealityGlitchDisplays.remove(g.display);
            }
            glitches.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_PORTAL_AMBIENT", origin, 1.0f, 1.6f);
        visuals.sound("BLOCK_NOTE_BLOCK_BIT", origin, 0.8f, 0.5f);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopSounds(origin);
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks) {
                cleanup(visuals, origin, glitches, killer);
                finished[0] = true;
                return false;
            }

            if (current < jitterPhase) {
                for (Glitch g : glitches) {
                    double jx = (Math.random() - 0.5) * jitterAmp * 2.0;
                    double jy = Math.random() * jitterAmp * 1.4;
                    double jz = (Math.random() - 0.5) * jitterAmp * 2.0;
                    Location at = g.base.clone().add(jx, 0.05 + jy, jz);
                    float yaw = (float) ((Math.random() - 0.5) * 1.2);
                    float pitch = (float) ((Math.random() - 0.5) * 0.8);
                    float roll = (float) ((Math.random() - 0.5) * 0.8);
                    float scale = 0.85f + (float) Math.random() * 0.45f;
                    RealityGlitchDisplays.place(g.display, at, scale, yaw, pitch, roll);
                }
                if (current % 4 == 0) {
                    visuals.dust(origin.clone().add(0, 1, 0), current % 8 == 0 ? GLITCH_A : GLITCH_B, 1.4f, 8, sampleRadius * 0.3, 0.6, sampleRadius * 0.3, 0.0);
                }
                if (current % 7 == 0) {
                    visuals.sound("UI_BUTTON_CLICK", origin, 0.35f, 1.8f + (float) Math.random());
                }
                if (current % 12 == 0) {
                    visuals.sound("ENTITY_ENDERMAN_TELEPORT", origin, 0.4f, 1.5f);
                }
            } else if (!crashed[0]) {
                crashed[0] = true;
                doCrash(session, visuals, world, origin, killer, victimId, glitches, damageValue, damageRadius);
            } else {
                int crashAge = current - jitterPhase;
                for (Glitch g : glitches) {
                    double sink = crashAge * 0.22;
                    Location at = g.base.clone().add(0, 0.05 - sink, 0);
                    float spin = crashAge * 0.4f + (float) g.phase;
                    RealityGlitchDisplays.place(
                            g.display,
                            at,
                            Math.max(0.15f, 1.1f - crashAge * 0.04f),
                            spin,
                            spin * 0.4f,
                            -spin);
                }
                if (crashAge > crashTicks) {
                    cleanup(visuals, origin, glitches, killer);
                    finished[0] = true;
                    return false;
                }
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                String phase = crashed[0] ? "&cCRASH" : "&dGLITCH";
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&5▣ REALITY GLITCH &8| %s &8| &7BLOCKS &f%d",
                                phase,
                                glitches.size()));
            }
            return true;
        });
    }

    private static void doCrash(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            List<Glitch> glitches,
            double damage,
            double radius) {
        visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 1.5f, 0.7f);
        visuals.sound("BLOCK_GLASS_BREAK", origin, 1.2f, 0.45f);
        visuals.sound("BLOCK_BEACON_DEACTIVATE", origin, 1.0f, 0.6f);
        visuals.dust(origin.clone().add(0, 1, 0), CRASH, 2.5f, 70, radius * 0.3, 1.0, radius * 0.3, 0.0);
        visuals.dust(origin.clone().add(0, 1.2, 0), GLITCH_A, 2.0f, 40, radius * 0.35, 0.8, radius * 0.35, 0.0);
        visuals.particle("EXPLOSION", origin.clone().add(0, 1, 0), ParticleScale.scale(8), 1.5, 0.8, 1.5, 0.0, null);

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
            if (player.getLocation().distanceSquared(origin) > radiusSq) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (killer != null) {
                player.damage(damage, killer);
            } else {
                player.damage(damage);
            }
            Vector away = player.getLocation().toVector().subtract(origin.toVector());
            away.setY(0);
            if (away.lengthSquared() < 0.01) {
                away = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            away.normalize().multiply(1.4).setY(0.65);
            player.setVelocity(away);
        }

        for (Glitch g : glitches) {
            RealityGlitchDisplays.place(g.display, g.base.clone().add(0, 1.2, 0), 1.35f, 0.5f, 0.5f, 0.5f);
        }
    }

    private static void cleanup(
            VisualEffectService visuals, Location origin, List<Glitch> glitches, Player killer) {
        visuals.particle("CLOUD", origin.clone().add(0, 1, 0), ParticleScale.scale(20), 1.5, 0.8, 1.5, 0.02, null);
        for (Glitch g : glitches) {
            RealityGlitchDisplays.remove(g.display);
        }
        glitches.clear();
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

    private static final class Glitch {
        private final BlockDisplay display;
        private final Location base;
        private final double phase;

        private Glitch(BlockDisplay display, Location base, double phase) {
            this.display = display;
            this.base = base.clone();
            this.phase = phase;
        }
    }
}
