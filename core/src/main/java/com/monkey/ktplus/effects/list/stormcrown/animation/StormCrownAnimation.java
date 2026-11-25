package com.monkey.ktplus.effects.list.stormcrown.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.runtime.EffectGameplay;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.effects.visual.display.DisplayFactory;
import com.monkey.ktplus.effects.visual.display.FxGeometry;
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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class StormCrownAnimation {
    private static final int TOTAL = 220;
    private static final int SPIKES = 12;
    private static final int CHAIN_INTERVAL = 18;
    private static final Color STORM = Color.fromRGB(255, 235, 120);
    private static final Color SKY = Color.fromRGB(180, 210, 255);
    private static final Color FLASH = Color.fromRGB(255, 255, 200);

    private StormCrownAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.1, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        EffectDamageConfig damageCfg = context.config().effectDamage("stormcrown");
        double hopDamage = damageCfg.enabled() ? Math.max(1.0, damageCfg.value()) : 5.0;
        double range = damageCfg.enabled() ? Math.max(2.0, damageCfg.radius()) : 8.0;
        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;

        List<ItemDisplay> crown = new ArrayList<>();
        for (int i = 0; i < SPIKES; i++) {
            ItemDisplay rod = DisplayFactory.item(origin, Material.LIGHTNING_ROD, 0.65f, 1.15f, 0.65f);
            if (rod == null) {
                continue;
            }
            DisplayFactory.glow(rod, STORM);
            session.trackEntity(rod);
            crown.add(rod);
        }

        session.onCleanup(() -> {
            for (ItemDisplay r : crown) {
                DisplayFactory.remove(r);
            }
            PerkActionBar.clear(killer);
        });
        session.resetDeadline(TOTAL + 20L);
        visuals.sound("ENTITY_LIGHTNING_BOLT_THUNDER", origin, 0.95f, 1.05f);
        visuals.sound("ENTITY_LIGHTNING_BOLT_IMPACT", origin, 0.75f, 1.2f);
        PerkActionBar.show(killer, "&e&l⚡ STORM CROWN &8› &fchaining");

        AtomicInteger tick = new AtomicInteger();
        Set<UUID> zapped = new HashSet<>();

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= TOTAL) {
                PerkActionBar.clear(killer);
                return false;
            }

            double spin = current * 0.09;
            float height = 5.2f + (float) Math.sin(current * 0.07) * 0.35f;
            double crownR = 2.9;
            for (int i = 0; i < crown.size(); i++) {
                double a = (Math.PI * 2.0 * i) / crown.size() + spin;
                Location at = origin.clone().add(Math.cos(a) * crownR, height, Math.sin(a) * crownR);
                DisplayFactory.place(crown.get(i), at, 0.58f, 1.05f, 0.58f, (float) (a + Math.PI));
            }
            Location crownCenter = origin.clone().add(0, height, 0);
            visuals.dust(crownCenter, SKY, 1.05f, 3, 0.35, 0.25, 0.35, 0);
            FxGeometry.circle(origin, crownR, height, 24, at -> visuals.particle("ELECTRIC_SPARK", at, 1, 0.02, 0.04, 0.02, 0.01, null));

            if (current % CHAIN_INTERVAL == 3) {
                List<LivingEntity> targets = nearby(world, origin, killer, victimId, range, 5);
                for (LivingEntity target : targets) {
                    if (zapped.contains(target.getUniqueId())) {
                        continue;
                    }
                    zapped.add(target.getUniqueId());
                    Location targetPoint = target.getLocation().clone().add(0, 1.0, 0);
                    visuals.beam("END_ROD", crownCenter, targetPoint, 18, FLASH);
                    FxGeometry.beam(crownCenter, targetPoint, 12, at ->
                            visuals.particle("ELECTRIC_SPARK", at, 2, 0.02, 0.02, 0.02, 0.02, null));
                    visuals.sound("ENTITY_LIGHTNING_BOLT_THUNDER", target.getLocation(), 0.85f, 1.15f);
                    visuals.sound("ENTITY_LIGHTNING_BOLT_IMPACT", target.getLocation(), 0.9f, 1.05f);
                    EffectGameplay.damage(session, target, hopDamage * 0.85, killer);
                    PerkActionBar.show(killer, "&e&l⚡ STORM CROWN &8› &fchaining");
                }
            }
            return true;
        });
    }

    private static List<LivingEntity> nearby(
            World world, Location origin, Player killer, UUID victimId, double range, int max) {
        double rSq = range * range;
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.isDead() || killer != null && entity.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && entity.getUniqueId().equals(victimId)) {
                continue;
            }
            if (entity.getLocation().distanceSquared(origin) > rSq) {
                continue;
            }
            out.add(entity);
        }
        out.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(origin)));
        if (out.size() > max) {
            return out.subList(0, max);
        }
        return out;
    }
}