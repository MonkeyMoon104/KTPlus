package com.monkey.ktplus.gui.action;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.util.compat.EntityCompat;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class GuiClickFeedback {
    private GuiClickFeedback() {}

    public static void play(Player player, @Nullable GuiAction action) {
        Objects.requireNonNull(player, "player");
        if (action == null) {
            return;
        }
        Location at = ear(player);
        switch (action.type()) {
            case CLOSE -> {}
            case SCROLL_UP, SCROLL_DOWN ->
                    EntityCompat.playSound(player, at, Sound.ITEM_BOOK_PAGE_TURN, 0.9f, 1.15f);
            case CATEGORY -> category(player, at, action.category());
            case BALANCE -> {
                EntityCompat.playSound(player, at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.55f);
                EntityCompat.playSound(player, at, Sound.ITEM_ARMOR_EQUIP_GOLD, 0.45f, 1.35f);
            }
            case FILTER -> {
                EntityCompat.playSound(player, at, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.55f, 1.45f);
                EntityCompat.playSound(player, at, Sound.BLOCK_COMPARATOR_CLICK, 0.4f, 1.3f);
            }
            case PREVIEW -> {
                EntityCompat.playSound(player, at, Sound.ENTITY_ENDER_EYE_LAUNCH, 0.55f, 1.2f);
                EntityCompat.playSound(player, at, Sound.BLOCK_ENDER_CHEST_OPEN, 0.35f, 1.4f);
            }
            case SEARCH -> EntityCompat.playSound(player, at, Sound.ITEM_SPYGLASS_USE, 0.75f, 1.15f);
            case FAVORITES -> {
                EntityCompat.playSound(player, at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.35f);
                EntityCompat.playSound(player, at, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.35f, 1.6f);
            }
            case CLEAR -> {
                EntityCompat.playSound(player, at, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.55f, 1.3f);
                EntityCompat.playSound(player, at, Sound.ITEM_FIRECHARGE_USE, 0.3f, 1.5f);
            }
            case EFFECT -> {
                EntityCompat.playSound(player, at, Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.55f, 1.25f);
                EntityCompat.playSound(player, at, Sound.UI_LOOM_SELECT_PATTERN, 0.25f, 1.4f);
            }
        }
    }

    public static void open(Player player) {
        Objects.requireNonNull(player, "player");
        Location at = ear(player);
        EntityCompat.playSound(player, at, Sound.ITEM_BOOK_PAGE_TURN, 0.95f, 1.0f);
        EntityCompat.playSound(player, at, Sound.ITEM_BOOK_PAGE_TURN, 0.4f, 1.2f);
    }

    public static void close(Player player) {
        Objects.requireNonNull(player, "player");
        EntityCompat.playSound(player, ear(player), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 0.78f);
    }

    private static void category(Player player, Location at, @Nullable EffectCategory category) {
        float pitch = categoryPitch(category);
        EntityCompat.playSound(player, at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.65f, pitch);
        EntityCompat.playSound(player, at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35f, pitch + 0.18f);
    }

    private static float categoryPitch(@Nullable EffectCategory category) {
        if (category == null) {
            return 1.0f;
        }
        return 0.75f + category.ordinal() * 0.15f;
    }

    private static Location ear(Player player) {
        return player.getLocation().clone().add(0.0, 1.8, 0.0);
    }
}
