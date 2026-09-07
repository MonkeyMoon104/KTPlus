package com.monkey.ktplus.gui.inventory;

import java.util.Arrays;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jspecify.annotations.Nullable;

public final class PlayerInventorySnapshot {
    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final @Nullable ItemStack offhand;

    private PlayerInventorySnapshot(ItemStack[] storage, ItemStack[] armor, @Nullable ItemStack offhand) {
        this.storage = cloneArray(storage, 36);
        this.armor = cloneArray(armor, 4);
        this.offhand = cloneItem(offhand);
    }

    public static PlayerInventorySnapshot capture(Player player) {
        Objects.requireNonNull(player, "player");
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        ItemStack[] storage = new ItemStack[36];
        if (contents != null) {
            System.arraycopy(contents, 0, storage, 0, Math.min(contents.length, storage.length));
        }
        ItemStack[] armor = inventory.getArmorContents();
        ItemStack offhand = readOffhand(inventory);
        return new PlayerInventorySnapshot(storage, armor, offhand);
    }

    public static PlayerInventorySnapshot fromStored(
            ItemStack[] storage, ItemStack[] armor, @Nullable ItemStack offhand) {
        return new PlayerInventorySnapshot(storage, armor, offhand);
    }

    public void apply(Player player) {
        Objects.requireNonNull(player, "player");
        PlayerInventory inventory = player.getInventory();
        inventory.setContents(cloneArray(storage, storage.length));
        inventory.setArmorContents(cloneArray(armor, armor.length));
        writeOffhand(inventory, offhand);
        player.updateInventory();
    }

    public ItemStack[] storage() {
        return cloneArray(storage, storage.length);
    }

    public ItemStack[] armor() {
        return cloneArray(armor, armor.length);
    }

    public @Nullable ItemStack offhand() {
        return cloneItem(offhand);
    }

    private static @Nullable ItemStack readOffhand(PlayerInventory inventory) {
        try {
            Object value = inventory.getClass().getMethod("getItemInOffHand").invoke(inventory);
            return value instanceof ItemStack ? cloneItem((ItemStack) value) : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static void writeOffhand(PlayerInventory inventory, @Nullable ItemStack offhand) {
        try {
            inventory.getClass().getMethod("setItemInOffHand", ItemStack.class).invoke(inventory, cloneItem(offhand));
        } catch (ReflectiveOperationException ex) {
        }
    }

    private static ItemStack[] cloneArray(@Nullable ItemStack[] source, int length) {
        ItemStack[] copy = new ItemStack[length];
        if (source == null) {
            return copy;
        }
        for (int index = 0; index < Math.min(source.length, length); index++) {
            copy[index] = cloneItem(source[index]);
        }
        return copy;
    }

    private static @Nullable ItemStack cloneItem(@Nullable ItemStack item) {
        return item == null ? null : item.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlayerInventorySnapshot)) {
            return false;
        }
        PlayerInventorySnapshot that = (PlayerInventorySnapshot) other;
        return Arrays.equals(storage, that.storage)
                && Arrays.equals(armor, that.armor)
                && Objects.equals(offhand, that.offhand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(storage), Arrays.hashCode(armor), offhand);
    }
}
