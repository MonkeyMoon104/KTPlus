package com.monkey.ktplus.gui.layout;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.CategoryDefinition;
import com.monkey.ktplus.util.compat.MaterialResolver;
import com.monkey.ktplus.util.text.TextFormatter;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.monkey.ktplus.gui.effect.EffectGuiSortMode;

public final class GuiTheme {
    private GuiTheme() {}

    public static ItemStack blackBorderGlass(ConfigSnapshot config) {
        return namedGlass(config.guiBorderMaterial(), config.guiBorderName());
    }

    public static ItemStack emptyEffectGlass(ConfigSnapshot config) {
        return namedGlass(config.guiEmptyEffectMaterial(), config.guiEmptyEffectName());
    }

    public static ItemStack scrollArrow(ConfigSnapshot config, String buttonKey, boolean enabled) {
        String materialKey = config.guiButtonMaterial(buttonKey, "ARROW");
        Material material = MaterialResolver.resolve(
                enabled ? materialKey : "GRAY_STAINED_GLASS_PANE", materialKey);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String label = enabled
                    ? config.guiButtonName(buttonKey, "&eScroll")
                    : config.guiButtonNameDisabled(buttonKey, "&8Scroll");
            meta.displayName(TextFormatter.component(label));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack categoryTab(CategoryDefinition definition, boolean selected) {
        ItemStack item = new ItemStack(MaterialResolver.resolve(definition.tabIconKey(), "IRON_INGOT"));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextFormatter.component((selected ? "&a" : "&7") + stripColor(definition.displayName())));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack filterHopper(ConfigSnapshot config, EffectGuiSortMode mode) {
        ItemStack item = new ItemStack(MaterialResolver.resolve(config.guiButtonMaterial("filter", "HOPPER"), "HOPPER"));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String modeLabel = config.guiFilterModeLabel(mode.name(), mode.shortLabel());
            String title = config.guiButtonName("filter", "&6Filter &8| {mode}").replace("{mode}", modeLabel);
            meta.displayName(TextFormatter.component(title));

            List<String> configuredLore = config.guiButtonLore("filter");
            List<String> lore = new ArrayList<>();
            if (configuredLore.isEmpty()) {
                lore.add(TextFormatter.color("&7Sort for &fthis category &7only"));
                lore.add(TextFormatter.color("&eLeft-click &7next option"));
                lore.add(TextFormatter.color("&eRight-click &7previous option"));
                lore.add(TextFormatter.color("&8"));
                appendModeLines(config, mode, lore);
            } else {
                for (String line : configuredLore) {
                    if ("{modes}".equals(line)) {
                        appendModeLines(config, mode, lore);
                    } else {
                        lore.add(TextFormatter.color(line.replace("{mode}", modeLabel)));
                    }
                }
            }
            meta.lore(lore.stream().map(TextFormatter::component).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void appendModeLines(ConfigSnapshot config, EffectGuiSortMode active, List<String> lore) {
        String activeTpl = config.guiText("filter-mode-active", "&a▶ {label}");
        String inactiveTpl = config.guiText("filter-mode-inactive", "&8  {label}");
        for (EffectGuiSortMode option : EffectGuiSortMode.values()) {
            String label = stripColor(config.guiFilterModeLabel(option.name(), option.shortLabel()));
            String template = option == active ? activeTpl : inactiveTpl;
            lore.add(TextFormatter.color(template.replace("{label}", label)));
        }
    }

    private static String stripColor(String value) {
        return value.replaceAll("(?i)&[0-9a-fk-or]", "");
    }

    private static ItemStack namedGlass(String materialKey, String displayName) {
        ItemStack item = MaterialResolver.stainedGlassPane(materialKey, "STAINED_GLASS_PANE");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextFormatter.component(displayName));
            item.setItemMeta(meta);
        }
        return item;
    }
}
