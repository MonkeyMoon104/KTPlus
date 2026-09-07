package com.monkey.ktplus.gui.effect;

import com.monkey.ktplus.access.effect.EffectAccessService;
import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.economy.EconomyService;
import com.monkey.ktplus.effects.api.CategoryDefinition;
import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.user.UserService;
import com.monkey.ktplus.util.compat.MaterialResolver;
import com.monkey.ktplus.util.text.TextFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import com.monkey.ktplus.gui.action.GuiAction;
import com.monkey.ktplus.gui.action.GuiActionType;
import com.monkey.ktplus.gui.layout.GuiLayout;
import com.monkey.ktplus.gui.layout.GuiTheme;
import com.monkey.ktplus.gui.session.GuiSession;
import com.monkey.ktplus.gui.window.GuiChestSlotWriter;

public final class EffectGuiPagePopulator {
    private final ConfigSnapshot config;
    private final UserService userService;
    private final EconomyService economyService;
    private final EffectAccessService accessService;

    public EffectGuiPagePopulator(
            ConfigSnapshot config,
            UserService userService,
            EconomyService economyService,
            EffectAccessService accessService) {
        this.config = Objects.requireNonNull(config, "config");
        this.userService = Objects.requireNonNull(userService, "userService");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.accessService = Objects.requireNonNull(accessService, "accessService");
    }

    public EffectGuiPagePopulator withConfig(ConfigSnapshot config) {
        return new EffectGuiPagePopulator(config, userService, economyService, accessService);
    }

    public void populateTop(
            GuiSession session,
            Player player,
            List<KillEffect> visibleEffects,
            int scrollOffset,
            int totalInCategory,
            GuiLayout layout,
            GuiChestSlotWriter writer) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(visibleEffects, "visibleEffects");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(writer, "writer");

