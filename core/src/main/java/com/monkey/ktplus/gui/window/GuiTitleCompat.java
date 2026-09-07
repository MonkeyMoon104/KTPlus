package com.monkey.ktplus.gui.window;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.util.text.TextFormatter;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

public final class GuiTitleCompat {
    private GuiTitleCompat() {}

    public static String fusedTitle(ConfigSnapshot config, EffectCategory category) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(category, "category");
        return TextFormatter.color(
                config.guiTitle() + " &8| &7" + config.categoryDefinition(category).displayName());
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
