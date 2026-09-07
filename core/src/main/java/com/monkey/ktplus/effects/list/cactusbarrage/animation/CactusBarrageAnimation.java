package com.monkey.ktplus.effects.list.cactusbarrage.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.cactusbarrage.animation.util.CactusBarrageDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
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
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class CactusBarrageAnimation {
    private static final int DEFAULT_SPINE_COUNT = 16;
    private static final double DEFAULT_SPREAD = 0.45;
    private static final double DEFAULT_SPEED = 0.65;
    private static final int DEFAULT_STICK_TICKS = 25;
    private static final double DEFAULT_BLEED_DAMAGE = 1.5;
    private static final double HIT_DISTANCE = 1.05;
    private static final int MAX_TICKS = 130;
    private static final int MAX_FLIGHT = 45;

    private static final Color LEAF = Color.fromRGB(55, 150, 55);
    private static final Color CACTUS = Color.fromRGB(35, 110, 45);
    private static final Color BURST = Color.fromRGB(120, 200, 90);

    private CactusBarrageAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.0, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("cactusbarrage");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int spineCount = perks == null
                ? DEFAULT_SPINE_COUNT
                : Math.max(4, Math.min(32, perks.getInt("spine-count", DEFAULT_SPINE_COUNT)));
        double spread = perks == null
                ? DEFAULT_SPREAD
                : Math.max(0.1, perks.getDouble("spread", DEFAULT_SPREAD));
        double speed = perks == null ? DEFAULT_SPEED : Math.max(0.2, perks.getDouble("speed", DEFAULT_SPEED));
        int stickTicks = perks == null
                ? DEFAULT_STICK_TICKS
                : Math.max(5, perks.getInt("stick-ticks", DEFAULT_STICK_TICKS));
        double bleedDamage = perks == null
                ? DEFAULT_BLEED_DAMAGE
                : Math.max(0.0, perks.getDouble("bleed-damage", DEFAULT_BLEED_DAMAGE));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("cactusbarrage");
        double impactDamage = damageCfg.enabled() ? Math.max(0.0, damageCfg.value()) : 0.0;
        PotionEffectType slowType = PotionTypes.resolve("SLOWNESS", "SLOW");

        List<Player> targets = findNearby(world, origin, killer, victimId, 14.0, session);
        List<Spine> spines = new ArrayList<>(spineCount);
        List<ItemDisplay> displays = new ArrayList<>(spineCount);

        for (int i = 0; i < spineCount; i++) {
            Vector dir;
            if (!targets.isEmpty() && i < targets.size() * 2) {
                Player aim = targets.get(i % targets.size());
                Location aimAt = aim.getLocation().clone().add(0, 1.0, 0);
                dir = aimAt.toVector().subtract(origin.toVector());
                if (dir.lengthSquared() < 1.0e-4) {
                    dir = randomCone(i, spread);
                } else {
                    dir.normalize();
                    dir.add(new Vector(
                                    (Math.random() - 0.5) * spread,
                                    (Math.random() - 0.2) * spread * 0.5,
                                    (Math.random() - 0.5) * spread))
                            .normalize();
                }
            } else {
                dir = randomCone(i, spread);
            }

            ItemDisplay display = CactusBarrageDisplays.spawn(origin);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            displays.add(display);
            spines.add(new Spine(display, origin.clone(), dir, i, i % 4));
        }

        if (spines.isEmpty()) {
            return;
        }

        session.onCleanup(() -> CactusBarrageDisplays.removeAll(displays));
        visuals.sound("BLOCK_BAMBOO_HIT", origin, 1.0f, 0.8f);
        visuals.sound("ENTITY_ARROW_SHOOT", origin, 0.85f, 0.7f);
        visuals.dust(origin, CACTUS, 1.2f, 10, 0.3, 0.2, 0.3, 0.0);
        session.resetDeadline(MAX_TICKS + 20L);

        AtomicInteger tick = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= MAX_TICKS || spines.stream().allMatch(s -> s.done)) {
                CactusBarrageDisplays.removeAll(displays);
                return false;
            }

            for (Spine spine : spines) {
                updateSpine(
                        session,
                        visuals,
                        world,
                        killer,
                        victimId,
                        spine,
                        speed,
                        stickTicks,
                        impactDamage,
                        bleedDamage,
                        slowType,
                        current);
            }
            return true;
        });
    }

    private static Vector randomCone(int index, double spread) {
        double angle = (Math.PI * 2.0 * index) / 16.0 + Math.random() * spread;
        double elev = 0.05 + Math.random() * 0.35;
        return new Vector(Math.cos(angle), elev, Math.sin(angle)).normalize();
    }

    private static void updateSpine(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Spine spine,
            double speed,
            int stickTicks,
            double impactDamage,
            double bleedDamage,
            PotionEffectType slowType,
            int globalTick) {
        if (spine.done) {
            return;
        }
        if (globalTick < spine.launchDelay) {
            return;
        }

        if (spine.stuck) {
            spine.stickAge++;
            Player stuckPlayer = spine.stuckTarget == null
                    ? null
                    : org.bukkit.Bukkit.getPlayer(spine.stuckTarget);
            if (stuckPlayer != null && stuckPlayer.isOnline() && !stuckPlayer.isDead()) {
                Location base = stuckPlayer.getLocation().clone();
                if (spine.stickOffset == null) {
                    spine.stickOffset = new Vector(
                            (Math.random() - 0.5) * 0.45,
                            0.7 + Math.random() * 0.55,
                            (Math.random() - 0.5) * 0.45);
                }
                spine.loc = base.add(spine.stickOffset);
            }
            Location stickAt = spine.loc.clone().add(0.0, Math.sin(spine.stickAge * 0.3) * 0.02, 0.0);
            CactusBarrageDisplays.place(
                    spine.display, stickAt, spine.yaw, spine.pitch, spine.stickAge * 0.05f, 0.38f, 0.55f);
            if (spine.stickAge % 5 == 0) {
                visuals.dust(stickAt, LEAF, 0.7f, 1, 0.02, 0.04, 0.02, 0.0);
            }
            if (spine.stickAge >= stickTicks) {
                burst(visuals, spine.loc);
                if (spine.stuckTarget != null && killer != null && bleedDamage > 0.0) {
                    Player target = org.bukkit.Bukkit.getPlayer(spine.stuckTarget);
                    if (target != null
                            && target.isOnline()
                            && !target.isDead()
                            && session.allowsWorldMutation(killer, target.getLocation())) {
                        target.damage(bleedDamage, killer);
                    }
                }
                CactusBarrageDisplays.remove(spine.display);
                spine.done = true;
            }
            return;
        }

        spine.age++;
        spine.loc.add(spine.velocity.clone().multiply(speed));
        spine.velocity.setY(spine.velocity.getY() - 0.012);

        spine.yaw = (float) Math.atan2(-spine.velocity.getX(), spine.velocity.getZ());
        spine.pitch = (float) (-Math.asin(Math.max(-1.0, Math.min(1.0, spine.velocity.clone().normalize().getY()))));
        CactusBarrageDisplays.place(spine.display, spine.loc, spine.yaw, spine.pitch, spine.age * 0.2f, 0.35f, 0.6f);

        if (spine.age % 2 == 0) {
            visuals.dust(spine.loc, spine.index % 2 == 0 ? CACTUS : LEAF, 0.75f, 1, 0, 0, 0, 0);
        }

        Player hit = findHit(world, spine.loc, killer, victimId, session, spine.hitIds);
        if (hit != null) {
            stickTo(session, visuals, killer, hit, spine, impactDamage, slowType);
            return;
        }

        if (spine.age >= MAX_FLIGHT || spine.loc.getY() < spine.startY - 8) {
            burst(visuals, spine.loc);
            CactusBarrageDisplays.remove(spine.display);
            spine.done = true;
        }
    }

    private static void stickTo(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Player hit,
            Spine spine,
            double impactDamage,
            PotionEffectType slowType) {
        spine.stuck = true;
        spine.stuckTarget = hit.getUniqueId();
        spine.hitIds.add(hit.getUniqueId());
        spine.stickOffset = new Vector(
                (Math.random() - 0.5) * 0.45,
                0.7 + Math.random() * 0.55,
                (Math.random() - 0.5) * 0.45);
        spine.loc = hit.getLocation().clone().add(spine.stickOffset);

        visuals.particle("BLOCK", spine.loc, 8, 0.1, 0.1, 0.1, 0.02, null, Material.CACTUS.createBlockData());
        visuals.dust(spine.loc, BURST, 1.1f, 5, 0.12, 0.12, 0.12, 0.0);
        visuals.sound("BLOCK_WET_GRASS_PLACE", spine.loc, 0.7f, 0.7f);
        visuals.sound("ENTITY_PLAYER_HURT", spine.loc, 0.45f, 1.2f);

        if (killer != null && impactDamage > 0.0 && session.allowsWorldMutation(killer, hit.getLocation())) {
            hit.damage(impactDamage, killer);
        }
        if (slowType != null) {
            hit.addPotionEffect(new PotionEffect(slowType, 30, 0, true, true, true));
        }
    }

    private static void burst(VisualEffectService visuals, Location at) {
        visuals.dust(at, BURST, 1.35f, 10, 0.25, 0.25, 0.25, 0.0);
        visuals.dust(at, LEAF, 1.0f, 6, 0.2, 0.2, 0.2, 0.0);
        visuals.particle("HAPPY_VILLAGER", at, 6, 0.2, 0.2, 0.2, 0.0, null);
        visuals.sound("BLOCK_GRASS_BREAK", at, 0.55f, 1.3f);
    }

    private static Player findHit(
            World world,
            Location loc,
            Player killer,
            UUID victimId,
            EffectSession session,
            Set<UUID> already) {
        for (Player player : world.getPlayers()) {
            if (killer != null && player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (already.contains(player.getUniqueId())) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().clone().add(0, 1, 0).distanceSquared(loc) > HIT_DISTANCE * HIT_DISTANCE) {
                continue;
            }
            if (killer != null && !session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            return player;
        }
        return null;
    }

    private static List<Player> findNearby(
            World world,
            Location origin,
            Player killer,
            UUID victimId,
            double radius,
            EffectSession session) {
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

    private static final class Spine {
        private final ItemDisplay display;
        private final Vector velocity;
        private final int index;
        private final int launchDelay;
        private final Set<UUID> hitIds = new HashSet<>();
        private final double startY;
        private Location loc;
        private int age;
        private int stickAge;
        private boolean stuck;
        private boolean done;
        private UUID stuckTarget;
        private Vector stickOffset;
        private float yaw;
        private float pitch;

        private Spine(ItemDisplay display, Location start, Vector dir, int index, int launchDelay) {
            this.display = display;
            this.loc = start.clone();
            this.velocity = dir.clone();
            this.index = index;
            this.launchDelay = launchDelay;
            this.startY = start.getY();
        }
    }
}
