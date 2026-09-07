package com.monkey.ktplus.effects.list.quillstorm.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.quillstorm.animation.util.QuillStormDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class QuillStormAnimation {
    private static final int MAX_TICKS = 150;
    private static final int DEFAULT_COUNT = 24;
    private static final double DEFAULT_SPEED = 0.7;
    private static final int DEFAULT_STICK = 30;
    private static final double DEFAULT_DAMAGE = 2.0;
    private static final double HIT_DISTANCE = 1.05;
    private static final double OUT_RANGE = 8.5;

    private static final Color QUILL = Color.fromRGB(210, 200, 180);
    private static final Color TIP = Color.fromRGB(90, 70, 50);
    private static final Color GLOW = Color.fromRGB(240, 230, 160);

    private static final String[] AMBIENT_SOUNDS = {
        "ENTITY_ARROW_SHOOT",
        "ENTITY_ARROW_HIT",
        "ENTITY_ARROW_HIT_PLAYER",
        "ITEM_CROSSBOW_SHOOT"
    };

    private QuillStormAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.1, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("quillstorm");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int quillCount = perks == null
                ? DEFAULT_COUNT
                : Math.max(8, Math.min(36, perks.getInt("quill-count", DEFAULT_COUNT)));
        double speed = perks == null ? DEFAULT_SPEED : Math.max(0.2, perks.getDouble("speed", DEFAULT_SPEED));
        int stickTicks = perks == null
                ? DEFAULT_STICK
                : Math.max(5, perks.getInt("stick-ticks", DEFAULT_STICK));
        double damageBase = perks == null
                ? DEFAULT_DAMAGE
                : Math.max(0.0, perks.getDouble("damage", DEFAULT_DAMAGE));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("quillstorm");
        final double damage = damageCfg.enabled()
                ? Math.max(damageBase, damageCfg.value())
                : damageBase;

        List<Quill> quills = new ArrayList<>(quillCount);
        List<ItemDisplay> displays = new ArrayList<>(quillCount);
        for (int i = 0; i < quillCount; i++) {
            double yaw = (Math.PI * 2.0 * i) / quillCount;
            double pitch = -0.15 + (i % 5) * 0.08;
            Vector dir = new Vector(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch))
                    .normalize();
            ItemDisplay display = QuillStormDisplays.spawn(origin, QuillStormDisplays.quillMaterial(i));
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            displays.add(display);
            quills.add(new Quill(display, origin.clone(), dir, i, i % 3));
        }

        if (quills.isEmpty()) {
            return;
        }

        session.onCleanup(() -> {
            stopAmbient(origin);
            QuillStormDisplays.removeAll(displays);
        });

        visuals.sound("ITEM_CROSSBOW_SHOOT", origin, 1.0f, 0.85f);
        visuals.sound("ENTITY_ARROW_SHOOT", origin, 0.8f, 1.2f);
        session.resetDeadline(MAX_TICKS + 20L);

        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        double finalDamage = damage;

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopAmbient(origin);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= MAX_TICKS || quills.stream().allMatch(q -> q.done)) {
                stopAmbient(origin);
                QuillStormDisplays.removeAll(displays);
                finished[0] = true;
                return false;
            }

            Location recallPoint = killer != null && killer.isOnline() && killer.getWorld().equals(world)
                    ? killer.getLocation().clone().add(0, 1.1, 0)
                    : origin.clone();

            for (Quill quill : quills) {
                updateQuill(
                        session,
                        visuals,
                        world,
                        killer,
                        victimId,
                        origin,
                        recallPoint,
                        quill,
                        speed,
                        stickTicks,
                        finalDamage,
                        current);
            }
            return true;
        });
    }

    private static void updateQuill(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location origin,
            Location recallPoint,
            Quill quill,
            double speed,
            int stickTicks,
            double damage,
            int globalTick) {
        if (quill.done) {
            return;
        }
        if (globalTick < quill.launchDelay) {
            QuillStormDisplays.place(quill.display, origin, globalTick * 0.2f, 0.4f, 0.0f, 0.35f, 0.55f);
            return;
        }

        if (quill.phase == Phase.OUT) {
            quill.loc.add(quill.dir.clone().multiply(speed));
            float yaw = (float) Math.atan2(-quill.dir.getX(), quill.dir.getZ());
            float pitch = (float) Math.asin(Math.max(-1.0, Math.min(1.0, -quill.dir.getY())));
            QuillStormDisplays.place(quill.display, quill.loc, yaw, pitch, quill.age * 0.05f, 0.4f, 0.75f);
            visuals.dust(quill.loc, quill.index % 2 == 0 ? QUILL : TIP, 0.75f, 1, 0, 0, 0, 0);

            tryHit(session, visuals, world, killer, victimId, quill, damage);

            quill.age++;
            if (quill.stuck || quill.loc.distanceSquared(origin) >= OUT_RANGE * OUT_RANGE || quill.age > 28) {
                quill.phase = Phase.STICK;
                quill.stickAge = 0;
                if (!quill.stuck) {
                    visuals.sound("ENTITY_ARROW_HIT", quill.loc, 0.45f, 1.3f);
                }
            }
            return;
        }

        if (quill.phase == Phase.STICK) {
            quill.stickAge++;
            Location bob = quill.loc.clone().add(0, Math.sin(quill.stickAge * 0.35) * 0.03, 0);
            QuillStormDisplays.place(quill.display, bob, quill.stickAge * 0.04f, 0.5f, 0.1f, 0.38f, 0.7f);
            if (quill.stickAge % 4 == 0) {
                visuals.dust(bob, GLOW, 0.7f, 1, 0.02, 0.02, 0.02, 0.0);
            }
            if (quill.stickAge >= stickTicks) {
                quill.phase = Phase.RECALL;
                quill.hitIds.clear();
                visuals.sound("ENTITY_ARROW_SHOOT", quill.loc, 0.35f, 1.6f);
            }
            return;
        }

        Vector to = recallPoint.toVector().subtract(quill.loc.toVector());
        double dist = to.length();
        if (dist <= 1.0) {
            visuals.dust(quill.loc, QUILL, 1.1f, 5, 0.15, 0.1, 0.15, 0.0);
            QuillStormDisplays.remove(quill.display);
            quill.done = true;
            return;
        }
        if (dist > 0.001) {
            quill.loc.add(to.normalize().multiply(Math.min(speed * 1.25, dist * 0.35 + 0.15)));
        }
        float yaw = (float) Math.atan2(-to.getX(), to.getZ());
        QuillStormDisplays.place(quill.display, quill.loc, yaw, 0.3f, globalTick * 0.2f, 0.35f, 0.65f);
        if (globalTick % 2 == 0) {
            visuals.dust(quill.loc, GLOW, 0.8f, 1, 0, 0, 0, 0);
        }
        tryHit(session, visuals, world, killer, victimId, quill, damage * 0.75);
    }

    private static void tryHit(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Quill quill,
            double damage) {
        if (killer == null || damage <= 0.0) {
            return;
        }
        double hitSq = HIT_DISTANCE * HIT_DISTANCE;
        for (Player player : world.getPlayers()) {
            if (player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            if (quill.hitIds.contains(player.getUniqueId())) {
                continue;
            }
            if (player.getLocation().clone().add(0, 1, 0).distanceSquared(quill.loc) > hitSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            quill.hitIds.add(player.getUniqueId());
            quill.stuck = true;
            player.damage(damage, killer);
            visuals.sound("ENTITY_ARROW_HIT_PLAYER", quill.loc, 0.7f, 1.1f);
            visuals.dust(player.getLocation().add(0, 1, 0), TIP, 1.2f, 6, 0.15, 0.2, 0.15, 0.0);
            visuals.particle("CLOUD", quill.loc, 3, 0.1, 0.1, 0.1, 0.01, null);
        }
    }

    private static void stopAmbient(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        double rangeSq = 64.0 * 64.0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) > rangeSq) {
                continue;
            }
            for (String sound : AMBIENT_SOUNDS) {
                EntityCompat.stopSound(player, sound);
            }
        }
    }

    private enum Phase {
        OUT,
        STICK,
        RECALL
    }

    private static final class Quill {
        private final ItemDisplay display;
        private final Location loc;
        private final Vector dir;
        private final int index;
        private final int launchDelay;
        private final Set<UUID> hitIds = new HashSet<>();
        private Phase phase = Phase.OUT;
        private int age;
        private int stickAge;
        private boolean stuck;
        private boolean done;

        private Quill(ItemDisplay display, Location loc, Vector dir, int index, int launchDelay) {
            this.display = display;
            this.loc = loc;
            this.dir = dir;
            this.index = index;
            this.launchDelay = launchDelay;
        }
    }
}
