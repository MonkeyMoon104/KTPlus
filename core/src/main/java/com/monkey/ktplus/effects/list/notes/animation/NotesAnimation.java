package com.monkey.ktplus.effects.list.notes.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.notes.animation.util.NotesDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
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
import org.bukkit.util.Vector;

public final class NotesAnimation {
    private static final int DEFAULT_CHARGE_TICKS = 70;
    private static final int DEFAULT_DISC_COUNT = 10;
    private static final int DEFAULT_DISC_INTERVAL = 5;
    private static final int TRANSFORM_TICKS = 12;
    private static final int JUKEBOX_AFTER_LAST_DISC_TICKS = 2;
    private static final int AERIAL_MAX_AGE = 55;
    private static final int ATTACK_MAX_AGE = 70;
    private static final double DEFAULT_RANGE = 12.0;
    private static final double DEFAULT_SPEED = 0.62;
    private static final double HIT_DISTANCE = 1.2;
    private static final double GRAVITY = 0.045;
    private static final double START_HEIGHT = 4.6;

    private static final Material[] DISCS = {
        Material.MUSIC_DISC_13,
        Material.MUSIC_DISC_CAT,
        Material.MUSIC_DISC_BLOCKS,
        Material.MUSIC_DISC_CHIRP,
        Material.MUSIC_DISC_FAR,
        Material.MUSIC_DISC_MALL,
        Material.MUSIC_DISC_MELLOHI,
        Material.MUSIC_DISC_STAL,
        Material.MUSIC_DISC_STRAD,
        Material.MUSIC_DISC_WARD,
        Material.MUSIC_DISC_WAIT,
        Material.MUSIC_DISC_OTHERSIDE,
        Material.MUSIC_DISC_PIGSTEP,
        Material.MUSIC_DISC_RELIC
    };

    private static final Color[] NOTE_COLORS = {
        Color.fromRGB(255, 85, 85),
        Color.fromRGB(255, 170, 0),
        Color.fromRGB(255, 255, 85),
        Color.fromRGB(85, 255, 85),
        Color.fromRGB(85, 255, 255),
        Color.fromRGB(85, 85, 255),
        Color.fromRGB(255, 85, 255)
    };

    private NotesAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location ground = context.location().clone().add(0.5, 0.05, 0.5);
        World world = ground.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("notes");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        int chargeTicks = perks == null
                ? DEFAULT_CHARGE_TICKS
                : Math.max(20, perks.getInt("charge-ticks", DEFAULT_CHARGE_TICKS));
        int discCount = perks == null
                ? DEFAULT_DISC_COUNT
                : Math.max(1, perks.getInt("disc-count", DEFAULT_DISC_COUNT));
        int discInterval = perks == null
                ? DEFAULT_DISC_INTERVAL
                : Math.max(2, perks.getInt("disc-interval-ticks", DEFAULT_DISC_INTERVAL));
        double range = perks == null ? DEFAULT_RANGE : Math.max(2.0, perks.getDouble("range", DEFAULT_RANGE));
        double speed = perks == null ? DEFAULT_SPEED : Math.max(0.2, perks.getDouble("speed", DEFAULT_SPEED));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damage = context.config().effectDamage("notes");
        double damagePerDisc = damage.enabled() ? Math.max(0.0, damage.value()) : 0.0;

        Location skySpawn = ground.clone().add(0.0, START_HEIGHT, 0.0);
        ItemDisplay block = NotesDisplays.spawnNoteBlock(skySpawn);
        if (block == null) {
            return;
        }
        session.trackEntity(block);
        NotesDisplays.placeBlock(block, skySpawn, 0.55f, 0.0f);

        List<ItemDisplay> discDisplays = new ArrayList<>();
        List<NoteOrb> orbs = new ArrayList<>();
        List<DiscShot> shots = new ArrayList<>();

