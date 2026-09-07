package com.monkey.ktplus.effects.list.bubbletrap.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.bubbletrap.animation.util.BubbleTrapDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.item.PotionTypes;
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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class BubbleTrapAnimation {
    private static final int DEFAULT_BUBBLE_COUNT = 6;
    private static final int DEFAULT_TRAP_TICKS = 45;
    private static final int LOCK_TICKS = 5;
    private static final double DEFAULT_SEEK_RANGE = 14.0;
    private static final int DEFAULT_SLOW_AMP = 1;
    private static final double DEFAULT_POP_DAMAGE = 2.5;
    private static final double DEFAULT_MOVE_SPEED = 0.22;
    private static final int DEFAULT_DURATION = 160;
    private static final float TRAP_SCALE_MIN = 1.9f;
    private static final float TRAP_SCALE_SPAN = 0.4f;

    private static final Color WATER_DEEP = Color.fromRGB(30, 90, 160);
    private static final Color WATER_MID = Color.fromRGB(70, 170, 220);
    private static final Color WATER_FOAM = Color.fromRGB(200, 235, 255);

    private static final Material[] BUBBLE_MATS = {
        Material.GLASS,
        Material.HEART_OF_THE_SEA,
        Material.PRISMARINE_CRYSTALS,
        Material.BLUE_STAINED_GLASS,
        Material.LIGHT_BLUE_STAINED_GLASS
    };

    private BubbleTrapAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 1.0, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("bubbletrap");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int bubbleCount = perks == null
                ? DEFAULT_BUBBLE_COUNT
                : Math.max(4, Math.min(8, perks.getInt("bubble-count", DEFAULT_BUBBLE_COUNT)));
        int trapTicks = perks == null
                ? DEFAULT_TRAP_TICKS
                : Math.max(15, perks.getInt("trap-ticks", DEFAULT_TRAP_TICKS));
        double seekRange = perks == null
                ? DEFAULT_SEEK_RANGE
                : Math.max(4.0, perks.getDouble("seek-range", DEFAULT_SEEK_RANGE));
        int slowAmp = perks == null
                ? DEFAULT_SLOW_AMP
                : Math.max(0, perks.getInt("slow-amplifier", DEFAULT_SLOW_AMP));
        double popDamageBase = perks == null
                ? DEFAULT_POP_DAMAGE
                : Math.max(0.0, perks.getDouble("pop-damage", DEFAULT_POP_DAMAGE));
        double moveSpeed = perks == null
                ? DEFAULT_MOVE_SPEED
                : Math.max(0.08, perks.getDouble("move-speed", DEFAULT_MOVE_SPEED));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(60, perks.getInt("duration-ticks", DEFAULT_DURATION));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("bubbletrap");
        final double popDamage = damageCfg.enabled()
                ? Math.max(popDamageBase, damageCfg.value())
                : popDamageBase;
        final double damage = popDamage;
        PotionEffectType slowType = PotionTypes.resolve("SLOWNESS", "SLOW");

        List<Bubble> bubbles = new ArrayList<>(bubbleCount);
        for (int i = 0; i < bubbleCount; i++) {
            double angle = (Math.PI * 2.0 * i) / bubbleCount;
            Location spawn = origin.clone().add(Math.cos(angle) * 1.4, 0.4 + (i % 3) * 0.35, Math.sin(angle) * 1.4);
            Material mat = BUBBLE_MATS[i % BUBBLE_MATS.length];
            float scale = TRAP_SCALE_MIN + (i % 3) * (TRAP_SCALE_SPAN / 2.0f);
            ItemDisplay display = BubbleTrapDisplays.spawnBubble(spawn, mat, scale * 0.35f);
            if (display == null) {
                continue;
            }
            session.trackEntity(display);
            bubbles.add(new Bubble(
                    display,
                    spawn,
                    i,
                    i * 3,
                    scale,
                    0.35 + (i % 4) * 0.12,
                    (Math.PI * 2.0 * i) / bubbleCount,
                    mat));
        }

        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};

        session.onCleanup(() -> {
            for (Bubble bubble : bubbles) {
                BubbleTrapDisplays.remove(bubble.display);
            }
            PerkActionBar.clear(killer);
        });

        visuals.sound("ENTITY_PLAYER_SPLASH_HIGH_SPEED", origin, 1.1f, 1.2f);
        visuals.sound("BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT", origin, 0.7f, 1.1f);
        session.resetDeadline(durationTicks + 40L);

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks || bubbles.isEmpty()) {
                for (Bubble bubble : new ArrayList<>(bubbles)) {
                    popBubble(session, visuals, killer, victimId, bubble, damage, true);
                }
                bubbles.clear();
                finished[0] = true;
                PerkActionBar.clear(killer);
                return false;
            }

            bubbles.removeIf(bubble -> updateBubble(
                    session,
                    visuals,
                    world,
                    killer,
                    victimId,
                    bubble,
                    seekRange,
                    moveSpeed,
                    trapTicks,
                    slowAmp,
                    slowType,
                    damage,
                    current));

            if (current % 10 == 0) {
                visuals.sound("BLOCK_BUBBLE_COLUMN_BUBBLE_POP", origin, 0.4f, 0.9f + (float) Math.random() * 0.4f);
                visuals.sound("ENTITY_DOLPHIN_SPLASH", origin, 0.2f, 1.4f);
            }

            long trapping = bubbles.stream()
                    .filter(b -> b.state == BubbleState.LOCK || b.state == BubbleState.LIFT)
                    .count();
            if (killer != null && killer.isOnline() && current % 4 == 0) {
                PerkActionBar.show(
                        killer,
                        String.format(
                                "&b🫧 BUBBLETRAP &8| &3ACTIVE &f%d &8| &9TRAP &f%d",
                                bubbles.size(),
                                trapping));
            }
            return !bubbles.isEmpty();
        });
    }

    private static boolean updateBubble(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Bubble bubble,
            double seekRange,
            double moveSpeed,
            int trapTicks,
            int slowAmp,
            PotionEffectType slowType,
            double popDamage,
            int globalTick) {
        if (bubble.done) {
            return true;
        }
        if (bubble.age < bubble.launchDelay) {
            wobblePlace(bubble, globalTick);
            drawBubbleAura(visuals, bubble.loc, bubble.age);
            bubble.age++;
            return false;
        }

        if (bubble.state == BubbleState.SEEK) {
            Player target = resolveTarget(world, bubble, killer, victimId, seekRange, session);
            if (target != null) {
                Location aim = target.getLocation().clone().add(0.0, 1.05, 0.0);
                Vector to = aim.toVector().subtract(bubble.loc.toVector());
                double dist = to.length();
                if (dist < 1.15) {
                    bubble.state = BubbleState.LOCK;
                    bubble.lockLeft = LOCK_TICKS;
                    bubble.trapLeft = Math.max(20, trapTicks - LOCK_TICKS);
                    bubble.targetId = target.getUniqueId();
                    bubble.loc = aim.clone();
                    bubble.displayScale = bubble.baseScale * 0.35f;
                    visuals.sound("ENTITY_PLAYER_SPLASH", bubble.loc, 0.9f, 1.35f);
                    visuals.sound("BLOCK_GLASS_PLACE", bubble.loc, 0.55f, 1.5f);
                } else if (dist > 0.001) {
                    double speed = moveSpeed * bubble.speedMul;
                    Vector step = to.normalize().multiply(Math.min(speed, dist * 0.28));
                    step.setY(step.getY() + Math.sin(bubble.age * 0.21 + bubble.phase) * 0.04);
                    Vector side = step.clone().crossProduct(new Vector(0, 1, 0));
                    if (side.lengthSquared() > 1.0e-6) {
                        side.normalize().multiply(Math.sin(bubble.age * 0.19 + bubble.phase) * 0.06);
                        step.add(side);
                    }
                    bubble.loc.add(step);
                }
            } else {
                double drift = 0.06 + bubble.index * 0.004;
                bubble.loc.add(
                        Math.cos(bubble.age * 0.07 + bubble.phase) * drift,
                        Math.sin(bubble.age * 0.11 + bubble.phase) * 0.03,
                        Math.sin(bubble.age * 0.07 + bubble.phase) * drift);
            }
            wobblePlace(bubble, globalTick);
        } else if (bubble.state == BubbleState.LOCK || bubble.state == BubbleState.LIFT) {
            Player trapped = bubble.targetId == null
                    ? null
                    : world.getPlayers().stream()
                            .filter(p -> p.getUniqueId().equals(bubble.targetId))
                            .findFirst()
                            .orElse(null);
            if (trapped == null || !trapped.isOnline() || trapped.isDead() || !trapped.getWorld().equals(world)) {
                popBubble(session, visuals, killer, victimId, bubble, popDamage, false);
                return true;
            }
            if (bubble.state == BubbleState.LOCK) {
                bubble.loc = trapped.getLocation().clone().add(0.0, 1.05, 0.0);
                float growT = 1.0f - (bubble.lockLeft / (float) LOCK_TICKS);
                bubble.displayScale = bubble.baseScale * (0.35f + 0.65f * clamp01(growT));
                trapped.setVelocity(trapped.getVelocity().multiply(0.08));
                teleportIntoBubble(trapped, bubble.loc);
                bubble.lockLeft--;
                if (bubble.lockLeft <= 0) {
                    bubble.state = BubbleState.LIFT;
                    bubble.displayScale = bubble.baseScale;
                }
            } else {
                bubble.loc.add(0.0, 0.12, 0.0);
                teleportIntoBubble(trapped, bubble.loc);
                Vector towardCenter = bubble.loc
                        .toVector()
                        .subtract(trapped.getLocation().toVector().add(new Vector(0.0, 1.05, 0.0)));
                Vector vel = towardCenter.multiply(0.65);
                if (vel.lengthSquared() > 0.81) {
                    vel.normalize().multiply(0.9);
                }
                trapped.setVelocity(vel);
                bubble.trapLeft--;
                if (bubble.trapLeft <= 0) {
                    popBubble(session, visuals, killer, victimId, bubble, popDamage, true);
                    return true;
                }
            }
            if (slowType != null
                    && killer != null
                    && session.allowsWorldMutation(killer, trapped.getLocation())) {
                trapped.addPotionEffect(new PotionEffect(slowType, 30, slowAmp, true, true, true));
            }
            placeTrapped(bubble, globalTick);
        }

        drawBubbleAura(visuals, bubble.loc, bubble.age);
        if (bubble.age % 6 == bubble.index % 6) {
            visuals.particle("BUBBLE_POP", bubble.loc, 2, 0.2, 0.2, 0.2, 0.0, null);
            visuals.particle("BUBBLE_COLUMN_UP", bubble.loc, ParticleScale.scale(3), 0.15, 0.2, 0.15, 0.01, null);
        }
        bubble.age++;
        return false;
    }

    private static void teleportIntoBubble(Player player, Location bubbleCenter) {
        Location feet = bubbleCenter.clone().subtract(0.0, 1.05, 0.0);
        feet.setYaw(player.getLocation().getYaw());
        feet.setPitch(player.getLocation().getPitch());
        player.teleport(feet);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static void wobblePlace(Bubble bubble, int tick) {
        float wobbleYaw = (float) (tick * 0.05 + bubble.phase);
        float wobblePitch = (float) (Math.sin(tick * 0.18 + bubble.phase) * 0.35);
        float wobbleRoll = (float) (Math.cos(tick * 0.14 + bubble.phase) * 0.4);
        float scalePulse = bubble.baseScale * 0.35f * (0.92f + (float) Math.sin(tick * 0.22 + bubble.phase) * 0.12f);
        Location at = bubble.loc.clone().add(0.0, Math.sin(tick * 0.16 + bubble.phase) * 0.12, 0.0);
        BubbleTrapDisplays.place(bubble.display, at, scalePulse, wobbleYaw, wobblePitch, wobbleRoll);
    }

    private static void placeTrapped(Bubble bubble, int tick) {
        float wobbleYaw = (float) (tick * 0.04 + bubble.phase);
        float wobblePitch = (float) (Math.sin(tick * 0.12 + bubble.phase) * 0.12);
        float wobbleRoll = (float) (Math.cos(tick * 0.1 + bubble.phase) * 0.12);
        float scale = bubble.displayScale * (0.98f + (float) Math.sin(tick * 0.18 + bubble.phase) * 0.04f);
        BubbleTrapDisplays.place(bubble.display, bubble.loc, scale, wobbleYaw, wobblePitch, wobbleRoll);
    }

    private static void drawBubbleAura(VisualEffectService visuals, Location at, int age) {
        visuals.dust(at, WATER_MID, 1.05f, ParticleScale.scale(2), 0.25, 0.25, 0.25, 0.0);
        if (age % 2 == 0) {
            visuals.dust(at.clone().add(0, 0.3, 0), WATER_FOAM, 0.8f, 1, 0.15, 0.15, 0.15, 0.0);
        }
        if (age % 3 == 0) {
            visuals.dust(at, WATER_DEEP, 0.9f, 1, 0.3, 0.2, 0.3, 0.0);
        }
    }

    private static void popBubble(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            UUID victimId,
            Bubble bubble,
            double popDamage,
            boolean dealDamage) {
        if (bubble.done) {
            return;
        }
        bubble.done = true;
        Location at = bubble.loc.clone();
        visuals.sound("BLOCK_BUBBLE_COLUMN_BUBBLE_POP", at, 1.2f, 0.85f);
        visuals.sound("ENTITY_PLAYER_SPLASH_HIGH_SPEED", at, 0.7f, 1.4f);
        visuals.sound("BLOCK_GLASS_BREAK", at, 0.55f, 1.35f);
        visuals.particle("SPLASH", at, ParticleScale.scale(24), 0.55, 0.45, 0.55, 0.08, null);
        visuals.particle("BUBBLE_POP", at, ParticleScale.scale(12), 0.4, 0.4, 0.4, 0.0, null);
        visuals.dust(at, WATER_FOAM, 1.4f, ParticleScale.scale(10), 0.5, 0.4, 0.5, 0.0);
        visuals.dust(at, WATER_MID, 1.1f, ParticleScale.scale(8), 0.6, 0.45, 0.6, 0.0);
        BubbleTrapDisplays.remove(bubble.display);

        if (!dealDamage || killer == null || popDamage <= 0.0 || at.getWorld() == null) {
            return;
        }
        double radius = 2.4;
        double radiusSq = radius * radius;
        for (Player player : at.getWorld().getPlayers()) {
            if (player.getUniqueId().equals(killer.getUniqueId())) {
                continue;
            }
            if (victimId != null && player.getUniqueId().equals(victimId)) {
                continue;
            }
            if (!player.isOnline() || player.isDead() || !player.getWorld().equals(at.getWorld())) {
                continue;
            }
            if (player.getLocation().distanceSquared(at) > radiusSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            player.damage(popDamage, killer);
            Vector knock = player.getLocation().toVector().subtract(at.toVector());
            if (knock.lengthSquared() > 1.0e-4) {
                player.setVelocity(player.getVelocity().add(knock.normalize().multiply(0.35).setY(0.25)));
            }
        }
    }

    private static Player resolveTarget(
            World world,
            Bubble bubble,
            Player killer,
            UUID victimId,
            double seekRange,
            EffectSession session) {
        if (bubble.targetId != null) {
            Player locked = world.getPlayers().stream()
                    .filter(p -> p.getUniqueId().equals(bubble.targetId))
                    .findFirst()
                    .orElse(null);
            if (locked != null
                    && locked.isOnline()
                    && !locked.isDead()
                    && locked.getWorld().equals(world)
                    && locked.getLocation().distanceSquared(bubble.loc) <= seekRange * seekRange) {
                return locked;
            }
        }
        List<Player> nearby = findNearby(world, bubble.loc, killer, victimId, seekRange, session);
        if (nearby.isEmpty()) {
            return null;
        }
        Player next = nearby.get(bubble.index % nearby.size());
        bubble.targetId = next.getUniqueId();
        return next;
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

    private enum BubbleState {
        SEEK,
        LOCK,
        LIFT
    }

    private static final class Bubble {
        private final ItemDisplay display;
        private Location loc;
        private final int index;
        private final int launchDelay;
        private final float baseScale;
        private float displayScale;
        private final double speedMul;
        private final double phase;
        private final Material material;
        private BubbleState state = BubbleState.SEEK;
        private UUID targetId;
        private int trapLeft;
        private int lockLeft;
        private int age;
        private boolean done;

        private Bubble(
                ItemDisplay display,
                Location loc,
                int index,
                int launchDelay,
                float baseScale,
                double speedMul,
                double phase,
                Material material) {
            this.display = display;
            this.loc = loc.clone();
            this.index = index;
            this.launchDelay = launchDelay;
            this.baseScale = baseScale;
            this.displayScale = baseScale * 0.35f;
            this.speedMul = speedMul;
            this.phase = phase;
            this.material = material;
        }
    }
}
