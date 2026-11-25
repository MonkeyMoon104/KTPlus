package com.monkey.ktplus.gui.session;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.gui.inventory.PlayerInventorySnapshot;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.monkey.ktplus.gui.action.GuiAction;
import com.monkey.ktplus.gui.effect.EffectGuiSortMode;
import org.jspecify.annotations.Nullable;

public final class GuiSession {
    private final UUID id;
    private final UUID playerId;
    private final EffectCategory category;
    private final int scrollOffset;
    private final EffectGuiSortMode sortMode;
    private final PlayerInventorySnapshot inventorySnapshot;
    private final @Nullable String searchQuery;
    private final boolean favoritesOnly;
    private final Set<String> favoriteIds;
    private final Map<Integer, GuiAction> actions = new HashMap<>();
    private volatile boolean refreshing;

    public GuiSession(
            UUID id,
            UUID playerId,
            EffectCategory category,
            int scrollOffset,
            PlayerInventorySnapshot inventorySnapshot) {
        this(id, playerId, category, scrollOffset, EffectGuiSortMode.NONE, inventorySnapshot, null, false, Set.of());
    }

    public GuiSession(
            UUID id,
            UUID playerId,
            EffectCategory category,
            int scrollOffset,
            EffectGuiSortMode sortMode,
            PlayerInventorySnapshot inventorySnapshot) {
        this(id, playerId, category, scrollOffset, sortMode, inventorySnapshot, null, false, Set.of());
    }

    public GuiSession(
            UUID id,
            UUID playerId,
            EffectCategory category,
            int scrollOffset,
            EffectGuiSortMode sortMode,
            PlayerInventorySnapshot inventorySnapshot,
            @Nullable String searchQuery,
            boolean favoritesOnly,
            Set<String> favoriteIds) {
        this.id = Objects.requireNonNull(id, "id");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.category = Objects.requireNonNull(category, "category");
        this.scrollOffset = Math.max(0, scrollOffset);
        this.sortMode = Objects.requireNonNull(sortMode, "sortMode");
        this.inventorySnapshot = Objects.requireNonNull(inventorySnapshot, "inventorySnapshot");
        this.searchQuery = searchQuery == null || searchQuery.isBlank() ? null : searchQuery.trim();
        this.favoritesOnly = favoritesOnly;
        this.favoriteIds = Collections.unmodifiableSet(new HashSet<>(Objects.requireNonNull(favoriteIds, "favoriteIds")));
    }

    public UUID id() {
        return id;
    }

    public UUID playerId() {
        return playerId;
    }

    public EffectCategory category() {
        return category;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public EffectGuiSortMode sortMode() {
        return sortMode;
    }

    public PlayerInventorySnapshot inventorySnapshot() {
        return inventorySnapshot;
    }

    public @Nullable String searchQuery() {
        return searchQuery;
    }

    public boolean favoritesOnly() {
        return favoritesOnly;
    }

    public Set<String> favoriteIds() {
        return favoriteIds;
    }

    public boolean isFavorite(String effectId) {
        if (effectId == null) {
            return false;
        }
        return favoriteIds.contains(effectId) || favoriteIds.contains(effectId.toLowerCase());
    }

    public boolean refreshing() {
        return refreshing;
    }

    public void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }

    public void put(int rawSlot, GuiAction action) {
        actions.put(rawSlot, action);
    }

    public void clearActions() {
        actions.clear();
    }

    public void clearTopActions(int topSize) {
        actions.keySet().removeIf(rawSlot -> rawSlot >= 0 && rawSlot < topSize);
    }

    public Optional<GuiAction> action(int rawSlot) {
        return Optional.ofNullable(actions.get(rawSlot));
    }
}
