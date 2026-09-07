package com.monkey.ktplus.effects.list.glowmissile.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.list.glowmissile.animation.util.GlowMissileBlocks;
import com.monkey.ktplus.effects.list.glowmissile.animation.util.GlowMissileDebris;
import com.monkey.ktplus.effects.list.glowmissile.animation.util.GlowMissileParticles;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class GlowMissileLauncher {
    public static final long EFFECT_DURATION_TICKS = 500L;

    private static final String EFFECT_ID = "glowmissile";
    private static final int BUILD_BLOCKS_PER_TICK = 3;
    private static final long BUILD_TICK_INTERVAL = 1L;
    private static final long BUILD_PAUSE_TICKS = 15L;
    private static final int RISE_STEPS = 18;
    private static final long RISE_TICK_INTERVAL = 2L;
    private static final double DEFAULT_DEBRIS_DAMAGE = 3.0;
    private static final double DEFAULT_DEBRIS_RADIUS = 1.4;

    private GlowMissileLauncher() {}

    public static void launch(
            EffectSession session, VisualEffectService visuals, EffectContext context, Location startLoc) {
        if (startLoc.getWorld() == null) {
            session.complete();
            return;
        }
        Player killer = context.killer();
        Location anchor = GlowMissileBlocks.resolveAnchor(startLoc);
        Map<Location, Material> buildBlocks = new HashMap<>();
        session.resetDeadline(EFFECT_DURATION_TICKS);
        runBuildingPhase(session, killer, anchor, buildBlocks, () -> session.runLater(
                BUILD_PAUSE_TICKS,
                () -> runLaunchPhase(session, visuals, context, killer, anchor, buildBlocks)));
    }

    private static void runBuildingPhase(
            EffectSession session,
            Player killer,
            Location anchor,
            Map<Location, Material> buildBlocks,
            Runnable onComplete) {
        GlowMissileBlocks.placeAnimated(
                session, killer, anchor, BUILD_BLOCKS_PER_TICK, BUILD_TICK_INTERVAL, buildBlocks, onComplete);
    }

    private static void runLaunchPhase(
            EffectSession session,
            VisualEffectService visuals,
            EffectContext context,
            Player killer,
            Location anchor,
            Map<Location, Material> buildBlocks) {
        GlowMissileBlocks.clearBlocks(session, killer, buildBlocks);
        buildBlocks.clear();
        AtomicInteger height = new AtomicInteger();
        Map<Location, Material> frameBlocks = new HashMap<>();
        AtomicInteger lastRise = new AtomicInteger();
        session.runTimer(0L, RISE_TICK_INTERVAL, () -> {
            int current = height.getAndIncrement();
            if (current >= RISE_STEPS) {
                runFinalPhase(
                        session,
                        visuals,
                        context,
                        killer,
                        GlowMissileBlocks.tipLocation(anchor, lastRise.get()),
                        frameBlocks);
                return false;
            }
            GlowMissileBlocks.clearBlocks(session, killer, frameBlocks);
            Location risen = anchor.clone().add(0, current, 0);
            GlowMissileBlocks.placeFrame(session, killer, risen, frameBlocks);
            lastRise.set(current);
            GlowMissileParticles.spawnEngineParticles(visuals, risen);
            return true;
        });
    }

    private static void runFinalPhase(
            EffectSession session,
            VisualEffectService visuals,
            EffectContext context,
            Player killer,
            Location explosionCenter,
            Map<Location, Material> frameBlocks) {
        EffectDamageConfig damageCfg = context.config().effectDamage(EFFECT_ID);
        if (killer != null) {
            BuiltInDamageService.apply(session, killer, explosionCenter, damageCfg);
        }

        ConfigurationSection section = context.config().effectSection(EFFECT_ID);
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double debrisDamage = perks == null
                ? DEFAULT_DEBRIS_DAMAGE
                : Math.max(0.0, perks.getDouble("debris-damage", DEFAULT_DEBRIS_DAMAGE));
        double debrisRadius = perks == null
                ? DEFAULT_DEBRIS_RADIUS
                : Math.max(0.6, perks.getDouble("debris-radius", DEFAULT_DEBRIS_RADIUS));
        if (damageCfg.enabled()) {
            debrisDamage = Math.max(debrisDamage, damageCfg.value() * 0.35);
        }

        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        boolean[] explosionDone = {false};
        boolean[] debrisDone = {false};
        Runnable maybeComplete = () -> {
            if (explosionDone[0] && debrisDone[0]) {
                session.complete();
            }
        };

        visuals.sound("ENTITY_DRAGON_FIREBALL_EXPLODE", explosionCenter, 2.0f, 1.0f);
        GlowMissileDebris.cascade(
                session,
                visuals,
                killer,
                victimId,
                frameBlocks,
                explosionCenter,
                debrisDamage,
                debrisRadius,
                () -> {
                    debrisDone[0] = true;
                    maybeComplete.run();
                });
        GlowMissileExplosion.start(session, visuals, explosionCenter, () -> {
            explosionDone[0] = true;
            maybeComplete.run();
        });
    }
}
