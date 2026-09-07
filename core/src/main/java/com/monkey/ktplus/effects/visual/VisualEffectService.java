package com.monkey.ktplus.effects.visual;

import com.monkey.ktplus.bridge.VersionBridge;
import com.monkey.ktplus.util.compat.MaterialResolver;
import com.monkey.ktplus.util.OnceLogger;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public final class VisualEffectService {
    private static final Map<String, String> SOUND_KEYS = Map.ofEntries(
            Map.entry("ITEM_BOOK_PAGE_TURN", "item.book.page_turn"),
            Map.entry("BLOCK_ENCHANTMENT_TABLE_USE", "block.enchantment_table.use"),
            Map.entry("ENTITY_EXPERIENCE_ORB_PICKUP", "entity.experience_orb.pickup"),
            Map.entry("BLOCK_BEACON_POWER_SELECT", "block.beacon.power_select"),
            Map.entry("ENTITY_FIREWORK_ROCKET_BLAST", "entity.firework_rocket.blast"),
            Map.entry("ENTITY_FIREWORK_ROCKET_LAUNCH", "entity.firework_rocket.launch"),
            Map.entry("ENTITY_FIREWORK_ROCKET_TWINKLE", "entity.firework_rocket.twinkle"),
            Map.entry("ENTITY_LIGHTNING_BOLT_THUNDER", "entity.lightning_bolt.thunder"),
            Map.entry("ENTITY_LIGHTNING_BOLT_IMPACT", "entity.lightning_bolt.impact"),
            Map.entry("BLOCK_NOTE_BLOCK_PLING", "block.note_block.pling"),
            Map.entry("BLOCK_NOTE_BLOCK_CHIME", "block.note_block.chime"),
            Map.entry("BLOCK_NOTE_BLOCK_HARP", "block.note_block.harp"),
            Map.entry("ENTITY_ELDER_GUARDIAN_CURSE", "entity.elder_guardian.curse"),
            Map.entry("ENTITY_ENDERMAN_TELEPORT", "entity.enderman.teleport"),
            Map.entry("ENTITY_GENERIC_EXPLODE", "entity.generic.explode"),
            Map.entry("ENTITY_PLAYER_LEVELUP", "entity.player.levelup"),
            Map.entry("BLOCK_BEACON_ACTIVATE", "block.beacon.activate"),
            Map.entry("BLOCK_BEACON_AMBIENT", "block.beacon.ambient"),
            Map.entry("ENTITY_BLAZE_HURT", "entity.blaze.hurt"),
            Map.entry("ENTITY_BLAZE_SHOOT", "entity.blaze.shoot"),
            Map.entry("BLOCK_RESPAWN_ANCHOR_CHARGE", "block.respawn_anchor.charge"),
            Map.entry("BLOCK_RESPAWN_ANCHOR_SET_SPAWN", "block.respawn_anchor.set_spawn"),
            Map.entry("BLOCK_AMETHYST_BLOCK_CHIME", "block.amethyst_block.chime"),
            Map.entry("BLOCK_AMETHYST_BLOCK_RESONATE", "block.amethyst_block.resonate"),
            Map.entry("ENTITY_ALLAY_AMBIENT_WITH_ITEM", "entity.allay.ambient_with_item"),
            Map.entry("BLOCK_END_PORTAL_SPAWN", "block.end_portal.spawn"));

    private final VersionBridge bridge;
    private final OnceLogger onceLogger;

    public VisualEffectService(VersionBridge bridge, OnceLogger onceLogger) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.onceLogger = Objects.requireNonNull(onceLogger, "onceLogger");
    }

    public void particle(
            String name,
            Location location,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra,
            @Nullable Color color) {
        particle(name, location, count, offsetX, offsetY, offsetZ, extra, color, null);
    }

    public void particle(
            String name,
            Location location,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra,
            @Nullable Color color,
            @Nullable Object data) {
        if (location == null || location.getWorld() == null || name == null) {
            return;
        }
        String mappedName = bridge.particle(name);
        Particle particle = resolveParticle(mappedName);
        if (particle != null) {
            spawnResolvedParticle(
                    location, mappedName, particle, count, offsetX, offsetY, offsetZ, extra, color, data);
            return;
        }
        legacyEffect(mappedName, location, count);
    }

    public void blockParticle(
            String name,
            Location location,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra,
            Material blockMaterial) {
        if (blockMaterial == null) {
            particle(name, location, count, offsetX, offsetY, offsetZ, extra, null, null);
            return;
        }
        particle(name, location, count, offsetX, offsetY, offsetZ, extra, null, blockMaterial.createBlockData());
    }

    public void dust(
            Location location,
            Color color,
            float size,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        World world = location.getWorld();
        DustOptions dustOptions = new DustOptions(color != null ? color : Color.WHITE, size);
        world.spawnParticle(Particle.DUST, location, Math.max(0, count), offsetX, offsetY, offsetZ, extra, dustOptions);
    }

    public void sound(String name, Location location, float volume, float pitch) {
        if (location == null || location.getWorld() == null || name == null) {
            return;
        }
        String mapped = normalizeSound(bridge.sound(name));
        String key = toSoundKey(mapped);
        if (key.isEmpty()) {
            return;
        }
        location.getWorld().playSound(location, key, volume, pitch);
    }

    public String entity(String name) {
        return bridge.entity(name);
    }

    public String material(String name) {
        return bridge.material(name);
    }

    private void spawnResolvedParticle(
            Location location,
            String mappedName,
            Particle particle,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra,
            @Nullable Color color,
            @Nullable Object explicitData) {
        Class<?> dataType = particle.getDataType();
        Object data = resolveParticleData(mappedName, dataType, color, explicitData);
        if (requiresData(dataType) && data == null) {
            onceLogger.warnOnce(
                    "particle-data:" + mappedName,
                    "Unable to spawn particle '" + mappedName + "': missing particle data");
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        if (data == null) {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
            return;
        }
        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data);
    }

    private @Nullable Object resolveParticleData(
            String mappedName, Class<?> dataType, @Nullable Color color, @Nullable Object explicitData) {
        if (dataType == Void.class) {
            return null;
        }
        if (explicitData != null) {
            return explicitData;
        }
        if (DustOptions.class.isAssignableFrom(dataType)) {
            Color dustColor = color != null ? color : Color.WHITE;
            return new DustOptions(dustColor, 1.0f);
        }
        if (Integer.class.equals(dataType) || int.class.equals(dataType)) {
            return 0;
        }
        if (Float.class.equals(dataType) || float.class.equals(dataType)) {
            if (explicitData instanceof Number number) {
                return number.floatValue();
            }
            
            return 1.0f;
        }
        if (Color.class.isAssignableFrom(dataType)) {
            return color != null ? color : Color.WHITE;
        }
        String upper = mappedName.toUpperCase(Locale.ROOT);
        if (ItemStack.class.isAssignableFrom(dataType)
                && ("ITEM_SNOWBALL".equals(upper) || "ITEM".equals(upper))) {
            Material snowball = MaterialResolver.resolve("SNOWBALL", "SNOWBALL");
            return new ItemStack(snowball);
        }
        if (BlockData.class.isAssignableFrom(dataType)) {
            return Bukkit.createBlockData(Material.STONE);
        }
        return null;
    }

    private static boolean requiresData(Class<?> dataType) {
        
        return dataType != Void.class
                && dataType != Integer.class
                && dataType != int.class;
    }

    private void legacyEffect(String name, Location location, int count) {
        Effect effect = legacyEffect(name);
        if (effect == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        for (int i = 0; i < Math.max(1, Math.min(count, 16)); i++) {
            world.playEffect(location, effect, 0);
        }
    }

    private @Nullable Effect legacyEffect(String name) {
        String value = name == null ? "" : name.toUpperCase(Locale.ROOT);
        if (value.contains("FLAME") || value.contains("FIRE") || value.contains("LAVA")) {
            return resolveEffect("MOBSPAWNER_FLAMES", "FLAME");
        }
        if (value.contains("SMOKE") || value.contains("CLOUD")) {
            return resolveEffect("SMOKE", "CLOUD");
        }
        if (value.contains("HEART")) {
            return resolveEffect("HEART");
        }
        if (value.contains("NOTE")) {
            return resolveEffect("NOTE");
        }
        if (value.contains("PORTAL") || value.contains("ENCHANT")) {
            return resolveEffect("ENDER_SIGNAL", "PORTAL");
        }
        return resolveEffect("COLORED_DUST", "SMOKE");
    }

    private @Nullable Particle resolveParticle(String mappedName) {
        String normalized = normalizeParticle(mappedName);
        Particle particle = valueOfParticle(normalized);
        if (particle != null) {
            return particle;
        }
        if ("DUST".equals(normalized) || "REDSTONE".equals(normalized)) {
            return firstParticle("DUST", "REDSTONE");
        }
        if ("FIREWORK".equals(normalized)
                || "FIREWORKS".equals(normalized)
                || "FIREWORKS_SPARK".equals(normalized)) {
            return firstParticle("FIREWORK", "FIREWORKS_SPARK");
        }
        if ("LARGE_SMOKE".equals(normalized)) {
            return firstParticle("SMOKE_LARGE", "SMOKE", "CLOUD");
        }
        if ("VILLAGER_HAPPY".equals(normalized)
                || "HAPPY_VILLAGER".equals(normalized)) {
            return firstParticle("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        }
        if ("ITEM_SNOWBALL".equals(normalized)) {
            return firstParticle("ITEM_SNOWBALL", "ITEM");
        }
        return firstParticle(normalized);
    }

    private @Nullable Particle firstParticle(String... names) {
        for (String name : names) {
            Particle particle = valueOfParticle(name);
            if (particle != null) {
                return particle;
            }
        }
        return null;
    }

    private static @Nullable Particle valueOfParticle(String name) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static @Nullable Effect resolveEffect(String... names) {
        for (String name : names) {
            try {
                return Effect.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private String normalizeParticle(String name) {
        if (name == null) {
            return "CLOUD";
        }
        String value = name.toUpperCase(Locale.ROOT);
        if ("LARGE_SMOKE".equals(value)) {
            return "SMOKE_LARGE";
        }
        return value;
    }

    private String normalizeSound(String name) {
        String value = name.toUpperCase(Locale.ROOT);
        if (enumExists(value)) {
            return value;
        }
        if (value.contains("ENDERMAN")) {
            return "ENTITY_ENDERMAN_TELEPORT";
        }
        if (value.contains("EXPLODE")) {
            return "ENTITY_GENERIC_EXPLODE";
        }
        if (value.contains("FIREWORK")) {
            return "ENTITY_FIREWORK_ROCKET_BLAST";
        }
        if (value.contains("LEVELUP") || value.contains("PLAYER_LEVELUP")) {
            return "ENTITY_PLAYER_LEVELUP";
        }
        if (value.contains("BLAZE")) {
            return "ENTITY_BLAZE_SHOOT";
        }
        if (value.contains("WITHER")) {
            return "ENTITY_WITHER_SPAWN";
        }
        if (value.contains("LIGHTNING")) {
            return "ENTITY_LIGHTNING_BOLT_THUNDER";
        }
        return value;
    }

    private boolean enumExists(String value) {
        String key = toSoundKey(value);
        return !key.isEmpty() && Registry.SOUNDS.get(NamespacedKey.minecraft(key)) != null;
    }

    private static String toSoundKey(String mapped) {
        if (mapped == null || mapped.isEmpty()) {
            return "";
        }
        String upper = mapped.toUpperCase(Locale.ROOT);
        String known = SOUND_KEYS.get(upper);
        if (known != null) {
            return known;
        }
        if (upper.startsWith("ENTITY_FIREWORK_ROCKET_")) {
            return "entity.firework_rocket."
                    + upper.substring("ENTITY_FIREWORK_ROCKET_".length()).toLowerCase(Locale.ROOT);
        }
        if (upper.startsWith("BLOCK_NOTE_BLOCK_")) {
            return "block.note_block."
                    + upper.substring("BLOCK_NOTE_BLOCK_".length()).toLowerCase(Locale.ROOT);
        }
        if (upper.startsWith("BLOCK_RESPAWN_ANCHOR_")) {
            return "block.respawn_anchor."
                    + upper.substring("BLOCK_RESPAWN_ANCHOR_".length()).toLowerCase(Locale.ROOT);
        }
        if (upper.startsWith("BLOCK_AMETHYST_BLOCK_")) {
            return "block.amethyst_block."
                    + upper.substring("BLOCK_AMETHYST_BLOCK_".length()).toLowerCase(Locale.ROOT);
        }
        if (upper.startsWith("BLOCK_ENCHANTMENT_TABLE_")) {
            return "block.enchantment_table."
                    + upper.substring("BLOCK_ENCHANTMENT_TABLE_".length()).toLowerCase(Locale.ROOT);
        }
        if (upper.startsWith("ENTITY_EXPERIENCE_ORB_")) {
            return "entity.experience_orb."
                    + upper.substring("ENTITY_EXPERIENCE_ORB_".length()).toLowerCase(Locale.ROOT);
        }
        if (upper.startsWith("ENTITY_ELDER_GUARDIAN_")) {
            return "entity.elder_guardian."
                    + upper.substring("ENTITY_ELDER_GUARDIAN_".length()).toLowerCase(Locale.ROOT);
        }
        if (upper.startsWith("ENTITY_LIGHTNING_BOLT_")) {
            return "entity.lightning_bolt."
                    + upper.substring("ENTITY_LIGHTNING_BOLT_".length()).toLowerCase(Locale.ROOT);
        }
        return mapped.toLowerCase(Locale.ROOT).replace('_', '.');
    }
}