        session.clearTopActions(layout.size());
        Set<Integer> reservedSlots = new HashSet<>();
        populateEffects(session, player, visibleEffects, layout, writer, reservedSlots);
        populateToolbar(session, player, layout, scrollOffset, totalInCategory, writer, reservedSlots);
        populateTopBorders(layout, writer, reservedSlots);
    }

    public void populateBottomCategoryBar(
            GuiSession session, Player player, GuiLayout layout, EffectCategory selectedCategory) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(selectedCategory, "selectedCategory");

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = new ItemStack[36];
        ItemStack border = GuiTheme.blackBorderGlass(config);
        for (int slot = 0; slot < contents.length; slot++) {
            contents[slot] = border.clone();
        }
        List<EffectCategory> categories = config.effectCategories();
        for (int index = 0; index < categories.size(); index++) {
            EffectCategory category = categories.get(index);
            int rawSlot = layout.categorySlot(index);
            if (rawSlot < 0) {
                continue;
            }
            int playerSlot = layout.playerInventoryIndex(rawSlot);
            if (playerSlot < 0 || playerSlot >= contents.length) {
                continue;
            }
            CategoryDefinition definition = config.categoryDefinition(category);
            contents[playerSlot] = GuiTheme.categoryTab(definition, category == selectedCategory);
            session.put(rawSlot, GuiAction.category(category));
        }
        inventory.setContents(contents);
        player.updateInventory();
    }

    private void populateTopBorders(GuiLayout layout, GuiChestSlotWriter writer, Set<Integer> reservedSlots) {
        for (int slot = 0; slot < layout.size(); slot++) {
            if (reservedSlots.contains(slot) || !layout.isBorderSlot(config, slot)) {
                continue;
            }
            writer.setBorder(slot, GuiTheme.blackBorderGlass(config));
        }
    }

    private void populateToolbar(
            GuiSession session,
            Player player,
            GuiLayout layout,
            int scrollOffset,
            int totalInCategory,
            GuiChestSlotWriter writer,
            Set<Integer> reservedSlots) {
        int scrollUpSlot = layout.buttonSlot(config, "scroll-up", 8);
        int scrollDownSlot = layout.buttonSlot(config, "scroll-down", 8);
        int filterSlot = layout.buttonSlot(config, "filter", 0);
        int clearSlot = layout.buttonSlot(config, "clear", 4);
        int closeSlot = layout.buttonSlot(config, "close", 3);
        int balanceSlot = layout.buttonSlot(config, "balance", 5);

        int visibleCount = config.guiEffectSlots().size();
        boolean canScrollUp = scrollOffset > 0;
        boolean canScrollDown = scrollOffset + visibleCount < totalInCategory;

        addButton(
                writer,
                session,
                layout,
                scrollUpSlot,
                GuiTheme.scrollArrow(config, "scroll-up", canScrollUp),
                canScrollUp
                        ? GuiAction.scroll(GuiActionType.SCROLL_UP, session.category(), Math.max(0, scrollOffset - visibleCount))
                        : null,
                reservedSlots);
        addButton(
                writer,
                session,
                layout,
                scrollDownSlot,
                GuiTheme.scrollArrow(config, "scroll-down", canScrollDown),
                canScrollDown
                        ? GuiAction.scroll(
                                GuiActionType.SCROLL_DOWN,
                                session.category(),
                                scrollOffset + visibleCount)
                        : null,
                reservedSlots);
        addButton(
                writer,
                session,
                layout,
                filterSlot,
                GuiTheme.filterHopper(config, session.sortMode()),
                GuiAction.simple(GuiActionType.FILTER, session.category(), scrollOffset),
                reservedSlots);
        addButton(
                writer,
                session,
                layout,
                clearSlot,
                buttonItem(
                        config.guiButtonMaterial("clear", "FIRE_CHARGE"),
                        "FIREBALL",
                        config.guiButtonName("clear", "&cClear Effect")),
                GuiAction.simple(GuiActionType.CLEAR, session.category(), scrollOffset),
                reservedSlots);
        addButton(
                writer,
                session,
                layout,
                closeSlot,
                buttonItem(
                        config.guiButtonMaterial("close", "OAK_DOOR"),
                        "WOOD_DOOR",
                        config.guiButtonName("close", "&cClose")),
                GuiAction.simple(GuiActionType.CLOSE, session.category(), scrollOffset),
                reservedSlots);
        long balance = economyService.balance(player);
        addButton(
                writer,
                session,
                layout,
                balanceSlot,
                buttonItem(
                        config.guiButtonMaterial("balance", "GOLD_NUGGET"),
                        "GOLD_NUGGET",
                        config.guiButtonName("balance", "&6Coins: &f{balance}")
                                .replace("{balance}", String.valueOf(balance))),
                GuiAction.simple(GuiActionType.BALANCE, session.category(), scrollOffset),
                reservedSlots);
    }

    private void populateEffects(
            GuiSession session,
            Player player,
            List<KillEffect> visibleEffects,
            GuiLayout layout,
            GuiChestSlotWriter writer,
            Set<Integer> reservedSlots) {
        List<Integer> effectSlots = config.guiEffectSlots();
        for (int index = 0; index < effectSlots.size(); index++) {
            int slot = effectSlots.get(index);
            if (slot < 0 || slot >= layout.size()) {
                continue;
            }
            if (index >= visibleEffects.size()) {
                addDecor(writer, layout, slot, GuiTheme.emptyEffectGlass(config), reservedSlots);
                continue;
            }
            KillEffect effect = visibleEffects.get(index);
            session.put(slot, GuiAction.effect(effect.definition().id(), session.category(), session.scrollOffset()));
            writer.set(slot, renderEffect(player, effect.definition()));
            reservedSlots.add(slot);
        }
    }

    private ItemStack renderEffect(Player player, EffectDefinition definition) {
        ItemStack item = definition.iconItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        CategoryDefinition category = config.categoryDefinition(definition.category());
        boolean selected = userService.selectedEffect(player).map(definition.id()::equalsIgnoreCase).orElse(false);
        boolean unlocked = accessService.canActivate(player, definition);

        String status = selected
                ? config.guiText("effect-item.status-selected", "&aSelected")
                : config.guiText("effect-item.status-click", "&7Click to select");
        String price = formatPrice(definition);
        String lock = unlocked
                ? config.guiText("effect-item.lock-unlocked", "&aUnlocked")
                : config.guiText("effect-item.lock-locked", "&cLocked");

        String nameTemplate = config.guiText("effect-item.name", "&b{name}");
        meta.displayName(TextFormatter.component(nameTemplate.replace("{name}", definition.displayName())));

        List<String> override = config.effectGuiLoreOverride(definition.id());
        List<String> template = override.isEmpty()
                ? config.guiTextList(
                        "effect-item.lore",
                        List.of("{category}", "{status}", "{price}", "{lock}"))
                : override;

        List<String> lore = new ArrayList<>();
        for (String line : template) {
            lore.add(TextFormatter.color(applyEffectPlaceholders(
                    line, definition.displayName(), category.displayName(), status, price, lock, definition.price())));
        }
        meta.lore(lore.stream().map(TextFormatter::component).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static String applyEffectPlaceholders(
            String line,
            String name,
            String category,
            String status,
            String price,
            String lock,
            int rawPrice) {
        return line.replace("{name}", name)
                .replace("{category}", category)
                .replace("{status}", status)
                .replace("{price}", price)
                .replace("{lock}", lock)
                .replace("{raw_price}", String.format("%,d", rawPrice));
    }

    private void addButton(
            GuiChestSlotWriter writer,
            GuiSession session,
            GuiLayout layout,
            int slot,
            ItemStack item,
            GuiAction action,
            Set<Integer> reservedSlots) {
        if (slot < 0 || slot >= layout.size() || action == null) {
            if (slot >= 0 && slot < layout.size()) {
                addDecor(writer, layout, slot, item, reservedSlots);
            }
            return;
        }
        session.put(slot, action);
        reservedSlots.add(slot);
        writer.set(slot, item);
    }

    private void addDecor(
            GuiChestSlotWriter writer, GuiLayout layout, int slot, ItemStack item, Set<Integer> reservedSlots) {
        if (slot < 0 || slot >= layout.size()) {
            return;
        }
        reservedSlots.add(slot);
        writer.set(slot, item);
    }

    private ItemStack buttonItem(String materialKey, String fallbackKey, String name) {
        ItemStack item = new ItemStack(MaterialResolver.resolve(materialKey, fallbackKey));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextFormatter.component(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatPrice(EffectDefinition definition) {
        if (definition.price() <= 0) {
            return config.guiText("effect-item.price-free", "&7Price: &aFree");
        }
        return config.guiText("effect-item.price-paid", "&7Price: &f{price} coins")
                .replace("{price}", String.format("%,d", definition.price()));
    }
}
