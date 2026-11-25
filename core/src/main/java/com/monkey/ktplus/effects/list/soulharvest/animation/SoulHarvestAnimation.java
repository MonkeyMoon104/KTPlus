package com.monkey.ktplus.effects.list.soulharvest.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectGameplay;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.effects.visual.display.DisplayFactory;
import com.monkey.ktplus.effects.visual.display.FxGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class SoulHarvestAnimation {
    private static final int SOUL_COUNT = 12;
    private static final int RISE_TICKS = 58;
    private static final int HARVEST_TICKS = 95;
    private static final int SUCK_TICKS = 20;
    private static final Color SOUL = Color.fromRGB(60, 200, 210);
    private static final Color GHOST = Color.fromRGB(180, 240, 255);
    private static final Color DEEP = Color.fromRGB(30, 80, 100);

    private SoulHarvestAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("soulharvest");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int soulCount = perks == null ? SOUL_COUNT : Math.max(6, Math.min(18, perks.getInt("soul-count", SOUL_COUNT)));
        int harvestTicks = perks == null ? HARVEST_TICKS : Math.max(50, perks.getInt("harvest-ticks", HARVEST_TICKS));
        int total = RISE_TICKS + harvestTicks + SUCK_TICKS;

        EffectDamageConfig damageCfg = context.config().effectDamage("soulharvest");
        double damage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 7.0;
        double radius = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 9.0;
        Player killer = context.killer();

        List<SoulWisp> souls = new ArrayList<>();
        Material[] mats = {Material.SOUL_LANTERN, Material.SOUL_TORCH, Material.SOUL_SAND, Material.SOUL_CAMPFIRE};
        for (int i = 0; i < soulCount; i++) {
            double angle = (Math.PI * 2.0 * i) / soulCount;
            Location at = origin.clone().add(Math.cos(angle) * 1.1, 0.1, Math.sin(angle) * 1.1);
            ItemDisplay display = DisplayFactory.item(at, mats[i % mats.length], 0.46f, 0.55f, 0.46f);
            if (display == null) {
                continue;
            }
            DisplayFactory.glow(display, i % 2 == 0 ? SOUL : GHOST);
            session.trackEntity(display);
            souls.add(new SoulWisp(display, angle, 1.0 + (i % 4) * 0.28));
        }

        session.onCleanup(() -> {
            for (SoulWisp s : souls) {
                DisplayFactory.remove(s.display);
            }
        });
        session.resetDeadline(total + 30L);
        visuals.sound("ENTITY_ALLAY_AMBIENT_WITH_ITEM", origin, 1.25f, 0.85f);
        visuals.sound("BLOCK_SOUL_SOIL_BREAK", origin, 1.2f, 0.72f);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", origin, 1.05f, 0.9f);
        visuals.sound("ENTITY_VEX_AMBIENT", origin, 1.1f, 0.65f);

        AtomicInteger tick = new AtomicInteger();
        boolean[] harvested = {false};
        Location[] suckFrom = new Location[souls.size()];

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total) {
                return false;
            }

            int harvestStart = RISE_TICKS;
            int suckStart = harvestStart + harvestTicks;
            double rise = current < harvestStart
                    ? (current + 1) / (double) RISE_TICKS
                    : 1.0 + Math.sin((current - harvestStart) * 0.07) * 0.08;
            float shell = (float) (1.4 + rise * 2.8);

            if (current < suckStart) {
                for (SoulWisp soul : souls) {
                    double spiral = soul.angle + current * 0.055;
                    double harvestBlend = current >= harvestStart
                            ? (current - harvestStart) / (double) harvestTicks
                            : 0.0;
                    double r = soul.radius * (0.75 + rise * 0.55);
                    r *= 1.0 - harvestBlend * 0.4;
                    double y = rise * 3.6 + Math.sin(spiral * 2.1) * 0.32;
                    Location at = origin.clone().add(Math.cos(spiral) * r, y, Math.sin(spiral) * r);
                    float scale = 0.42f + (float) rise * 0.18f;
                    DisplayFactory.place(soul.display, at, scale, scale * 1.1f, scale, (float) spiral);
                    if (current % 3 == 0) {
                        visuals.dust(at, SOUL, 0.75f, 1, 0.03, 0.05, 0.03, 0);
                    }
                }

                if (current % 2 == 0) {
                    visuals.sphereShell("SOUL", origin.clone().add(0, rise * 2.6, 0), shell, 5, 16, DEEP);
                    visuals.sphereShell("DUST", origin.clone().add(0, rise * 2.1, 0), shell * 0.72f, 3, 12, GHOST);
                }

                if (current >= harvestStart) {
                    int h = current - harvestStart;
                    double ringR = radius * (1.0 - h / (double) harvestTicks * 0.28);
                    FxGeometry.circle(origin, ringR, 0.25 + rise, 22, at -> visuals.particle("SOUL", at, 1, 0, 0, 0, 0, null));
                    if (h % 2 == 0) {
                        pullAndDamage(
                                session, world, origin, killer, ringR, 0.1 + h * 0.0008, h == harvestTicks / 2 && !harvested[0] ? damage : 0);
                    }
                    if (h == harvestTicks / 2 && !harvested[0]) {
                        harvested[0] = true;
                        visuals.sound("ENTITY_ALLAY_AMBIENT_WITH_ITEM", origin, 0.85f, 0.75f);
                        visuals.beam("SOUL", origin, origin.clone().add(0, 10, 0), 20, SOUL);
                    }
                }
                if (current == suckStart - 1) {
                    for (int i = 0; i < souls.size(); i++) {
                        SoulWisp soul = souls.get(i);
                        double spiral = soul.angle + current * 0.055;
                        double r = soul.radius * (0.75 + rise * 0.55);
                        r *= 0.6;
                        double y = rise * 3.6 + Math.sin(spiral * 2.1) * 0.32;
                        suckFrom[i] = origin.clone().add(Math.cos(spiral) * r, y, Math.sin(spiral) * r);
                    }
                }
                return true;
            }

            int suck = current - suckStart;
            double suckT = easeInOut((suck + 1) / (double) SUCK_TICKS);
            Location center = origin.clone().add(0, 1.1, 0);
            for (int i = 0; i < souls.size(); i++) {
                SoulWisp soul = souls.get(i);
                Location from = suckFrom[i] != null ? suckFrom[i] : soul.display.getLocation();
                Location at = from.clone().add(
                        (center.getX() - from.getX()) * suckT,
                        (center.getY() - from.getY()) * suckT,
                        (center.getZ() - from.getZ()) * suckT);
                float scale = (float) (0.55 * (1.0 - suckT * 0.85));
                DisplayFactory.place(soul.display, at, scale, scale * 1.05f, scale, (float) (soul.angle + suck * 0.25));
                if (suck % 2 == 0) {
                    visuals.dust(at, GHOST, 0.85f, 1, 0.02, 0.04, 0.02, 0);
                }
            }
            if (suck % 3 == 0) {
                visuals.particle("SOUL", center, 3, 0.12, 0.12, 0.12, 0.01, null);
            }
            if (suck == SUCK_TICKS - 1) {
                visuals.sound("ENTITY_GENERIC_EXPLODE", origin, 0.55f, 1.35f);
                visuals.particle("SOUL", center, ParticleScale.scale(20), 0.35, 0.35, 0.35, 0.05, null);
                visuals.particle("REVERSE_PORTAL", center, 8, 0.25, 0.25, 0.25, 0.02, null);
                for (SoulWisp s : souls) {
                    DisplayFactory.remove(s.display);
                }
                souls.clear();
            }
            return suck < SUCK_TICKS;
        });
    }

    private static void pullAndDamage(
            EffectSession session,
            World world,
            Location origin,
            Player killer,
            double ringR,
            double pull,
            double damage) {
        double rSq = ringR * ringR;
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.isDead() || !entity.getWorld().equals(world)) {
                continue;
            }
            if (killer != null && entity.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            double dSq = entity.getLocation().distanceSquared(origin);
            if (dSq > rSq) {
                continue;
            }
            if (damage > 0) {
                EffectGameplay.damage(session, entity, damage, killer);
            }
            Vector toward = origin.toVector().subtract(entity.getLocation().toVector());
            toward.setY(0);
            if (toward.lengthSquared() > 0.01) {
                toward.normalize().multiply(pull);
                toward.setY(0.08);
                EffectGameplay.velocity(session, entity, toward, killer);
            }
        }
    }

    private static double easeInOut(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    private static final class SoulWisp {
        private final ItemDisplay display;
        private final double angle;
        private final double radius;

        private SoulWisp(ItemDisplay display, double angle, double radius) {
            this.display = display;
            this.angle = angle;
            this.radius = radius;
        }
    }
}