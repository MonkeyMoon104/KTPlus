package com.monkey.ktplus.effects.list.totem.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.damage.BuiltInDamageService;
import com.monkey.ktplus.effects.list.totem.animation.util.TotemDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;

public final class TotemAnimation {
    private static final int SPIN_TICKS = 24;
    private static final int ARC_TICKS = 10;
    private static final int HOLD_TICKS = 10;
    private static final int SPIRAL_TICKS = 34;
    private static final int FINALE_TICKS = 12;
    private static final int SLOT_COUNT = 5;
    private static final double ORBIT_RADIUS = 5.8;
    private static final double ARC_HEIGHT = 2.2;
    private static final double CENTER_Y = 1.0;
    private static final String TOTEM_PARTICLE = "TOTEM_OF_UNDYING";
    private static final String VILLAGER_PARTICLE = "VILLAGER_HAPPY";
    private static final int SPLASH_JETS = 10;

    private static final OrbitStyle[] ORBITS = {
        new OrbitStyle(0.00, 0.22, 1.15, 0.35, 0.55, 0.42f, -0.28f),
        new OrbitStyle(1.15, 0.31, 0.55, 2.10, -0.40, 0.61f, 0.35f),
        new OrbitStyle(2.40, 0.17, 1.45, 4.20, 0.85, 0.38f, 0.55f),
        new OrbitStyle(3.70, 0.38, 0.85, 0.90, -0.95, 0.72f, -0.48f),
        new OrbitStyle(5.10, 0.26, 1.70, 3.40, 0.25, 0.50f, 0.22f)
    };

    private TotemAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location ground = context.location().clone();
        if (ground.getWorld() == null) {
            return;
        }
        Location center = ground.clone().add(0.0, CENTER_Y, 0.0);
        ItemDisplay main = TotemDisplays.spawnMain(center);
        if (main == null) {
            return;
        }
        session.trackEntity(main);

        List<ItemDisplay> satellites = new ArrayList<>(SLOT_COUNT);
        boolean[] arrived = new boolean[SLOT_COUNT];
        float[] mainYaw = {0.0f};
        float[] selfYaw = new float[SLOT_COUNT];
        float[] selfPitch = new float[SLOT_COUNT];
        float[] selfRoll = new float[SLOT_COUNT];
        double[] phases = new double[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            phases[i] = ORBITS[i].phase;
        }

        AtomicInteger tick = new AtomicInteger();
        int[] spawnIndex = {0};
        int[] arcTick = {0};
        boolean[] mainRemoved = {false};
        boolean[] spiralStarted = {false};

        session.onCleanup(() -> {
            TotemDisplays.remove(main);
            TotemDisplays.removeAll(satellites);
        });

