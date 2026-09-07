package com.monkey.ktplus.gui.inventory;

import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public final class PlayerInventorySerializer {
    private static final String STORAGE_KEY = "storage";
    private static final String ARMOR_KEY = "armor";
    private static final String OFFHAND_KEY = "offhand";

    private PlayerInventorySerializer() {}

    public static String serialize(PlayerInventorySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        YamlConfiguration yaml = new YamlConfiguration();
        writeItems(yaml, STORAGE_KEY, snapshot.storage());
        writeItems(yaml, ARMOR_KEY, snapshot.armor());
        ItemStack offhand = snapshot.offhand();
        if (offhand != null) {
            yaml.set(OFFHAND_KEY, offhand.serialize());
        }
        return yaml.saveToString();
    }

    public static PlayerInventorySnapshot deserialize(String payload) {
        Objects.requireNonNull(payload, "payload");
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(payload);
        } catch (org.bukkit.configuration.InvalidConfigurationException ex) {
            throw new IllegalArgumentException("Invalid inventory payload", ex);
        }
        ItemStack[] storage = readItems(yaml, STORAGE_KEY, 36);
        ItemStack[] armor = readItems(yaml, ARMOR_KEY, 4);
        ItemStack offhand = readItem(yaml.get(OFFHAND_KEY));
        return PlayerInventorySnapshot.fromStored(storage, armor, offhand);
    }

    private static void writeItems(YamlConfiguration yaml, String key, ItemStack[] items) {
        for (int index = 0; index < items.length; index++) {
            ItemStack item = items[index];
            if (item != null) {
                yaml.set(key + "." + index, item.serialize());
            }
        }
    }

    private static ItemStack[] readItems(YamlConfiguration yaml, String key, int length) {
        ItemStack[] items = new ItemStack[length];
        if (!yaml.isConfigurationSection(key)) {
            return items;
        }
        for (String indexKey : yaml.getConfigurationSection(key).getKeys(false)) {
            try {
                int index = Integer.parseInt(indexKey);
                if (index < 0 || index >= length) {
                    continue;
                }
                items[index] = readItem(yaml.get(key + "." + indexKey));
            } catch (NumberFormatException ignored) {
            }
        }
        return items;
    }

    private static @Nullable ItemStack readItem(@Nullable Object raw) {
        Map<String, Object> map = asMap(raw);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return ItemStack.deserialize(map);
    }

    private static @Nullable Map<String, Object> asMap(@Nullable Object raw) {
        if (raw instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) raw;
            return map;
        }
        if (raw instanceof ConfigurationSection) {
            return ((ConfigurationSection) raw).getValues(false);
        }
        return null;
    }
}
