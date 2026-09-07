package com.monkey.ktplus.config;

import com.monkey.ktplus.effects.api.CategoryDefinition;
import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.config.EffectDamageConfig;
import com.monkey.ktplus.gui.layout.GuiStructure;
import com.monkey.ktplus.util.text.TextFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.Nullable;

public final class ConfigSnapshot {
    private final YamlConfiguration main;
    private final YamlConfiguration messages;
    private final YamlConfiguration effects;
    private final YamlConfiguration economy;
    private final YamlConfiguration gui;
    private final YamlConfiguration database;
    private final YamlConfiguration events;
    private final YamlConfiguration resourcePack;
    private final YamlConfiguration performance;
    private final GuiStructure guiStructure;

    public ConfigSnapshot(
            YamlConfiguration main,
            YamlConfiguration messages,
            YamlConfiguration effects,
            YamlConfiguration economy,
            YamlConfiguration gui,
            YamlConfiguration database,
            YamlConfiguration events,
            YamlConfiguration resourcePack,
            YamlConfiguration performance) {
        this.main = Objects.requireNonNull(main, "main");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.effects = Objects.requireNonNull(effects, "effects");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.gui = Objects.requireNonNull(gui, "gui");
        this.database = Objects.requireNonNull(database, "database");
        this.events = Objects.requireNonNull(events, "events");
        this.resourcePack = Objects.requireNonNull(resourcePack, "resourcePack");
        this.performance = Objects.requireNonNull(performance, "performance");
        this.guiStructure = GuiStructure.parse(gui, guiRows());
    }

    public FileConfiguration main() {
        return main;
    }

    public FileConfiguration database() {
        return database;
    }

    public FileConfiguration events() {
        return events;
    }

    public FileConfiguration resourcePack() {
        return resourcePack;
    }

    public FileConfiguration gui() {
        return gui;
    }

    public GuiStructure guiStructure() {
        return guiStructure;
    }

    public String message(String key) {
        String prefix = messages.getString("prefix", "");
        if (prefix == null) {
            prefix = "";
        }
        String value = messages.getString(key, "");
        if (value == null) {
            value = "";
        }
        return TextFormatter.color(prefix + value);
    }

    public boolean economyEnabled() {
        return economy.getBoolean("enabled", true);
    }

    public String economyProviderRaw() {
        String provider = economy.getString("provider", "");
        if (provider != null && !provider.trim().isEmpty()) {
            return provider.trim().toUpperCase(Locale.ROOT);
        }
        return economy.getBoolean("use-internal", true) ? "KILLCOINS" : "VAULT";
    }

    public int startingBalance() {
        return Math.max(0, economy.getInt("starting-balance", 0));
    }

    public int killReward(boolean playerKill) {
        if (!economy.getBoolean("kill-reward.enabled", true)) {
            return 0;
        }
        return Math.max(
                0,
                playerKill
                        ? economy.getInt("kill-reward.player", 10)
                        : economy.getInt("kill-reward.mob", 1));
    }

    public boolean luckPermsGrantOnPurchase() {
        return main.getBoolean("luckperms.grant-permission-on-purchase", true);
    }

    public String worldGuardBlockChangesRaw() {
        String value = main.getString("worldguard.block-changes", "RESPECT");
        return value == null ? "RESPECT" : value;
    }

    public boolean worldGuardRespectBuildFlag() {
        return main.getBoolean("worldguard.respect-build-flag", true);
    }

    public String worldGuardBypassPermission() {
        String value = main.getString("worldguard.bypass-permission", "ktplus.worldguard.bypass");
        return value == null ? "ktplus.worldguard.bypass" : value;
    }