        visuals.sound("ITEM_TOTEM_USE", center, 1.15f, 0.85f);
        TotemDisplays.place(main, center, TotemDisplays.MAIN_SCALE, 0.0f);

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                TotemDisplays.remove(main);
                TotemDisplays.removeAll(satellites);
                return false;
            }
            int current = tick.getAndIncrement();

            if (current < SPIN_TICKS) {
                double progress = current / (double) Math.max(1, SPIN_TICKS - 1);
                mainYaw[0] += (float) (0.06 + progress * 0.28);
                TotemDisplays.place(main, center, TotemDisplays.MAIN_SCALE, mainYaw[0]);
                spawnSpinAura(visuals, center, progress);
                return true;
            }

            if (spawnIndex[0] < SLOT_COUNT) {
                mainYaw[0] += 0.22f + spawnIndex[0] * 0.04f;
                if (main.isValid() && !main.isDead()) {
                    TotemDisplays.place(main, center, TotemDisplays.MAIN_SCALE, mainYaw[0]);
                    spawnMainDriftParticles(visuals, center);
                }

                ensureSatellite(session, satellites, center, spawnIndex[0]);
                if (arcTick[0] == 0) {
                    float pitch = 0.75f + spawnIndex[0] * 0.14f;
                    visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", center, 1.05f, pitch);
                    visuals.sound("ENTITY_EXPERIENCE_ORB_PICKUP", center, 0.55f, 0.85f + spawnIndex[0] * 0.1f);
                }
                updateSingleArc(
                        visuals,
                        center,
                        satellites.get(spawnIndex[0]),
                        spawnIndex[0],
                        arcTick[0],
                        selfYaw,
                        selfPitch,
                        selfRoll);
                holdArrived(visuals, satellites, arrived, center, selfYaw, selfPitch, selfRoll);

                arcTick[0]++;
                if (arcTick[0] >= ARC_TICKS) {
                    arrived[spawnIndex[0]] = true;
                    Location slot = slotLocation(center, spawnIndex[0], ORBIT_RADIUS);
                    TotemDisplays.place(
                            satellites.get(spawnIndex[0]),
                            slot,
                            TotemDisplays.SMALL_SCALE,
                            selfYaw[spawnIndex[0]],
                            selfPitch[spawnIndex[0]],
                            selfRoll[spawnIndex[0]]);
                    spawnIndex[0]++;
                    arcTick[0] = 0;
                    if (spawnIndex[0] >= SLOT_COUNT && !mainRemoved[0]) {
                        TotemDisplays.remove(main);
                        mainRemoved[0] = true;
                    }
                }
                return true;
            }

            if (!mainRemoved[0]) {
                TotemDisplays.remove(main);
                mainRemoved[0] = true;
            }

            int afterSpawn = SPIN_TICKS + SLOT_COUNT * ARC_TICKS;
            int holdTick = current - afterSpawn;
            if (holdTick < HOLD_TICKS) {
                holdArrived(visuals, satellites, arrived, center, selfYaw, selfPitch, selfRoll);
                return true;
            }

            int spiralTick = holdTick - HOLD_TICKS;
            if (spiralTick < SPIRAL_TICKS) {
                if (!spiralStarted[0]) {
                    spiralStarted[0] = true;
                    visuals.sound("ENTITY_ILLUSIONER_CAST_SPELL", center, 1.15f, 1.15f);
                    visuals.sound("BLOCK_BEACON_ACTIVATE", center, 0.85f, 1.35f);
                }
                double progress = spiralTick / (double) Math.max(1, SPIRAL_TICKS - 1);
                double eased = 1.0 - Math.pow(1.0 - progress, 1.55);
                double radius = ORBIT_RADIUS * (1.0 - eased);
                updateSpiralPhase(
                        visuals,
                        center,
                        satellites,
                        selfYaw,
                        selfPitch,
                        selfRoll,
                        phases,
                        radius,
                        spiralTick,
                        progress);
                return true;
            }

            TotemDisplays.removeAll(satellites);
            satellites.clear();
            playFinale(session, visuals, center);
            BuiltInDamageService.apply(session, context.killer(), ground, context.config().effectDamage("totem"));
            return false;
        });
    }

    private static void ensureSatellite(
            EffectSession session, List<ItemDisplay> satellites, Location center, int index) {
        while (satellites.size() <= index) {
            ItemDisplay satellite = TotemDisplays.spawnSmall(center);
            if (satellite == null) {
                return;
            }
            session.trackEntity(satellite);
            satellites.add(satellite);
            TotemDisplays.place(satellite, center, TotemDisplays.SMALL_SCALE, 0.0f);
        }
    }

    private static void holdArrived(
            VisualEffectService visuals,
            List<ItemDisplay> satellites,
            boolean[] arrived,
            Location center,
            float[] selfYaw,
            float[] selfPitch,
            float[] selfRoll) {
        for (int i = 0; i < satellites.size(); i++) {
            if (!arrived[i]) {
                continue;
            }
            ItemDisplay satellite = satellites.get(i);
            if (!satellite.isValid() || satellite.isDead()) {
                continue;
            }
            Location slot = slotLocation(center, i, ORBIT_RADIUS);
            TotemDisplays.place(
                    satellite, slot, TotemDisplays.SMALL_SCALE, selfYaw[i], selfPitch[i], selfRoll[i]);
            int count = ParticleScale.scale(3);
            for (int p = 0; p < count; p++) {
                double angle = Math.random() * Math.PI * 2.0;
                double radius = 0.15 + Math.random() * 0.4;
                Location point = slot.clone().add(
                        Math.cos(angle) * radius,
                        (Math.random() - 0.25) * 0.55,
                        Math.sin(angle) * radius);
                visuals.particle(TOTEM_PARTICLE, point, 1, 0.0, 0.04, 0.0, 0.018, null);
            }
        }
    }

    private static void updateSingleArc(
            VisualEffectService visuals,
            Location center,
            ItemDisplay satellite,
            int index,
            int localArcTick,
            float[] selfYaw,
            float[] selfPitch,
            float[] selfRoll) {
        if (satellite == null || !satellite.isValid() || satellite.isDead()) {
            return;
        }
        OrbitStyle style = ORBITS[index];
        double t = Math.min(1.0, (localArcTick + 1) / (double) ARC_TICKS);
        Location slot = slotLocation(center, index, ORBIT_RADIUS);
        Location along = lerp(center, slot, t);
        along.add(0.0, Math.sin(t * Math.PI) * ARC_HEIGHT, 0.0);
        selfYaw[index] += 0.28f + (float) t * 0.2f;
        selfPitch[index] += style.pitchSpin * 0.35f;
        selfRoll[index] += style.rollSpin * 0.3f;
        TotemDisplays.place(
                satellite,
                along,
                TotemDisplays.SMALL_SCALE,
                selfYaw[index],
                selfPitch[index],
                selfRoll[index]);
        spawnWhiteArcTrail(visuals, center, slot, t);
    }

    private static void updateSpiralPhase(
            VisualEffectService visuals,
            Location center,
            List<ItemDisplay> satellites,
            float[] selfYaw,
            float[] selfPitch,
            float[] selfRoll,
            double[] phases,
            double radius,
            int spiralTick,
            double progress) {
        int count = satellites.size();
        for (int i = 0; i < count; i++) {
            ItemDisplay satellite = satellites.get(i);
            if (!satellite.isValid() || satellite.isDead()) {
                continue;
            }
            OrbitStyle style = ORBITS[i];
            double speed = style.speed * (1.15 + progress * 2.2);
            phases[i] += speed;
            Location at = orbitPoint(center, phases[i], radius, style, progress);
            selfYaw[i] += style.yawSpin + (float) progress * 0.4f;
            selfPitch[i] += style.pitchSpin * (0.85f + (float) progress);
            selfRoll[i] += style.rollSpin * (0.75f + (float) progress * 0.85f);
            TotemDisplays.place(
                    satellite, at, TotemDisplays.SMALL_SCALE, selfYaw[i], selfPitch[i], selfRoll[i]);
            visuals.particle(VILLAGER_PARTICLE, at, 2, 0.1, 0.12, 0.1, 0.0, null);
            Location trail = orbitPoint(center, phases[i] - 0.35, Math.max(0.15, radius * 1.05), style, progress);
            visuals.particle(VILLAGER_PARTICLE, trail, 2, 0.08, 0.1, 0.08, 0.0, null);
            Location trail2 = orbitPoint(center, phases[i] - 0.7, Math.max(0.1, radius * 1.1), style, progress);
            visuals.particle(VILLAGER_PARTICLE, trail2, 1, 0.06, 0.08, 0.06, 0.0, null);
        }
    }

    private static Location orbitPoint(
            Location center, double phase, double radius, OrbitStyle style, double progress) {
        double lx = Math.cos(phase) * radius;
        double ly = Math.sin(phase) * radius * Math.sin(style.tilt);
        double lz = Math.sin(phase) * radius * Math.cos(style.tilt);
        double plane = style.planeYaw + progress * style.planeDrift;
        double cos = Math.cos(plane);
        double sin = Math.sin(plane);
        double wx = lx * cos - lz * sin;
        double wz = lx * sin + lz * cos;
        double wy = ly + Math.sin(phase * 1.7 + style.phase) * 0.4 * (1.0 - progress);
        return center.clone().add(wx, wy, wz);
    }

    private static void playFinale(EffectSession session, VisualEffectService visuals, Location center) {
        visuals.sound("ENTITY_GENERIC_EXPLODE", center, 3.4f, 0.85f);
        visuals.sound("ENTITY_DRAGON_FIREBALL_EXPLODE", center, 2.2f, 0.95f);
        visuals.sound("ITEM_TOTEM_USE", center, 1.35f, 0.8f);

        AtomicInteger tick = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            int current = tick.getAndIncrement();
            if (current >= FINALE_TICKS) {
                return false;
            }

            double shockRadius = 0.35 + current * 2.5;
            int ringPoints = ParticleScale.scale(40);
            for (int i = 0; i < ringPoints; i++) {
                double angle = Math.PI * 2.0 * i / ringPoints;
                Location point = center.clone().add(Math.cos(angle) * shockRadius, 0.08, Math.sin(angle) * shockRadius);
                visuals.dust(point, Color.WHITE, 1.4f, 2, 0.05, 0.02, 0.05, 0.0);
                visuals.particle("CLOUD", point, 1, 0.06, 0.02, 0.06, 0.01, null);
                if (i % 2 == 0) {
                    visuals.particle("CRIT", point, 1, 0.0, 0.0, 0.0, 0.0, null);
                }
            }

            spawnSplashFountain(visuals, center, current);
            return true;
        });
    }

    private static void spawnSplashFountain(VisualEffectService visuals, Location center, int tick) {
        double life = (tick + 0.5) / (double) Math.max(1, FINALE_TICKS);
        int jets = SPLASH_JETS;
        int samples = 9;
        for (int j = 0; j < jets; j++) {
            double angle = Math.PI * 2.0 * j / jets + 0.08;
            double peak = 11.2 + (j % 3) * 0.9;
            double reach = 11.4 + (j % 4) * 0.85;
            double rim = 0.45 + (j % 2) * 0.18;
            drawSplashJet(visuals, center, angle, rim, reach, peak, life, samples);

            if (j % 2 == 0) {
                double mid = angle + Math.PI / jets;
                drawSplashJet(visuals, center, mid, rim + 0.15, reach * 0.78, peak * 0.75, life, 6);
            }
        }
    }

    private static void drawSplashJet(
            VisualEffectService visuals,
            Location center,
            double angle,
            double rim,
            double reach,
            double peak,
            double life,
            int samples) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        
        double maxT = Math.min(1.0, life * 1.15);
        for (int step = 0; step <= samples; step++) {
            double t = (step / (double) samples) * maxT;
            if (t < 0.02) {
                continue;
            }
            double outward = rim + t * reach;
            
            double height = peak * (4.0 * t * (1.0 - t));
            Location point = center.clone().add(cos * outward, height, sin * outward);
            visuals.particle(TOTEM_PARTICLE, point, 1, 0.02, 0.03, 0.02, 0.01, null);
        }
        
        double tipT = maxT;
        double tipOut = rim + tipT * reach;
        double tipY = peak * (4.0 * tipT * (1.0 - tipT));
        Location tip = center.clone().add(cos * tipOut, tipY, sin * tipOut);
        visuals.particle(TOTEM_PARTICLE, tip, 2, 0.04, 0.05, 0.04, 0.02, null);
    }

    private static void spawnSpinAura(VisualEffectService visuals, Location center, double progress) {
        int count = ParticleScale.scale(3 + (int) (progress * 5));
        double radius = 0.35 + progress * 0.45;
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            Location point = center.clone().add(
                    Math.cos(angle) * radius,
                    (Math.random() - 0.5) * 0.5,
                    Math.sin(angle) * radius);
            visuals.particle(TOTEM_PARTICLE, point, 1, 0.0, 0.04, 0.0, 0.015, null);
        }
    }

    private static void spawnMainDriftParticles(VisualEffectService visuals, Location center) {
        int count = ParticleScale.scale(4);
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double radius = 0.2 + Math.random() * 0.55;
            Location point = center.clone().add(
                    Math.cos(angle) * radius,
                    (Math.random() - 0.35) * 0.7,
                    Math.sin(angle) * radius);
            visuals.particle(TOTEM_PARTICLE, point, 1, 0.0, 0.05, 0.0, 0.02, null);
        }
    }

    private static void spawnWhiteArcTrail(VisualEffectService visuals, Location from, Location to, double t) {
        int segments = Math.max(5, ParticleScale.scale(9));
        double trailStart = Math.max(0.0, t - 0.22);
        for (int step = 0; step <= segments; step++) {
            double u = trailStart + (t - trailStart) * (step / (double) segments);
            Location point = lerp(from, to, u);
            point.add(0.0, Math.sin(u * Math.PI) * ARC_HEIGHT, 0.0);
            visuals.dust(point, Color.WHITE, 1.05f, 1, 0.01, 0.01, 0.01, 0.0);
            visuals.particle("CLOUD", point, 1, 0.02, 0.02, 0.02, 0.0, null);
        }
    }

    private static double slotAngle(int slot) {
        return Math.PI * 2.0 * slot / SLOT_COUNT;
    }

    private static Location slotLocation(Location center, int slot, double radius) {
        double angle = slotAngle(slot);
        return center.clone().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
    }

    private static Location lerp(Location from, Location to, double t) {
        return from.clone().add(
                (to.getX() - from.getX()) * t,
                (to.getY() - from.getY()) * t,
                (to.getZ() - from.getZ()) * t);
    }

    private record OrbitStyle(
            double phase,
            double speed,
            double tilt,
            double planeYaw,
            double planeDrift,
            float yawSpin,
            float pitchSpin,
            float rollSpin) {
        private OrbitStyle(
                double phase,
                double speed,
                double tilt,
                double planeYaw,
                double planeDrift,
                float yawSpin,
                float pitchSpin) {
            this(phase, speed, tilt, planeYaw, planeDrift, yawSpin, pitchSpin, pitchSpin * 0.7f);
        }
    }
}
