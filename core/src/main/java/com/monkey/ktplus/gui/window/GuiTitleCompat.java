package com.monkey.ktplus.gui.window;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.lang.LangService;
import com.monkey.ktplus.util.text.TextFormatter;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

public final class GuiTitleCompat {
    private GuiTitleCompat() {}

    public static String fusedTitle(
            ConfigSnapshot config, LangService lang, Player player, EffectCategory category) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(lang, "lang");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(category, "category");
        String title = lang.guiText(player, "title", config.guiTitle());
        String categoryName = lang.categoryDisplayName(
                player, category.configId(), config.categoryDefinition(category).displayName());
        return TextFormatter.color(title + " &8| &7" + categoryName);
    }

    static void updateOpenViewTitle(Player player, String title) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");
        InventoryView view = player.getOpenInventory();
        try {
            view.getClass().getMethod("setTitle", String.class).invoke(view, title);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
