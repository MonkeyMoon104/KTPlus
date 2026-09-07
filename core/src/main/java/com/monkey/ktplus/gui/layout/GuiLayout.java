package com.monkey.ktplus.gui.layout;

import com.monkey.ktplus.config.ConfigSnapshot;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class GuiLayout {
    public static final int TOP_SIZE = 54;
    public static final int FUSED_SIZE = 90;
    public static final int PLAYER_INVENTORY_OFFSET = 54;

    private final int rows;
    private final int size;
    private final int toolbarRow;
    private final int toolbarBaseSlot;
    private final List<Integer> categorySlots;

    private GuiLayout(int rows, List<Integer> categorySlots) {
        this.rows = Math.max(1, Math.min(6, rows));
        this.size = this.rows * 9;
        this.toolbarRow = this.rows - 1;
        this.toolbarBaseSlot = this.toolbarRow * 9;
        this.categorySlots = Collections.unmodifiableList(categorySlots);
    }

    public static GuiLayout of(int rows, List<Integer> categorySlots) {
        return new GuiLayout(rows, categorySlots);
    }

    public int rows() {
        return rows;
    }

    public int size() {
        return size;
    }

    public int toolbarRow() {
        return toolbarRow;
    }

    public int toolbarBaseSlot() {
        return toolbarBaseSlot;
    }

    public List<Integer> categorySlots() {
        return categorySlots;
    }

    public int categorySlot(int index) {
        if (index < 0 || index >= categorySlots.size()) {
            return -1;
        }
        return categorySlots.get(index);
    }

    public int buttonSlot(ConfigSnapshot config, String key, int toolbarColumn) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(key, "key");
        int configured = config.guiButtonSlot(key, -1);
        if (configured >= 0 && configured < size) {
            return configured;
        }
        return toolbarBaseSlot + toolbarColumn;
    }

    public boolean isBorderSlot(ConfigSnapshot config, int slot) {
        Objects.requireNonNull(config, "config");
        if (config.guiStructure().hasExplicitBorders()) {
            return config.guiStructure().borderSlots().contains(slot);
        }
        return isBorderSlot(slot);
    }

    public boolean isTopSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < size;
    }

    public boolean isBottomSlot(int rawSlot) {
        return rawSlot >= PLAYER_INVENTORY_OFFSET && rawSlot < FUSED_SIZE;
    }

    public boolean isFusedSlot(int rawSlot) {
        return isTopSlot(rawSlot) || isBottomSlot(rawSlot);
    }

    public int playerInventoryIndex(int rawSlot) {
        if (rawSlot >= 81) {
            return rawSlot - 81;
        }
        if (rawSlot >= PLAYER_INVENTORY_OFFSET) {
            return rawSlot - PLAYER_INVENTORY_OFFSET + 9;
        }
        return -1;
    }

    public boolean isBorderSlot(int slot) {
        if (slot < 0 || slot >= size) {
            return false;
        }
        int row = slotRow(slot);
        int column = slotColumn(slot);
        if (row == 0) {
            return true;
        }
        if (row == toolbarRow) {
            return true;
        }
        return column == 0 || column == 8;
    }

    public boolean isToolbarSlot(int slot) {
        return slot >= 0 && slot < size && slotRow(slot) == toolbarRow;
    }

    public boolean isCategorySlot(int rawSlot) {
        return categorySlots.contains(rawSlot);
    }

    private static int slotRow(int slot) {
        return slot / 9;
    }

    private static int slotColumn(int slot) {
        return slot % 9;
    }
}
