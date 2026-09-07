package com.monkey.ktplus.effects.list.vinegrasp.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.vinegrasp.animation.util.VineGraspDisplays;
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
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class VineGraspAnimation {
    private static final int DEFAULT_VINE_COUNT = 7;
    private static final int DEFAULT_HOLD_TICKS = 45;
    private static final double DEFAULT_PULL_STRENGTH = 0.25;
    private static final double DEFAULT_PERK_DAMAGE = 2.0;
    private static final int DEFAULT_DURATION = 160;
    private static final int GROW_TICKS = 18;

    private static final Color LEAF = Color.fromRGB(45, 140, 55);
    private static final Color VINE = Color.fromRGB(30, 100, 40);
    private static final Color SAP = Color.fromRGB(90, 180, 70);
    private static final Color DARK = Color.fromRGB(20, 70, 30);

    private enum Phase {
        GROW,
        HOLD,
        WILT,
        DONE
    }

    private VineGraspAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("vinegrasp");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int vineCount = perks == null
                ? DEFAULT_VINE_COUNT
                : Math.max(3, Math.min(14, perks.getInt("vine-count", DEFAULT_VINE_COUNT)));
        int holdTicks = perks == null
                ? DEFAULT_HOLD_TICKS
                : Math.max(20, perks.getInt("hold-ticks", DEFAULT_HOLD_TICKS));
        double pullStrength = perks == null
                ? DEFAULT_PULL_STRENGTH
                : Math.max(0.05, perks.getDouble("pull-strength", DEFAULT_PULL_STRENGTH));
        double perkDamage = perks == null
                ? DEFAULT_PERK_DAMAGE
                : Math.max(0.0, perks.getDouble("damage", DEFAULT_PERK_DAMAGE));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(80, perks.getInt("duration-ticks", DEFAULT_DURATION));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("vinegrasp");
        double tickDamage = damageCfg.enabled() ? Math.max(perkDamage, damageCfg.value()) : perkDamage;
        double damageRadius = damageCfg.enabled() && damageCfg.radius() > 0.0 ? damageCfg.radius() : 5.0;

        List<Player> targets = findNearby(world, origin, killer, victimId, damageRadius + 2.0, session);
        List<Vine> vines = new ArrayList<>(vineCount);
        List<BlockDisplay> displays = new ArrayList<>(vineCount);
        Material[] mats = {Material.VINE, Material.MOSS_BLOCK, Material.MOSS_CARPET, Material.OAK_LEAVES};

        for (int i = 0; i < vineCount; i++) {
            Location base;
            Player bound = null;
            if (!targets.isEmpty()) {
                bound = targets.get(i % targets.size());
                Location feet = bound.getLocation();
                double a = (Math.PI * 2.0 * i) / vineCount + Math.random() * 0.3;
                base = feet.clone().add(Math.cos(a) * 0.85, 0.0, Math.sin(a) * 0.85);
                base.setY(origin.getY());
            } else {
                double a = (Math.PI * 2.0 * i) / vineCount;
                base = origin.clone().add(Math.cos(a) * (1.4 + i % 3 * 0.4), 0.0, Math.sin(a) * (1.4 + i % 3 * 0.4));
            }
            float width = 0.35f + (i % 3) * 0.08f;
            BlockDisplay display = VineGraspDisplays.spawn(base, mats[i % mats.length], width);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            displays.add(display);
            vines.add(new Vine(
                    display,
                    base,
                    bound != null ? bound.getUniqueId() : null,
                    width,
                    1.6f + (i % 4) * 0.25f,
                    i,
                    (i % 4) * 2));
        }

        if (vines.isEmpty()) {
            return;
        }

        Set<UUID> damaged = new HashSet<>();
        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};

        session.onCleanup(() -> {
            VineGraspDisplays.removeAll(displays);
            displays.clear();
            vines.clear();
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_GRASS_BREAK", origin, 0.9f, 0.7f);
        visuals.sound("BLOCK_VINE_STEP", origin, 0.7f, 0.85f);
        visuals.dust(origin, LEAF, 1.3f, ParticleScale.scale(12), 0.5, 0.2, 0.5, 0.0);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks || vines.stream().allMatch(v -> v.phase == Phase.DONE)) {
                finish(visuals, origin, vines, displays);
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            int holding = 0;
            for (Vine vine : vines) {
                if (vine.phase == Phase.DONE) {
                    continue;
                }
                if (current < vine.spawnDelay) {
                    continue;
                }
                Player bound = resolveBound(world, vine, killer, victimId, origin, damageRadius, session);
                switch (vine.phase) {
                    case GROW -> {
                        updateGrow(visuals, vine, current);
                        if (vine.growProgress >= 1.0) {
                            vine.phase = Phase.HOLD;
                            vine.holdLeft = holdTicks;
                            visuals.sound("BLOCK_VINE_PLACE", vine.base, 0.55f, 0.9f);
                        }
                    }
                    case HOLD -> {
                        holding++;
                        updateHold(
                                session,
                                visuals,
                                killer,
                                vine,
                                bound,
                                pullStrength,
                                tickDamage,
                                damaged,
                                current);
                        vine.holdLeft--;
                        if (vine.holdLeft <= 0) {
                            vine.phase = Phase.WILT;
                            vine.age = 0;
                        }
                    }
                    case WILT -> {
                        updateWilt(visuals, vine, current);
                        if (vine.age >= 12) {
                            VineGraspDisplays.remove(vine.display);
                            vine.phase = Phase.DONE;
                        }
                    }
                    default -> {
                    }
                }
            }

            if (current % 8 == 0) {
                visuals.sound("BLOCK_GRASS_STEP", origin, 0.22f, 0.75f + (float) (Math.random() * 0.2));
            }

            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format("&2🌿 VINEGRASP &8| &aBIND &f%d", holding));
            }
            return true;
        });
    }

    private static void updateGrow(VisualEffectService visuals, Vine vine, int tick) {
        vine.age++;
        vine.growProgress = Math.min(1.0, vine.age / (double) GROW_TICKS);
        double ease = 1.0 - Math.pow(1.0 - vine.growProgress, 2.0);
        float height = (float) (vine.maxHeight * ease);
        float sway = (float) Math.sin(tick * 0.2 + vine.index) * 0.12f;
        VineGraspDisplays.place(
                vine.display,
                vine.base,
                vine.width,
                Math.max(0.15f, height),
                vine.width,
                vine.index * 0.4f + sway,
                0.05f,
                sway * 0.5f);
        if (tick % 2 == vine.index % 2) {
            visuals.dust(vine.base.clone().add(0, height * 0.5, 0), LEAF, 0.8f, 1, 0.0, 0.0, 0.0, 0.0);
            visuals.dust(vine.base.clone().add(0, height, 0), SAP, 0.65f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void updateHold(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Vine vine,
            Player bound,
            double pullStrength,
            double tickDamage,
            Set<UUID> damaged,
            int tick) {
        float sway = (float) Math.sin(tick * 0.25 + vine.index) * 0.18f;
        float pulse = vine.maxHeight * (0.95f + (float) Math.sin(tick * 0.3 + vine.index) * 0.05f);
        Location at = vine.base.clone();
        if (bound != null) {
            Location feet = bound.getLocation();
            at = new Location(feet.getWorld(), feet.getX(), vine.base.getY(), feet.getZ());
            vine.base = at.clone();

            Vector pull = new Vector(0.0, -pullStrength, 0.0);
            Vector toward = at.toVector().subtract(feet.toVector());
            toward.setY(0);
            if (toward.lengthSquared() > 0.04) {
                pull.add(toward.normalize().multiply(pullStrength * 0.55));
            }
            if (killer != null && session.allowsWorldMutation(killer, feet)) {
                bound.setVelocity(bound.getVelocity().multiply(0.35).add(pull));
                if (tickDamage > 0.0 && tick % 10 == vine.index % 10 && damaged.add(bound.getUniqueId())) {
                    final UUID id = bound.getUniqueId();
                    session.runLater(12L, () -> damaged.remove(id));
                    bound.damage(tickDamage, killer);
                    visuals.sound("ENTITY_PLAYER_HURT", feet, 0.55f, 0.85f);
                    visuals.dust(feet.clone().add(0, 0.8, 0), VINE, 1.1f, 5, 0.15, 0.25, 0.15, 0.0);
                }
            }
            if (tick % 3 == vine.index % 3) {
                visuals.dust(feet.clone().add(0, 0.4, 0), LEAF, 0.85f, 1, 0.0, 0.0, 0.0, 0.0);
                visuals.dust(feet.clone().add(0, 0.1, 0), DARK, 0.7f, 1, 0.1, 0.05, 0.1, 0.0);
            }
        }

        VineGraspDisplays.place(
                vine.display, at, vine.width * 1.05f, pulse, vine.width * 1.05f, sway, 0.08f, sway * 0.6f);
        if (tick % 4 == vine.index % 4) {
            visuals.dust(at.clone().add(0, pulse * 0.6, 0), SAP, 0.75f, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void updateWilt(VisualEffectService visuals, Vine vine, int tick) {
        vine.age++;
        float shrink = vine.maxHeight * Math.max(0.1f, 1.0f - vine.age / 12.0f);
        VineGraspDisplays.place(
                vine.display, vine.base, vine.width * 0.8f, shrink, vine.width * 0.8f, tick * 0.05f, 0.2f, 0.1f);
        if (tick % 2 == 0) {
            visuals.dust(vine.base.clone().add(0, shrink * 0.4, 0), DARK, 0.9f, 2, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private static Player resolveBound(
            World world,
            Vine vine,
            Player killer,
            UUID victimId,
            Location origin,
            double radius,
            EffectSession session) {
        if (vine.targetId != null) {
            for (Player player : world.getPlayers()) {
                if (player.getUniqueId().equals(vine.targetId)
                        && player.isOnline()
                        && !player.isDead()
                        && player.getWorld().equals(world)
                        && player.getLocation().distanceSquared(origin) <= radius * radius) {
                    return player;
                }
            }
            vine.targetId = null;
        }
        List<Player> nearby = findNearby(world, origin, killer, victimId, radius, session);
        if (nearby.isEmpty()) {
            return null;
        }
        Player next = nearby.get(vine.index % nearby.size());
        vine.targetId = next.getUniqueId();
        return next;
    }

    private static void finish(
            VisualEffectService visuals, Location origin, List<Vine> vines, List<BlockDisplay> displays) {
        visuals.sound("BLOCK_GRASS_BREAK", origin, 0.8f, 0.6f);
        visuals.dust(origin, LEAF, 1.4f, ParticleScale.scale(14), 0.55, 0.35, 0.55, 0.0);
        visuals.particle("CLOUD", origin, ParticleScale.scale(5), 0.3, 0.2, 0.3, 0.01, null);
        for (Vine vine : vines) {
            VineGraspDisplays.remove(vine.display);
        }
        vines.clear();
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

    private static final class Vine {
        private final BlockDisplay display;
        private Location base;
        private UUID targetId;
        private final float width;
        private final float maxHeight;
        private final int index;
        private final int spawnDelay;
        private Phase phase = Phase.GROW;
        private int age;
        private int holdLeft;
        private double growProgress;

        private Vine(
                BlockDisplay display,
                Location base,
                UUID targetId,
                float width,
                float maxHeight,
                int index,
                int spawnDelay) {
            this.display = display;
            this.base = base.clone();
            this.targetId = targetId;
            this.width = width;
            this.maxHeight = maxHeight;
            this.index = index;
            this.spawnDelay = spawnDelay;
        }
    }
}