        session.onCleanup(() -> {
            NotesDisplays.remove(block);
            NotesDisplays.removeAll(discDisplays);
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_AMETHYST_BLOCK_RESONATE", skySpawn, 0.9f, 1.15f);
        visuals.sound("BLOCK_ENCHANTMENT_TABLE_USE", skySpawn, 0.55f, 1.4f);

        AtomicInteger tick = new AtomicInteger();
        AtomicInteger discsFired = new AtomicInteger();
        boolean[] transformed = {false};
        boolean[] blockGone = {false};
        Integer[] removeBlockAt = {null};
        float[] yaw = {0.0f};

        int fireStart = chargeTicks + TRANSFORM_TICKS;
        int lastDiscTick = fireStart + Math.max(0, discCount - 1) * discInterval;
        int total = lastDiscTick + ATTACK_MAX_AGE + 10;

        session.runTimer(0L, 1L, () -> {
            if (!session.active()) {
                PerkActionBar.clear(killer);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= total && shots.isEmpty()) {
                NotesDisplays.remove(block);
                NotesDisplays.removeAll(discDisplays);
                PerkActionBar.clear(killer);
                return false;
            }

            yaw[0] += 0.08f;
            int targets = findNearby(world, ground, killer, victimId, range, session).size();

            if (current < chargeTicks) {
                double progress = (current + 1) / (double) chargeTicks;
                updateCharge(visuals, ground, block, orbs, yaw[0], progress, current);
                if (current % 3 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&d♪ NOTES &8| &fCHARGE &a%d%% &8| &7DESCENDING &8| &bRANGE &f%.0f",
                                    (int) (progress * 100),
                                    range));
                }
                return true;
            }

            if (!transformed[0]) {
                transformed[0] = true;
                transformToJukebox(visuals, ground, block);
            }

            int transformLocal = current - chargeTicks;
            if (transformLocal < TRANSFORM_TICKS) {
                if (!blockGone[0]) {
                    float punch =
                            1.0f + (float) Math.sin((transformLocal / (double) TRANSFORM_TICKS) * Math.PI) * 0.35f;
                    NotesDisplays.placeBlock(block, ground, punch, yaw[0]);
                    if (current % 3 == 0) {
                        for (int n = 0; n < 3; n++) {
                            spawnNote(
                                    visuals,
                                    ground.clone()
                                            .add(
                                                    (Math.random() - 0.5) * 0.8,
                                                    0.5 + Math.random() * 0.6,
                                                    (Math.random() - 0.5) * 0.8),
                                    ((current + n * 3) % 24) / 24.0f);
                        }
                    }
                }
                if (current % 3 == 0) {
                    PerkActionBar.show(
                            killer,
                            String.format("&d♪ NOTES &8| &6JUKEBOX &fTRANSFORM &8| &bTARGETS &f%d", targets));
                }
                return true;
            }

            int fireLocal = current - fireStart;
            if (!blockGone[0]
                    && fireLocal >= 0
                    && discsFired.get() < discCount
                    && fireLocal % discInterval == 0) {
                int index = discsFired.getAndIncrement();
                launchDisc(
                        session,
                        visuals,
                        world,
                        ground,
                        killer,
                        victimId,
                        range,
                        speed,
                        index,
                        discDisplays,
                        shots);
                if (discsFired.get() >= discCount) {
                    removeBlockAt[0] = current + JUKEBOX_AFTER_LAST_DISC_TICKS;
                }
            }

            if (!blockGone[0] && removeBlockAt[0] != null && current >= removeBlockAt[0]) {
                visuals.sound("BLOCK_WOOD_BREAK", ground, 0.7f, 1.1f);
                visuals.particle("CLOUD", ground.clone().add(0, 0.6, 0), 8, 0.25, 0.2, 0.25, 0.02, null);
                NotesDisplays.remove(block);
                blockGone[0] = true;
            } else if (!blockGone[0]) {
                NotesDisplays.placeBlock(block, ground, 1.0f, yaw[0]);
            }

            shots.removeIf(shot -> updateDisc(
                    session, visuals, world, killer, victimId, ground, shot, range, speed, damagePerDisc));

            if (current % 3 == 0) {
                if (discsFired.get() < discCount) {
                    PerkActionBar.show(
                            killer,
                            String.format(
                                    "&d♪ NOTES &8| &eDISCS &f%d&8/&f%d &8| &bTARGETS &f%d &8| &7%s",
                                    discsFired.get(),
                                    discCount,
                                    targets,
                                    targets > 0 ? "ATTACK" : "AERIAL"));
                } else if (!shots.isEmpty()) {
                    PerkActionBar.show(
                            killer,
                            String.format("&d♪ NOTES &8| &eIN FLIGHT &f%d &8| &bTARGETS &f%d", shots.size(), targets));
                } else {
                    PerkActionBar.clear(killer);
                }
            }

            if (current >= total && shots.isEmpty()) {
                NotesDisplays.remove(block);
                NotesDisplays.removeAll(discDisplays);
                PerkActionBar.clear(killer);
                return false;
            }
            if (discsFired.get() >= discCount && shots.isEmpty() && blockGone[0]) {
                PerkActionBar.clear(killer);
                return false;
            }
            return true;
        });
    }

    private static void updateCharge(
            VisualEffectService visuals,
            Location ground,
            ItemDisplay block,
            List<NoteOrb> orbs,
            float yaw,
            double progress,
            int tick) {
        
        double remain = 1.0 - progress;
        double height = START_HEIGHT * remain * remain;
        Location blockAt = ground.clone().add(0.0, height, 0.0);
        float scale = 0.55f + (float) progress * 0.55f;
        NotesDisplays.placeBlock(block, blockAt, scale, yaw);

        if (tick % 14 == 0) {
            visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", blockAt, 0.35f, 1.2f + (float) progress * 0.35f);
        }
        if (tick % 22 == 0) {
            visuals.sound("BLOCK_BEACON_AMBIENT", blockAt, 0.22f, 1.55f);
        }

        Location absorbPoint = blockAt.clone().add(0.0, 0.55, 0.0);

        if (tick % 5 == 0 && orbs.size() < 10) {
            int spawn = progress < 0.5 ? 1 : 2;
            for (int i = 0; i < spawn; i++) {
                double angle = ((tick * 0.51) + i * 2.399963) % (Math.PI * 2.0);
                angle += (Math.random() - 0.5) * 0.4;
                double dist = 4.2 + Math.random() * 2.8;
                Location start = ground.clone().add(
                        Math.cos(angle) * dist,
                        0.6 + Math.random() * (height + 1.8),
                        Math.sin(angle) * dist);
                
                float hue = ((tick + i * 5) % 24) / 24.0f;
                NoteOrb orb = new NoteOrb(start, hue);
                spawnNote(visuals, orb.loc, orb.hue);
                orbs.add(orb);
            }
        }

        Iterator<NoteOrb> it = orbs.iterator();
        while (it.hasNext()) {
            NoteOrb orb = it.next();
            orb.age++;
            Vector delta = absorbPoint.toVector().subtract(orb.loc.toVector());
            double dist = delta.length();
            if (dist < 0.45) {
                visuals.sound(
                        "ENTITY_EXPERIENCE_ORB_PICKUP",
                        absorbPoint,
                        0.18f,
                        1.45f + orb.hue * 0.4f);
                spawnNote(visuals, absorbPoint, orb.hue);
                it.remove();
                continue;
            }
            
            double step = Math.min(0.32, 0.1 + dist * 0.06);
            orb.loc.add(delta.normalize().multiply(step));
            if (orb.age % 7 == 0) {
                spawnNote(visuals, orb.loc, orb.hue);
            }
        }
    }

    private static void spawnNote(VisualEffectService visuals, Location at, float hue) {
        float clamped = Math.max(0.0f, Math.min(0.999f, hue));
        visuals.particle("NOTE", at, 0, clamped, 0.0, 0.0, 1.0, null);
    }

    private static void transformToJukebox(VisualEffectService visuals, Location ground, ItemDisplay block) {
        visuals.sound("ENTITY_GENERIC_EXPLODE", ground, 1.0f, 1.4f);
        visuals.sound("BLOCK_AMETHYST_BLOCK_BREAK", ground, 1.0f, 0.75f);
        visuals.sound("UI_TOAST_CHALLENGE_COMPLETE", ground, 0.45f, 1.35f);
        visuals.particle("EXPLOSION_EMITTER", ground.clone().add(0, 0.6, 0), 1, 0.1, 0.1, 0.1, 0.0, null);
        for (int i = 0; i < 14; i++) {
            Location pop = ground.clone().add(
                    (Math.random() - 0.5) * 1.4,
                    0.5 + Math.random() * 1.0,
                    (Math.random() - 0.5) * 1.4);
            spawnNote(visuals, pop, (i % 24) / 24.0f);
        }
        NotesDisplays.setItem(block, Material.JUKEBOX);
        visuals.sound("BLOCK_WOOD_PLACE", ground, 0.9f, 0.85f);
    }

    private static void launchDisc(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Location ground,
            Player killer,
            UUID victimId,
            double range,
            double speed,
            int index,
            List<ItemDisplay> discDisplays,
            List<DiscShot> shots) {
        Material discMat = DISCS[index % DISCS.length];
        Location spawnAt = ground.clone().add(0.0, 0.95, 0.0);
        ItemDisplay display = NotesDisplays.spawnDisc(spawnAt, discMat);
        if (display == null) {
            return;
        }
        session.trackEntity(display);
        discDisplays.add(display);

        List<Player> nearby = findNearby(world, ground, killer, victimId, range, session);
        DiscMode mode;
        UUID targetId = null;
        Vector velocity;
        if (!nearby.isEmpty()) {
            mode = DiscMode.ATTACK;
            Player target = nearby.get(index % nearby.size());
            targetId = target.getUniqueId();
            Vector aim = target.getLocation().clone().add(0, 1.0, 0).toVector().subtract(spawnAt.toVector());
            if (aim.lengthSquared() < 1.0e-4) {
                aim = new Vector(Math.cos(index), 0.4, Math.sin(index));
            }
            
            double angle = (Math.PI * 2.0 * index) / Math.max(1, nearby.size() * 3) + index * 0.7;
            Vector side = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(0.35);
            velocity = aim.normalize().multiply(speed * 0.75).add(side).add(new Vector(0, 0.25, 0));
            visuals.sound("ENTITY_FIREWORK_ROCKET_LAUNCH", spawnAt, 0.55f, 1.35f);
            visuals.sound("BLOCK_NOTE_BLOCK_PLING", spawnAt, 0.9f, 1.5f);
        } else {
            mode = DiscMode.AERIAL;
            double angle = Math.random() * Math.PI * 2.0;
            velocity = new Vector(
                    Math.cos(angle) * (0.25 + Math.random() * 0.25),
                    0.55 + Math.random() * 0.35,
                    Math.sin(angle) * (0.25 + Math.random() * 0.25));
            visuals.sound("ENTITY_ITEM_PICKUP", spawnAt, 0.7f, 0.7f);
            visuals.sound("BLOCK_NOTE_BLOCK_HAT", spawnAt, 0.8f, 1.2f);
        }

        shots.add(new DiscShot(display, spawnAt, velocity, mode, targetId, index));
        spawnNote(visuals, spawnAt, (index % 24) / 24.0f);
        spawnNote(visuals, spawnAt.clone().add(0.15, 0.1, 0.0), ((index + 8) % 24) / 24.0f);
    }

    private static boolean updateDisc(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location ground,
            DiscShot shot,
            double range,
            double speed,
            double damagePerDisc) {
        if (shot.done || shot.display == null || !shot.display.isValid() || shot.display.isDead()) {
            NotesDisplays.remove(shot.display);
            return true;
        }

        shot.age++;
        int maxAge = shot.mode == DiscMode.ATTACK ? ATTACK_MAX_AGE : AERIAL_MAX_AGE;
        if (shot.age > maxAge) {
            fadeDisc(visuals, shot);
            return true;
        }

        if (shot.mode == DiscMode.ATTACK) {
            Player target = resolveAttackTarget(world, killer, victimId, ground, shot, range, session);
            if (target == null) {
                
                shot.mode = DiscMode.AERIAL;
                shot.targetId = null;
                shot.velocity = shot.velocity.clone().multiply(0.55).add(new Vector(
                        (Math.random() - 0.5) * 0.2, 0.35, (Math.random() - 0.5) * 0.2));
                visuals.sound("BLOCK_NOTE_BLOCK_BASS", shot.loc, 0.5f, 0.8f);
            } else {
                Location aim = target.getLocation().clone().add(0.0, 1.0, 0.0);
                
                double spin = shot.index * 0.9 + shot.age * 0.08;
                aim.add(Math.cos(spin) * 0.35, (shot.index % 3) * 0.12, Math.sin(spin) * 0.35);
                double dist = shot.loc.distance(aim);
                if (dist <= HIT_DISTANCE && shot.age >= 8) {
                    impact(session, visuals, killer, target, shot, damagePerDisc);
                    NotesDisplays.remove(shot.display);
                    shot.done = true;
                    return true;
                }
                Vector to = aim.toVector().subtract(shot.loc.toVector());
                if (to.lengthSquared() > 1.0e-6) {
                    Vector desired = to.normalize().multiply(speed);
                    shot.velocity = shot.velocity
                            .clone()
                            .multiply(0.72)
                            .add(desired.multiply(0.28));
                }
            }
        }

        if (shot.mode == DiscMode.AERIAL) {
            shot.velocity.setY(shot.velocity.getY() - GRAVITY);
            
            if (shot.loc.getY() <= ground.getY() + 0.15 && shot.velocity.getY() < 0) {
                fadeDisc(visuals, shot);
                visuals.sound("BLOCK_NOTE_BLOCK_BASEDRUM", shot.loc, 0.55f, 0.9f);
                return true;
            }
        }

        shot.loc.add(shot.velocity);
        float yaw = (float) (shot.age * 0.45 + shot.index);
        float pitch = (float) Math.atan2(-shot.velocity.getY(), Math.hypot(shot.velocity.getX(), shot.velocity.getZ()));
        NotesDisplays.placeDisc(shot.display, shot.loc, yaw, pitch, shot.age * 0.35f);
        
        if (shot.age % 3 == 0) {
            spawnNote(visuals, shot.loc, (shot.index % 24) / 24.0f);
        }
        Color trail = NOTE_COLORS[shot.index % NOTE_COLORS.length];
        visuals.dust(shot.loc, trail, 0.65f, 1, 0.0, 0.0, 0.0, 0.0);
        return false;
    }

    private static Player resolveAttackTarget(
            World world,
            Player killer,
            UUID victimId,
            Location ground,
            DiscShot shot,
            double range,
            EffectSession session) {
        Player locked = shot.targetId == null
                ? null
                : world.getPlayers().stream()
                        .filter(p -> p.getUniqueId().equals(shot.targetId))
                        .findFirst()
                        .orElse(null);
        if (locked != null
                && locked.isOnline()
                && !locked.isDead()
                && locked.getWorld().equals(world)
                && locked.getLocation().distanceSquared(ground) <= range * range) {
            return locked;
        }

        List<Player> nearby = findNearby(world, ground, killer, victimId, range, session);
        if (nearby.isEmpty()) {
            return null;
        }
        Player next = nearby.get(shot.index % nearby.size());
        shot.targetId = next.getUniqueId();
        return next;
    }

    private static void impact(
            EffectSession session,
            VisualEffectService visuals,
            Player killer,
            Player target,
            DiscShot shot,
            double damagePerDisc) {
        Location at = target.getLocation().clone().add(0.0, 1.0, 0.0);
        for (int i = 0; i < 10; i++) {
            spawnNote(
                    visuals,
                    at.clone().add((Math.random() - 0.5) * 0.6, Math.random() * 0.4, (Math.random() - 0.5) * 0.6),
                    ((shot.index + i) % 24) / 24.0f);
        }
        visuals.sound("BLOCK_NOTE_BLOCK_BASS", at, 1.2f, 0.7f);
        visuals.sound("ENTITY_PLAYER_HURT", at, 0.8f, 1.1f);
        if (killer == null || damagePerDisc <= 0.0) {
            return;
        }
        if (!session.allowsWorldMutation(killer, target.getLocation())) {
            return;
        }
        target.damage(damagePerDisc, killer);
    }

    private static void fadeDisc(VisualEffectService visuals, DiscShot shot) {
        for (int i = 0; i < 4; i++) {
            spawnNote(
                    visuals,
                    shot.loc.clone().add((Math.random() - 0.5) * 0.3, Math.random() * 0.2, (Math.random() - 0.5) * 0.3),
                    ((shot.index + i * 4) % 24) / 24.0f);
        }
        visuals.particle("SMOKE", shot.loc, 3, 0.08, 0.08, 0.08, 0.01, null);
        NotesDisplays.remove(shot.display);
        shot.done = true;
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
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            if (!player.getWorld().equals(world)) {
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

    private enum DiscMode {
        ATTACK,
        AERIAL
    }

    private static final class NoteOrb {
        private final Location loc;
        private final float hue;
        private int age;

        private NoteOrb(Location loc, float hue) {
            this.loc = loc.clone();
            this.hue = hue;
        }
    }

    private static final class DiscShot {
        private final ItemDisplay display;
        private final Location loc;
        private Vector velocity;
        private DiscMode mode;
        private UUID targetId;
        private final int index;
        private int age;
        private boolean done;

        private DiscShot(
                ItemDisplay display,
                Location loc,
                Vector velocity,
                DiscMode mode,
                UUID targetId,
                int index) {
            this.display = display;
            this.loc = loc.clone();
            this.velocity = velocity.clone();
            this.mode = mode;
            this.targetId = targetId;
            this.index = index;
        }
    }
}
