package com.monkey.ktplus.gui.action;

import com.monkey.ktplus.effects.api.EffectCategory;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class GuiAction {
    private final GuiActionType type;
    private final @Nullable String effectId;
    private final @Nullable EffectCategory category;
    private final int scrollOffset;

    public GuiAction(
            GuiActionType type,
            @Nullable String effectId,
            @Nullable EffectCategory category,
            int scrollOffset) {
        this.type = Objects.requireNonNull(type, "type");
        this.effectId = effectId;
        this.category = category;
        this.scrollOffset = Math.max(0, scrollOffset);
    }

    public static GuiAction effect(String effectId, EffectCategory category, int scrollOffset) {
        return new GuiAction(
                GuiActionType.EFFECT,
                Objects.requireNonNull(effectId, "effectId"),
                Objects.requireNonNull(category, "category"),
                scrollOffset);
    }

    public static GuiAction category(EffectCategory category) {
        return new GuiAction(GuiActionType.CATEGORY, null, Objects.requireNonNull(category, "category"), 0);
    }

    public static GuiAction scroll(GuiActionType type, EffectCategory category, int scrollOffset) {
        if (type != GuiActionType.SCROLL_UP && type != GuiActionType.SCROLL_DOWN) {
            throw new IllegalArgumentException("scroll action required");
        }
        return new GuiAction(type, null, Objects.requireNonNull(category, "category"), scrollOffset);
    }

    public static GuiAction simple(GuiActionType type, EffectCategory category, int scrollOffset) {
        return new GuiAction(Objects.requireNonNull(type, "type"), null, category, scrollOffset);
    }

    public GuiActionType type() {
        return type;
    }

    public @Nullable String effectId() {
        return effectId;
    }

    public @Nullable EffectCategory category() {
        return category;
    }

    public int scrollOffset() {
        return scrollOffset;
    }
}
