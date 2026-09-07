package com.monkey.ktplus.effects.list.icerink.animation;

import com.monkey.ktplus.effects.api.EffectContext;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.effects.list.icerink.animation.util.IceRinkDisplays;
import com.monkey.ktplus.effects.runtime.EffectSession;
import com.monkey.ktplus.effects.support.particle.ParticleScale;
import com.monkey.ktplus.effects.visual.VisualEffectService;
import com.monkey.ktplus.util.compat.EntityCompat;
import com.monkey.ktplus.util.item.PotionTypes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class IceRinkAnimation {
    private static final int MAX_TICKS = 150;
    private static final double DEFAULT_RADIUS = 6.0;
    private static final int DEFAULT_DURATION = 90;
    private static final double DEFAULT_SLIP = 0.35;
    private static final double DEFAULT_BURST = 3.0;

    private static final Color ICE = Color.fromRGB(170, 220, 255);
    private static final Color FROST = Color.fromRGB(220, 240, 255);
    private static final Color DEEP = Color.fromRGB(90, 150, 210);

    private static final String[] AMBIENT_SOUNDS = {
        "BLOCK_GLASS_BREAK",
        "BLOCK_POWDER_SNOW_STEP",
        "BLOCK_AMETHYST_BLOCK_CHIME",
        "ENTITY_PLAYER_HURT_FREEZE"
    };

    private IceRinkAnimation() {}

    public static void start(EffectSession session, VisualEffectService visuals, EffectContext context) {
        Location origin = context.location().clone().add(0.5, 0.05, 0.5);
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        ConfigurationSection section = context.config().effectSection("icerink");
        ConfigurationSection perks = section == null ? null : section.getConfigurationSection("perks");
        double radius = perks == null ? DEFAULT_RADIUS : Math.max(2.0, perks.getDouble("radius", DEFAULT_RADIUS));
        int durationTicks = perks == null
                ? DEFAULT_DURATION
                : Math.max(30, perks.getInt("duration-ticks", DEFAULT_DURATION));
        double slipStrength = perks == null
                ? DEFAULT_SLIP
                : Math.max(0.05, perks.getDouble("slip-strength", DEFAULT_SLIP));
        double burstDamageBase = perks == null
                ? DEFAULT_BURST
                : Math.max(0.0, perks.getDouble("burst-damage", DEFAULT_BURST));

        Player killer = context.killer();
        UUID victimId = context.victim() != null ? context.victim().getUniqueId() : null;
        EffectDamageConfig damageCfg = context.config().effectDamage("icerink");
        final double burstDamage = damageCfg.enabled()
                ? Math.max(burstDamageBase, damageCfg.value())
                : burstDamageBase;
        PotionEffectType slowType = PotionTypes.resolve("SLOWNESS", "SLOW");

        List<ItemDisplay> tiles = new ArrayList<>();
        List<Tile> floor = new ArrayList<>();
        int rings = Math.max(2, (int) Math.ceil(radius));
        for (int ring = 0; ring <= rings; ring++) {
            int points = ring == 0 ? 1 : Math.max(6, ring * 6);
            for (int i = 0; i < points; i++) {
                double angle = points == 1 ? 0.0 : (Math.PI * 2.0 * i) / points + ring * 0.12;
                double r = ring * 0.95;
                double x = origin.getX() + Math.cos(angle) * r;
                double z = origin.getZ() + Math.sin(angle) * r;
                double y = findGroundY(world, x, origin.getY(), z) + 0.06;
                Location at = new Location(world, x, y, z);
                ItemDisplay display = IceRinkDisplays.spawnTile(at, IceRinkDisplays.tileMaterial(i + ring));
                if (display == null) {
                    continue;
                }
                session.trackEntity(display);
                tiles.add(display);
                floor.add(new Tile(display, at, angle, r, (i + ring) * 0.07));
            }
        }

        if (tiles.isEmpty()) {
            return;
        }

        session.onCleanup(() -> {
            stopAmbient(origin);
            IceRinkDisplays.removeAll(tiles);
        });

        visuals.sound("BLOCK_GLASS_PLACE", origin, 0.9f, 1.4f);
        visuals.sound("BLOCK_AMETHYST_BLOCK_CHIME", origin, 0.7f, 1.6f);
        session.resetDeadline(Math.max(MAX_TICKS, durationTicks) + 30L);

        AtomicInteger tick = new AtomicInteger();
        boolean[] finished = {false};
        Set<UUID> slipped = new HashSet<>();

        session.runTimer(0L, 1L, () -> {
            if (!session.active() || finished[0]) {
                stopAmbient(origin);
                return false;
            }
            int current = tick.getAndIncrement();
            if (current >= durationTicks) {
                freezeBurst(session, visuals, world, killer, victimId, origin, radius, burstDamage, slowType);
                IceRinkDisplays.removeAll(tiles);
                stopAmbient(origin);
                finished[0] = true;
                return false;
            }

            for (Tile tile : floor) {
                float pulse = 0.88f + (float) Math.sin(current * 0.15 + tile.phase) * 0.08f;
                IceRinkDisplays.place(tile.display, tile.loc, (float) (tile.angle + current * 0.01), pulse, 0.1f);
            }

            if (current % 2 == 0) {
                drawFrost(visuals, origin, radius, current);
            }
            if (current % 10 == 0) {
                visuals.sound("BLOCK_POWDER_SNOW_STEP", origin, 0.3f, 1.5f);
            }

            applySlip(session, world, killer, victimId, origin, radius, slipStrength, slipped, current);
            return true;
        });
    }

    private static void drawFrost(VisualEffectService visuals, Location origin, double radius, int tick) {
        int points = ParticleScale.scale(8 + (int) radius);
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2.0 * i) / points + tick * 0.08;
            double r = radius * (0.35 + 0.55 * ((i + tick) % 5) / 5.0);
            Location p = origin.clone().add(Math.cos(a) * r, 0.15 + Math.sin(tick * 0.1 + i) * 0.05, Math.sin(a) * r);
            visuals.dust(p, i % 2 == 0 ? ICE : FROST, 0.9f, 1, 0, 0, 0, 0);
        }
        visuals.particle("CLOUD", origin.clone().add(0, 0.3, 0), 3, radius * 0.25, 0.05, radius * 0.25, 0.0, null);
    }

    private static void applySlip(
            EffectSession session,
            World world,
            Player killer,
            UUID victimId,
            Location origin,
            double radius,
            double slipStrength,
            Set<UUID> slipped,
            int tick) {
        if (killer == null) {
            return;
        }
        double radiusSq = radius * radius;
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
            Location feet = player.getLocation();
            if (feet.distanceSquared(origin) > radiusSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, feet)) {
                continue;
            }
            Vector vel = player.getVelocity();
            Vector slip = new Vector(
                    Math.sin(tick * 0.2 + player.getEntityId()) * slipStrength,
                    0.0,
                    Math.cos(tick * 0.17 + player.getEntityId()) * slipStrength);
            if (vel.lengthSquared() > 0.01) {
                Vector sideways = vel.clone().setY(0).crossProduct(new Vector(0, 1, 0));
                if (sideways.lengthSquared() > 1.0e-6) {
                    sideways.normalize().multiply(slipStrength * 0.45);
                    slip.add(sideways);
                }
            }
            player.setVelocity(vel.add(slip));
            if (tick % 8 == 0) {
                slipped.add(player.getUniqueId());
            }
        }
    }

    private static void freezeBurst(
            EffectSession session,
            VisualEffectService visuals,
            World world,
            Player killer,
            UUID victimId,
            Location origin,
            double radius,
            double burstDamage,
            PotionEffectType slowType) {
        visuals.sound("BLOCK_GLASS_BREAK", origin, 1.2f, 0.7f);
        visuals.sound("ENTITY_PLAYER_HURT_FREEZE", origin, 0.9f, 0.85f);
        visuals.dust(origin.clone().add(0, 1, 0), FROST, 1.8f, ParticleScale.scale(28), 1.0, 0.8, 1.0, 0.0);
        visuals.dust(origin.clone().add(0, 0.5, 0), DEEP, 1.4f, ParticleScale.scale(18), 0.8, 0.4, 0.8, 0.0);
        visuals.particle("CLOUD", origin.clone().add(0, 1.2, 0), ParticleScale.scale(22), 1.1, 0.7, 1.1, 0.02, null);

        if (killer == null) {
            return;
        }
        double radiusSq = radius * radius;
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
            if (player.getLocation().distanceSquared(origin) > radiusSq) {
                continue;
            }
            if (!session.allowsWorldMutation(killer, player.getLocation())) {
                continue;
            }
            if (burstDamage > 0.0) {
                player.damage(burstDamage, killer);
            }
            if (slowType != null) {
                player.addPotionEffect(new PotionEffect(slowType, 40, 2, true, true, true));
            }
            player.setFreezeTicks(Math.max(player.getFreezeTicks(), 60));
        }
    }

    private static double findGroundY(World world, double x, double aroundY, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int start = Math.min(world.getMaxHeight() - 2, (int) Math.floor(aroundY) + 3);
        int minY = Math.max(world.getMinHeight(), (int) Math.floor(aroundY) - 6);
        for (int y = start; y >= minY; y--) {
            Block block = world.getBlockAt(bx, y, bz);
            if (block.getType().isSolid()) {
                return y + 1.0;
            }
        }
        return aroundY;
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

    private static final class Tile {
        private final ItemDisplay display;
        private final Location loc;
        private final double angle;
        private final double radius;
        private final double phase;

        private Tile(ItemDisplay display, Location loc, double angle, double radius, double phase) {
            this.display = display;
            this.loc = loc;
            this.angle = angle;
            this.radius = radius;
            this.phase = phase;
        }
    }
}
