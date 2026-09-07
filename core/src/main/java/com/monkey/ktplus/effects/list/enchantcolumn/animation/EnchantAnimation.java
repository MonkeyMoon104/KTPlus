package com.monkey.ktplus.effects.list.enchantcolumn.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.list.enchantcolumn.animation.util.EnchantItemDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.support.ui.PerkActionBar;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class EnchantAnimation {
    private static final int APPEAR_TICKS = 10;
    private static final int CHARGE_TICKS = 125;
    private static final int DESCENT_TICKS = 24;
    private static final int TOTAL_TICKS = APPEAR_TICKS + CHARGE_TICKS + DESCENT_TICKS;

    private static final int MIN_BOOKS = 2;
    private static final int MAX_BOOKS = 8;
    private static final int BOOK_SLOTS = 8;
    private static final double BOOK_ORBIT_RADIUS = 8.6;
    private static final double MAX_HEIGHT = 9.5;
    private static final int STREAM_SEGMENTS = 18;

    private static final Color[] RAINBOW = {
        Color.RED,
        Color.ORANGE,
        Color.YELLOW,
        Color.LIME,
        Color.AQUA,
        Color.BLUE,
        Color.FUCHSIA,
        Color.PURPLE
    };

    private EnchantAnimation() {}

    public static void start(
            EffectSession session,
            VisualEffectService visuals,
            EffectContext context,
            PotionEffectType effectType,
            int amplifier,
            int duration) {
        Location ground = context.location().clone();
        if (ground.getWorld() == null) {
            return;
        }
        Player killer = context.killer();
        ItemDisplay table = EnchantItemDisplays.spawnTable(ground);
        if (table == null) {
            return;
        }
        List<ItemDisplay> books = new ArrayList<>();
        session.trackEntity(table);
        session.onCleanup(() -> {
            EnchantItemDisplays.remove(table);
            EnchantItemDisplays.removeAll(books);
            PerkActionBar.clear(killer);
        });

        visuals.sound("BLOCK_ENCHANTMENT_TABLE_USE", ground, 1.15f, 0.8f);
        visuals.sound("BLOCK_AMETHYST_BLOCK_RESONATE", ground, 0.9f, 0.85f);

        AtomicInteger ticksLeft = new AtomicInteger(TOTAL_TICKS);
        session.runTimer(0L, 1L, () -> {
            if (!session.active() || !killer.isOnline()) {
                PerkActionBar.clear(killer);
                return false;
            }
            int left = ticksLeft.decrementAndGet();
            if (left < 0) {
                return false;
            }
            PerkActionBar.show(
                    killer,
                    String.format(Locale.US, "&d✦ ENCHANT &8| &eBLAST IN &f%.1fs &8| &7stand clear", left / 20.0));
            return true;
        });

        float[] tableYaw = {0.0f};
        double[] height = {0.0};
        double[] orbitAngle = {0.0};
        AtomicInteger tick = new AtomicInteger();
        session.runTimer(0L, 1L, () -> {
            if (!session.active() || !table.isValid() || table.isDead()) {
                EnchantItemDisplays.remove(table);
                EnchantItemDisplays.removeAll(books);
                return false;
            }
            int current = tick.getAndIncrement();

            if (current < APPEAR_TICKS) {
                float progress = (current + 1) / (float) APPEAR_TICKS;
                tableYaw[0] = progress * (float) (Math.PI * 1.5);
                EnchantItemDisplays.placeTable(table, ground, 0.0, progress, tableYaw[0]);
                spawnAmbientGlyphs(visuals, ground, 0.5 + progress);
                return true;
            }

            int chargeTick = current - APPEAR_TICKS;
            if (chargeTick < CHARGE_TICKS) {
                double progress = chargeTick / (double) (CHARGE_TICKS - 1);
                double eased = progress * progress * (3.0 - 2.0 * progress);
                ensureBooks(session, visuals, books, bookCount(progress), ground, orbitAngle[0], height[0]);

                tableYaw[0] += (float) (0.03 + eased * 0.26);
                orbitAngle[0] += 0.016 + eased * 0.11;
                height[0] = MAX_HEIGHT * eased;

                EnchantItemDisplays.placeTable(table, ground, height[0], 1.0f, tableYaw[0]);
                Location tableCenter = ground.clone().add(0.0, height[0] + 0.5, 0.0);
                updateBooksAndStreams(
                        visuals, ground, tableCenter, books, orbitAngle[0], height[0], chargeTick, progress);

                int pulseEvery = Math.max(5, 14 - (int) (progress * 9));
                if (chargeTick % pulseEvery == 0) {
                    float chargePitch = 0.7f + (float) (progress * 0.85);
                    float chargeVolume = 0.3f + (float) (progress * 0.45);
                    visuals.sound("BLOCK_ENCHANTMENT_TABLE_USE", tableCenter, chargeVolume, chargePitch);
                    visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", tableCenter, chargeVolume * 0.85f, 0.8f + (float) progress);
                }
                if (chargeTick == CHARGE_TICKS - 1) {
                    visuals.sound("BLOCK_RESPAWN_ANCHOR_SET_SPAWN", tableCenter, 1.35f, 1.15f);
                    visuals.sound("BLOCK_BEACON_POWER_SELECT", tableCenter, 1.1f, 1.25f);
                    visuals.sound("BLOCK_AMETHYST_BLOCK_RESONATE", tableCenter, 1.2f, 1.1f);
                }
                return true;
            }

            int descentTick = chargeTick - CHARGE_TICKS;
            if (descentTick >= DESCENT_TICKS) {
                EnchantItemDisplays.remove(table);
                EnchantItemDisplays.removeAll(books);
                books.clear();
                playImpactExplosionSounds(visuals, ground.clone().add(0.0, 0.5, 0.0));
                FinalEffect.apply(
                        session,
                        visuals,
                        context,
                        ground.clone().add(0.0, 0.5, 0.0),
                        killer,
                        effectType,
                        amplifier,
                        duration);
                return false;
            }

            double descentProgress = descentTick / (double) Math.max(1, DESCENT_TICKS - 1);
            height[0] = MAX_HEIGHT * (1.0 - descentProgress);
            tableYaw[0] += 0.7f;
            orbitAngle[0] += 0.42;
            EnchantItemDisplays.placeTable(table, ground, height[0], 1.0f, tableYaw[0]);
            Location tableCenter = ground.clone().add(0.0, height[0] + 0.5, 0.0);
            updateBooksDescent(
                    visuals, ground, tableCenter, books, orbitAngle[0], height[0], descentProgress, descentTick);
            spawnDescentParticles(visuals, ground, tableCenter, descentTick);
            if (descentTick % 4 == 0) {
                visuals.sound("ENTITY_BLAZE_HURT", tableCenter, 0.45f, 1.25f);
            }
            return true;
        });
    }

    private static int bookCount(double progress) {
        int scaledMax = Math.min(BOOK_SLOTS, Math.max(MIN_BOOKS, ParticleScale.scale(MAX_BOOKS)));
        double reveal = Math.max(0.0, Math.min(1.0, progress / 0.88));
        return MIN_BOOKS + (int) Math.floor((scaledMax - MIN_BOOKS) * reveal + 1.0e-6);
    }

    private static void ensureBooks(
            EffectSession session,
            VisualEffectService visuals,
            List<ItemDisplay> books,
            int desired,
            Location ground,
            double orbitAngle,
            double tableHeight) {
        double bookHeight = 1.15 + tableHeight * 0.28;
        while (books.size() < desired) {
            int slot = books.size();
            Location spawnAt = bookOrbitLocation(ground, orbitAngle, slot, BOOK_ORBIT_RADIUS, bookHeight);
            ItemDisplay book = EnchantItemDisplays.spawnBook(spawnAt);
            if (book == null) {
                break;
            }
            session.trackEntity(book);
            books.add(book);
            EnchantItemDisplays.placeBook(
                    book, spawnAt, (float) (slotAngle(orbitAngle, slot) + Math.PI * 0.5), -0.35f);
            playBookChargeSound(visuals, ground, spawnAt, slot);
        }
        while (books.size() > desired) {
            EnchantItemDisplays.remove(books.remove(books.size() - 1));
        }
    }

    private static void playBookChargeSound(
            VisualEffectService visuals, Location center, Location bookAt, int slot) {
        float charge = BOOK_SLOTS <= 1 ? 1.0f : slot / (float) (BOOK_SLOTS - 1);
        float pitch = 0.75f + charge * 0.85f;
        float volume = 1.15f + charge * 0.55f;
        Location hearAt = center.clone().add(0.0, 1.2, 0.0);
        visuals.sound("BLOCK_RESPAWN_ANCHOR_CHARGE", hearAt, volume, pitch);
        visuals.sound("BLOCK_ENCHANTMENT_TABLE_USE", hearAt, 0.85f + charge * 0.35f, 0.9f + charge * 0.45f);
        visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", bookAt, 0.95f + charge * 0.4f, 0.85f + charge * 0.7f);
        visuals.sound("ITEM_BOOK_PAGE_TURN", bookAt, 0.7f, 1.0f + charge * 0.35f);
        if (slot >= BOOK_SLOTS - 1) {
            visuals.sound("BLOCK_RESPAWN_ANCHOR_SET_SPAWN", hearAt, 1.35f, 1.2f);
            visuals.sound("BLOCK_AMETHYST_BLOCK_RESONATE", hearAt, 1.2f, 1.15f);
        }
    }

    private static void playImpactExplosionSounds(VisualEffectService visuals, Location at) {
        visuals.sound("ENTITY_GENERIC_EXPLODE", at, 4.2f, 0.72f);
        visuals.sound("ENTITY_GENERIC_EXPLODE", at, 3.0f, 0.55f);
        visuals.sound("ENTITY_LIGHTNING_BOLT_IMPACT", at, 1.6f, 0.85f);
        visuals.sound("BLOCK_END_PORTAL_SPAWN", at, 1.1f, 0.9f);
    }

    private static double slotAngle(double orbitAngle, int slot) {
        return orbitAngle + (Math.PI * 2.0 * slot / BOOK_SLOTS);
    }

    private static Location bookOrbitLocation(
            Location ground, double orbitAngle, int slot, double radius, double bookHeight) {
        double angle = slotAngle(orbitAngle, slot);
        return ground.clone().add(Math.cos(angle) * radius, bookHeight, Math.sin(angle) * radius);
    }

    private static void updateBooksAndStreams(
            VisualEffectService visuals,
            Location ground,
            Location tableCenter,
            List<ItemDisplay> books,
            double orbitAngle,
            double tableHeight,
            int chargeTick,
            double progress) {
        int count = books.size();
        if (count == 0) {
            return;
        }
        int segments = Math.max(10, ParticleScale.scale(STREAM_SEGMENTS));
        double bookHeight = 1.15 + tableHeight * 0.28;
        for (int i = 0; i < count; i++) {
            ItemDisplay book = books.get(i);
            if (!book.isValid() || book.isDead()) {
                continue;
            }
            double angle = slotAngle(orbitAngle, i);
            Location bookAt = bookOrbitLocation(ground, orbitAngle, i, BOOK_ORBIT_RADIUS, bookHeight);
            float faceYaw = (float) (angle + Math.PI * 0.5);
            float pitch = -0.35f + (float) (Math.sin(chargeTick * 0.1 + i) * 0.1);
            EnchantItemDisplays.placeBook(book, bookAt, faceYaw, pitch);
            spawnWaveStream(visuals, bookAt.clone().add(0.0, 0.2, 0.0), tableCenter, segments, chargeTick, i, progress);
        }
        visuals.particle(
                "ENCHANT",
                tableCenter,
                ParticleScale.scale(6 + (int) (progress * 10)),
                0.28,
                0.4,
                0.28,
                0.4,
                null);
    }

    private static void updateBooksDescent(
            VisualEffectService visuals,
            Location ground,
            Location tableCenter,
            List<ItemDisplay> books,
            double orbitAngle,
            double tableHeight,
            double descentProgress,
            int descentTick) {
        int count = books.size();
        if (count == 0) {
            return;
        }
        double radius = BOOK_ORBIT_RADIUS * (1.0 - descentProgress * 0.85);
        double bookHeight = 1.0 + tableHeight * 0.35;
        int segments = Math.max(8, ParticleScale.scale(12));
        for (int i = 0; i < count; i++) {
            ItemDisplay book = books.get(i);
            if (!book.isValid() || book.isDead()) {
                continue;
            }
            double angle = slotAngle(orbitAngle, i);
            Location bookAt = bookOrbitLocation(ground, orbitAngle, i, radius, bookHeight);
            EnchantItemDisplays.placeBook(book, bookAt, (float) (angle + Math.PI * 0.5), -0.55f);
            spawnWaveStream(
                    visuals,
                    bookAt.clone().add(0.0, 0.15, 0.0),
                    tableCenter,
                    segments,
                    descentTick,
                    i,
                    1.0);
        }
    }

    private static void spawnAmbientGlyphs(VisualEffectService visuals, Location ground, double height) {
        int count = ParticleScale.scale(4);
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double radius = 0.45 + Math.random() * 0.9;
            Location from = ground.clone().add(
                    Math.cos(angle) * radius,
                    0.2 + Math.random() * height,
                    Math.sin(angle) * radius);
            Vector delta = ground.clone().add(0.0, 1.0, 0.0).toVector().subtract(from.toVector());
            visuals.particle("ENCHANT", from, 0, delta.getX(), delta.getY(), delta.getZ(), 0.85, null);
        }
    }

    private static void spawnWaveStream(
            VisualEffectService visuals,
            Location from,
            Location tableCenter,
            int segments,
            int tick,
            int streamIndex,
            double progress) {
        double waveSpeed = 0.32 + progress * 0.4;
        double wavePhase = tick * waveSpeed + streamIndex * 1.15;
        for (int step = 0; step <= segments; step++) {
            double t = step / (double) segments;
            Location point = lerp(from, tableCenter, t);
            double arc = Math.sin(t * Math.PI) * (1.6 + progress * 1.2);
            point.add(0.0, arc, 0.0);

            double wave = Math.sin(t * Math.PI * 2.6 - wavePhase);
            if (wave < 0.2) {
                continue;
            }
            double intensity = 0.4 + wave * 0.6;
            Vector pull = tableCenter.toVector().subtract(point.toVector()).multiply(0.28 * intensity);
            visuals.particle(
                    "ENCHANT",
                    point,
                    0,
                    pull.getX(),
                    pull.getY(),
                    pull.getZ(),
                    0.6 + intensity * 0.55,
                    null);
        }
    }

    private static void spawnDescentParticles(
            VisualEffectService visuals, Location ground, Location tableCenter, int descentTick) {
        Color color = RAINBOW[descentTick % RAINBOW.length];
        visuals.particle("DUST", tableCenter, 4, 0.05, 0.05, 0.05, 0.0, color);
        spawnTipCircle(visuals, tableCenter, 2.6 + Math.sin(descentTick * 0.35) * 0.35, color);

        visuals.particle("ENCHANT", tableCenter, ParticleScale.scale(16), 0.4, 0.35, 0.4, 0.5, null);
        int arms = ParticleScale.scale(8);
        for (int i = 0; i < arms; i++) {
            double angle = descentTick * 0.35 + (Math.PI * 2.0 * i / arms);
            Location from = tableCenter.clone().add(Math.cos(angle) * 1.0, 0.1, Math.sin(angle) * 1.0);
            Vector down = ground.clone().add(0.0, 0.4, 0.0).toVector().subtract(from.toVector());
            visuals.particle("ENCHANT", from, 0, down.getX(), down.getY(), down.getZ(), 1.05, null);
        }
    }

    private static void spawnTipCircle(VisualEffectService visuals, Location tip, double radius, Color color) {
        int points = ParticleScale.scale(28);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            Location loc = tip.clone().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            visuals.particle("DUST", loc, 1, 0.0, 0.0, 0.0, 0.0, color);
        }
    }

    private static Location lerp(Location from, Location to, double t) {
        return from.clone().add(
                (to.getX() - from.getX()) * t,
                (to.getY() - from.getY()) * t,
                (to.getZ() - from.getZ()) * t);
    }
}