    public boolean isWorldDisabled(String worldName) {
        if (worldName == null) {
            return false;
        }
        List<String> disabled = main.getStringList("disabled-worlds");
        for (String entry : disabled) {
            if (entry != null && entry.equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        return false;
    }

    public int maxSessionsPerPlayer() {
        return Math.max(1, performance.getInt("max-active-sessions-per-player", 2));
    }

    public int maxHeavySessionsGlobal() {
        return Math.max(1, performance.getInt("max-heavy-sessions-global", 12));
    }

    public boolean adaptiveTpsEnabled() {
        return performance.getBoolean("adaptive-tps.enabled", true);
    }

    public double adaptiveLowTpsThreshold() {
        return performance.getDouble("adaptive-tps.low-tps-threshold", 18.0D);
    }

    public double adaptiveCriticalTpsThreshold() {
        return performance.getDouble("adaptive-tps.critical-tps-threshold", 15.0D);
    }

    public String guiTitle() {
        String title = gui.getString("title", "&0KT+ Effects");
        return TextFormatter.color(title == null ? "&0KT+ Effects" : title);
    }

    public int guiRows() {
        return Math.max(1, Math.min(6, gui.getInt("rows", 6)));
    }

    public List<Integer> guiEffectSlots() {
        List<Integer> fromStructure = guiStructure.effectSlots();
        if (!fromStructure.isEmpty()) {
            return fromStructure;
        }
        return intList(gui.getIntegerList("effect-slots"));
    }

    public List<Integer> guiCategorySlots() {
        return intList(gui.getIntegerList("category-slots"));
    }

    public int guiButtonSlot(String key, int fallback) {
        int fromStructure = guiStructure.buttonSlot(key, -1);
        if (fromStructure >= 0) {
            return fromStructure;
        }
        if (gui.contains("buttons." + key) && gui.isInt("buttons." + key)) {
            return gui.getInt("buttons." + key, fallback);
        }
        if (gui.contains("buttons." + key + ".slot")) {
            return gui.getInt("buttons." + key + ".slot", fallback);
        }
        return fallback;
    }

    public String guiButtonMaterial(String key, String fallback) {
        String value = gui.getString("buttons." + key + ".material", fallback);
        return value == null || value.isBlank() ? fallback : value;
    }

    public String guiButtonName(String key, String fallback) {
        String value = gui.getString("buttons." + key + ".name", fallback);
        return value == null || value.isBlank() ? fallback : value;
    }

    public String guiButtonNameDisabled(String key, String fallback) {
        String value = gui.getString("buttons." + key + ".name-disabled", fallback);
        return value == null || value.isBlank() ? fallback : value;
    }

    public List<String> guiButtonLore(String key) {
        return stringList(gui.getStringList("buttons." + key + ".lore"));
    }

    public String guiText(String path, String fallback) {
        String value = gui.getString("texts." + path, fallback);
        return value == null || value.isBlank() ? fallback : value;
    }

    public List<String> guiTextList(String path, List<String> fallback) {
        List<String> value = gui.getStringList("texts." + path);
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        return stringList(value);
    }

    public String guiFilterModeLabel(String modeKey, String fallback) {
        String value = gui.getString("texts.filter-modes." + modeKey, fallback);
        return value == null || value.isBlank() ? fallback : value;
    }

    public String guiBorderMaterial() {
        String fromIngredient = firstIngredientMaterial("border");
        if (fromIngredient != null) {
            return fromIngredient;
        }
        return guiText("border.material", "BLACK_STAINED_GLASS_PANE");
    }

    public String guiBorderName() {
        String fromIngredient = firstIngredientName("border");
        if (fromIngredient != null) {
            return fromIngredient;
        }
        return guiText("border.name", "&0 ");
    }

    public String guiEmptyEffectMaterial() {
        return guiText("empty-effect.material", "WHITE_STAINED_GLASS_PANE");
    }

    public String guiEmptyEffectName() {
        return guiText("empty-effect.name", "&f ");
    }

    public List<EffectCategory> effectCategories() {
        return EffectCategory.ordered();
    }

    public CategoryDefinition categoryDefinition(EffectCategory category) {
        Objects.requireNonNull(category, "category");
        String id = category.configId();
        String display = firstNonBlank(
                gui.getString("categories." + id + ".display-name"),
                effects.getString("categories." + id + ".display-name"),
                category.name());
        String icon = firstNonBlank(
                gui.getString("categories." + id + ".tab-icon"),
                effects.getString("categories." + id + ".tab-icon"),
                "STONE");
        int minPrice = effects.getInt("categories." + id + ".min-price", 0);
        return new CategoryDefinition(category, display, icon, minPrice);
    }

    public String effectName(String id, String fallback) {
        String value = firstNonBlank(
                gui.getString("effects." + id + ".name"),
                effects.getString("effects." + id + ".name"),
                fallback);
        return value;
    }

    public @Nullable String effectIconKey(String id) {
        String value = firstNonBlank(
                gui.getString("effects." + id + ".material"),
                gui.getString("effects." + id + ".icon"),
                effects.getString("effects." + id + ".icon"));
        return value.isBlank() ? null : value;
    }

    public List<String> effectGuiLoreOverride(String id) {
        return stringList(gui.getStringList("effects." + id + ".lore"));
    }

    public EffectCategory effectCategory(String id, EffectCategory fallback) {
        return EffectCategory.fromConfigId(effects.getString("effects." + id + ".category"), fallback);
    }

    public int effectPremiumPrice(String id, int fallback) {
        if (!effects.contains("effects." + id + ".price")) {
            return fallback;
        }
        return Math.max(0, effects.getInt("effects." + id + ".price", fallback));
    }

    public @Nullable ConfigurationSection effectSection(String id) {
        return effects.getConfigurationSection("effects." + id);
    }

    public boolean effectStructure(String id, boolean fallback) {
        ConfigurationSection section = effectSection(id);
        if (section == null) {
            return fallback;
        }
        return section.getBoolean("structure", section.getBoolean("perks.structure", fallback));
    }

    public EffectDamageConfig effectDamage(String id) {
        ConfigurationSection section = effects.getConfigurationSection("effects." + id + ".damage");
        if (section == null) {
            return EffectDamageConfig.disabled();
        }
        return new EffectDamageConfig(
                section.getBoolean("enabled", false),
                section.getDouble("value", 0D),
                section.getDouble("radius", 0D),
                section.getLong("delay", 0L));
    }

    public long effectCooldownMillis() {
        double seconds = main.getDouble("effect-cooldown-seconds", main.getDouble("cooldown-seconds", 0.0D));
        if (seconds <= 0.0D) {
            return Math.max(0L, main.getLong("effect-cooldown-millis", 0L));
        }
        return Math.max(0L, Math.round(seconds * 1000.0D));
    }

    public double effectDurationMultiplier(String id) {
        ConfigurationSection section = effectSection(id);
        if (section == null) {
            return 1.0D;
        }
        return Math.max(0.1D, section.getDouble("duration-multiplier", section.getDouble("duration_multiplier", 1.0D)));
    }

    private @Nullable String firstIngredientMaterial(String type) {
        for (GuiStructure.Ingredient ingredient : guiStructure.ingredients().values()) {
            if (ingredient.type().name().equalsIgnoreCase(type) && ingredient.material() != null) {
                return ingredient.material();
            }
        }
        return null;
    }

    private @Nullable String firstIngredientName(String type) {
        for (GuiStructure.Ingredient ingredient : guiStructure.ingredients().values()) {
            if (ingredient.type().name().equalsIgnoreCase(type) && ingredient.name() != null) {
                return ingredient.name();
            }
        }
        return null;
    }

    private static String firstNonBlank(@Nullable String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static List<Integer> intList(List<Integer> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<Integer>(raw));
    }

    private static List<String> stringList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<String>(raw));
    }
}
