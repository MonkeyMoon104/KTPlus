package com.monkey.ktplus.gui.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.Nullable;

public final class GuiStructure {
    private final List<Integer> effectSlots;
    private final Map<String, Integer> buttonSlots;
    private final List<Integer> borderSlots;
    private final Map<Character, Ingredient> ingredients;

    private GuiStructure(
            List<Integer> effectSlots,
            Map<String, Integer> buttonSlots,
            List<Integer> borderSlots,
            Map<Character, Ingredient> ingredients) {
        this.effectSlots = Collections.unmodifiableList(effectSlots);
        this.buttonSlots = Collections.unmodifiableMap(buttonSlots);
        this.borderSlots = Collections.unmodifiableList(borderSlots);
        this.ingredients = Collections.unmodifiableMap(ingredients);
    }

    public static GuiStructure parse(FileConfiguration gui, int rows) {
        Objects.requireNonNull(gui, "gui");
        int safeRows = Math.max(1, Math.min(6, rows));
        List<String> structure = gui.getStringList("structure");
        ConfigurationSection ingredientsSection = gui.getConfigurationSection("ingredients");
        Map<Character, Ingredient> ingredients = parseIngredients(ingredientsSection);

        List<Integer> effectSlots = new ArrayList<>();
        Map<String, Integer> buttonSlots = new LinkedHashMap<>();
        List<Integer> borderSlots = new ArrayList<>();

        if (structure != null && !structure.isEmpty()) {
            int maxRows = Math.min(safeRows, structure.size());
            for (int row = 0; row < maxRows; row++) {
                String line = structure.get(row);
                if (line == null) {
                    continue;
                }
                int cols = Math.min(9, line.length());
                for (int col = 0; col < cols; col++) {
                    char ch = line.charAt(col);
                    int slot = row * 9 + col;
                    Ingredient ingredient = ingredients.get(ch);
                    if (ingredient == null) {
                        continue;
                    }
                    switch (ingredient.type()) {
                        case EFFECT -> effectSlots.add(slot);
                        case BORDER -> borderSlots.add(slot);
                        case BUTTON -> {
                            if (ingredient.buttonKey() != null && !buttonSlots.containsKey(ingredient.buttonKey())) {
                                buttonSlots.put(ingredient.buttonKey(), slot);
                            }
                        }
                        case EMPTY -> {
                        }
                    }
                }
            }
        }

        if (effectSlots.isEmpty()) {
            for (Integer slot : gui.getIntegerList("effect-slots")) {
                if (slot != null) {
                    effectSlots.add(slot);
                }
            }
        }
        if (buttonSlots.isEmpty()) {
            ConfigurationSection buttons = gui.getConfigurationSection("buttons");
            if (buttons != null) {
                for (String key : buttons.getKeys(false)) {
                    if (buttons.isInt(key)) {
                        buttonSlots.put(key, buttons.getInt(key));
                    } else if (buttons.isConfigurationSection(key) && buttons.isInt(key + ".slot")) {
                        buttonSlots.put(key, buttons.getInt(key + ".slot"));
                    }
                }
            }
        }

        return new GuiStructure(effectSlots, buttonSlots, borderSlots, ingredients);
    }

    public List<Integer> effectSlots() {
        return effectSlots;
    }

    public Map<String, Integer> buttonSlots() {
        return buttonSlots;
    }

    public int buttonSlot(String key, int fallback) {
        Integer slot = buttonSlots.get(key);
        return slot == null ? fallback : slot;
    }

    public List<Integer> borderSlots() {
        return borderSlots;
    }

    public boolean hasExplicitBorders() {
        return !borderSlots.isEmpty();
    }

    public Map<Character, Ingredient> ingredients() {
        return ingredients;
    }

    private static Map<Character, Ingredient> parseIngredients(@Nullable ConfigurationSection section) {
        Map<Character, Ingredient> map = new LinkedHashMap<>();
        if (section == null) {
            return map;
        }
        for (String key : section.getKeys(false)) {
            if (key == null || key.isEmpty()) {
                continue;
            }
            char ch = key.charAt(0);
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null) {
                continue;
            }
            String typeRaw = child.getString("type", "empty");
            IngredientType type = IngredientType.from(typeRaw);
            String button = child.getString("button");
            String material = child.getString("material");
            String name = child.getString("name");
            map.put(ch, new Ingredient(type, button, material, name));
        }
        return map;
    }

    public enum IngredientType {
        EFFECT,
        BUTTON,
        BORDER,
        EMPTY;

        static IngredientType from(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return EMPTY;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "effect" -> EFFECT;
                case "button" -> BUTTON;
                case "border" -> BORDER;
                default -> EMPTY;
            };
        }
    }

    public record Ingredient(
            IngredientType type, @Nullable String buttonKey, @Nullable String material, @Nullable String name) {}
}
