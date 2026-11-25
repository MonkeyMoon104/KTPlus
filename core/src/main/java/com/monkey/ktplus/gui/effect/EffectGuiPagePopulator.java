package com.monkey.ktplus.gui.effect;

import com.monkey.ktplus.access.effect.EffectAccessService;
import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.economy.EconomyService;
import com.monkey.ktplus.effects.api.CategoryDefinition;
import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.lang.LangService;
import com.monkey.ktplus.user.UserService;
import com.monkey.ktplus.util.compat.MaterialResolver;
import com.monkey.ktplus.util.text.TextFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
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
    private final LangService lang;
    private final UserService userService;
    private final EconomyService economyService;
    private final EffectAccessService accessService;

    public EffectGuiPagePopulator(
            ConfigSnapshot config,
            LangService lang,
            UserService userService,
            EconomyService economyService,
            EffectAccessService accessService) {
        this.config = Objects.requireNonNull(config, "config");
        this.lang = Objects.requireNonNull(lang, "lang");
        this.userService = Objects.requireNonNull(userService, "userService");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.accessService = Objects.requireNonNull(accessService, "accessService");
    }

    public EffectGuiPagePopulator withConfig(ConfigSnapshot config) {
        return new EffectGuiPagePopulator(config, lang, userService, economyService, accessService);
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
            CategoryDefinition base = config.categoryDefinition(category);
            String display = lang.categoryDisplayName(player, category.configId(), base.displayName());
            CategoryDefinition definition =
                    new CategoryDefinition(category, display, base.tabIconKey(), base.minPrice());
            contents[playerSlot] = GuiTheme.categoryTab(definition, category == selectedCategory);
            session.put(rawSlot, GuiAction.category(category));
        }
        placeBottomButton(
                session,
                contents,
                layout,
                "preview",
                buttonItem(
                        config.guiButtonMaterial("preview", "ENDER_EYE"),
                        "ENDER_EYE",
                        lang.guiText(
                                player,
                                "buttons.preview.name",
                                config.guiButtonName("preview", "<gradient:#67e8f9:#38bdf8>Preview</gradient>")),
                        buttonLore(player, "preview")),
                GuiAction.simple(GuiActionType.PREVIEW, session.category(), session.scrollOffset()));
        String searchLabel = session.searchQuery() == null
                ? lang.guiText(
                        player,
                        "buttons.search.name",
                        config.guiButtonName("search", "<#fbbf24>Search</#fbbf24>"))
                : lang.guiText(player, "buttons.search.active", "&eSearch: &f{query}")
                        .replace("{query}", session.searchQuery());
        placeBottomButton(
                session,
                contents,
                layout,
                "search",
                buttonItem(
                        config.guiButtonMaterial("search", "SPYGLASS"),
                        "SPYGLASS",
                        searchLabel,
                        buttonLore(player, "search")),
                GuiAction.simple(GuiActionType.SEARCH, session.category(), session.scrollOffset()));
        String favoritesLabel = session.favoritesOnly()
                ? lang.guiText(player, "buttons.favorites.active", "&dFavorites &aON")
                : lang.guiText(
                        player,
                        "buttons.favorites.name",
                        config.guiButtonName(
                                "favorites", "<gradient:#f472b6:#fb7185>Favorites</gradient>"));
        placeBottomButton(
                session,
                contents,
                layout,
                "favorites",
                buttonItem(
                        config.guiButtonMaterial("favorites", "NETHER_STAR"),
                        "NETHER_STAR",
                        favoritesLabel,
                        buttonLore(player, "favorites")),
                GuiAction.simple(GuiActionType.FAVORITES, session.category(), session.scrollOffset()));
        long balance = economyService.balance(player);
        placeBottomButton(
                session,
                contents,
                layout,
                "balance",
                buttonItem(
                        config.guiButtonMaterial("balance", "GOLD_NUGGET"),
                        "GOLD_NUGGET",
                        lang.guiText(player, "buttons.balance.name", config.guiButtonName("balance", "&6Coins: &f{balance}"))
                                .replace("{balance}", String.valueOf(balance)),
                        buttonLore(player, "balance")),
                GuiAction.simple(GuiActionType.BALANCE, session.category(), session.scrollOffset()));
        inventory.setContents(contents);
        player.updateInventory();
    }

    private void placeBottomButton(
            GuiSession session,
            ItemStack[] contents,
            GuiLayout layout,
            String buttonKey,
            ItemStack item,
            GuiAction action) {
        int rawSlot = layout.buttonSlot(config, buttonKey, -1);
        if (rawSlot < GuiLayout.PLAYER_INVENTORY_OFFSET || rawSlot >= GuiLayout.FUSED_SIZE) {
            return;
        }
        int playerSlot = layout.playerInventoryIndex(rawSlot);
        if (playerSlot < 0 || playerSlot >= contents.length) {
            return;
        }
        contents[playerSlot] = item;
        session.put(rawSlot, action);
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
        int closeSlot = layout.buttonSlot(config, "close", 8);

        int visibleCount = config.guiEffectSlots().size();
        boolean canScrollUp = scrollOffset > 0;
        boolean canScrollDown = scrollOffset + visibleCount < totalInCategory;

        addTopButton(
                writer,
                session,
                layout,
                scrollUpSlot,
                GuiTheme.scrollArrow(config, lang, player, "scroll-up", canScrollUp),
                canScrollUp
                        ? GuiAction.scroll(GuiActionType.SCROLL_UP, session.category(), Math.max(0, scrollOffset - visibleCount))
                        : null,
                reservedSlots);
        addTopButton(
                writer,
                session,
                layout,
                scrollDownSlot,
                GuiTheme.scrollArrow(config, lang, player, "scroll-down", canScrollDown),
                canScrollDown
                        ? GuiAction.scroll(
                                GuiActionType.SCROLL_DOWN,
                                session.category(),
                                scrollOffset + visibleCount)
                        : null,
                reservedSlots);
        addTopButton(
                writer,
                session,
                layout,
                filterSlot,
                GuiTheme.filterHopper(config, lang, player, session.sortMode()),
                GuiAction.simple(GuiActionType.FILTER, session.category(), scrollOffset),
                reservedSlots);
        addTopButton(
                writer,
                session,
                layout,
                clearSlot,
                buttonItem(
                        config.guiButtonMaterial("clear", "FIRE_CHARGE"),
                        "FIREBALL",
                        lang.guiText(player, "buttons.clear.name", config.guiButtonName("clear", "&cClear Effect")),
                        buttonLore(player, "clear")),
                GuiAction.simple(GuiActionType.CLEAR, session.category(), scrollOffset),
                reservedSlots);
        addTopButton(
                writer,
                session,
                layout,
                closeSlot,
                buttonItem(
                        config.guiButtonMaterial("close", "BARRIER"),
                        "BARRIER",
                        lang.guiText(player, "buttons.close.name", config.guiButtonName("close", "&cClose")),
                        buttonLore(player, "close")),
                GuiAction.simple(GuiActionType.CLOSE, session.category(), scrollOffset),
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
            writer.set(slot, renderEffect(session, player, effect.definition()));
            reservedSlots.add(slot);
        }
    }

    private ItemStack renderEffect(GuiSession session, Player player, EffectDefinition definition) {
        ItemStack item = definition.iconItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        CategoryDefinition category = config.categoryDefinition(definition.category());
        String categoryName =
                lang.categoryDisplayName(player, definition.category().configId(), category.displayName());
        String displayName = lang.effectName(player, definition.id(), definition.displayName());
        String description = lang.effectDescription(player, definition.id());
        boolean selected = userService.selectedEffect(player).map(definition.id()::equalsIgnoreCase).orElse(false);
        boolean unlocked = accessService.canActivate(player, definition);

        String status = selected
                ? lang.guiText(player, "texts.effect-item.status-selected", "&aSelected")
                : lang.guiText(player, "texts.effect-item.status-click", "&7Click to select");
        String price = formatPrice(player, definition);
        String lock = unlocked
                ? lang.guiText(player, "texts.effect-item.lock-unlocked", "&aUnlocked")
                : lang.guiText(player, "texts.effect-item.lock-locked", "&cLocked");
        boolean favorite = session.isFavorite(definition.id());
        String favoriteLine = favorite
                ? lang.guiText(player, "texts.effect-item.favorite-on", "<#f472b6>★ Favorited</#f472b6>")
                : lang.guiText(player, "texts.effect-item.favorite-off", "&8☆ Favorite");

        String nameTemplate = lang.guiText(
                player, "texts.effect-item.name", "<gradient:#38bdf8:#a78bfa>{name}</gradient>");
        meta.displayName(TextFormatter.component(nameTemplate.replace("{name}", displayName)));

        List<String> override = config.effectGuiLoreOverride(definition.id());
        List<String> template = override.isEmpty()
                ? lang.guiTextList(
                        player,
                        "texts.effect-item.lore",
                        List.of(
                                "{description}",
                                "{category}",
                                "{status}",
                                "{price}",
                                "{lock}",
                                "{favorite}",
                                "&8",
                                "&eLeft-click &7select",
                                "&eRight-click &7favorite",
                                "&eShift-click &7preview"))
                : override;

        List<String> lore = new ArrayList<>();
        for (String line : template) {
            lore.add(applyEffectPlaceholders(
                    line,
                    displayName,
                    description,
                    categoryName,
                    status,
                    price,
                    lock,
                    favoriteLine,
                    definition.price()));
        }
        meta.lore(lore.stream().map(TextFormatter::component).toList());
        if (favorite || selected) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static String applyEffectPlaceholders(
            String line,
            String name,
            String description,
            String category,
            String status,
            String price,
            String lock,
            String favorite,
            int rawPrice) {
        return line.replace("{name}", name)
                .replace("{description}", description)
                .replace("{category}", category)
                .replace("{status}", status)
                .replace("{price}", price)
                .replace("{lock}", lock)
                .replace("{favorite}", favorite)
                .replace("{raw_price}", String.format("%,d", rawPrice));
    }

    private void addTopButton(
            GuiChestSlotWriter writer,
            GuiSession session,
            GuiLayout layout,
            int slot,
            ItemStack item,
            GuiAction action,
            Set<Integer> reservedSlots) {
        
        if (slot < 0 || slot >= layout.size()) {
            return;
        }
        if (action == null) {
            addDecor(writer, layout, slot, item, reservedSlots);
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

    private List<String> buttonLore(Player player, String buttonKey) {
        List<String> fromConfig = config.guiButtonLore(buttonKey);
        if (!fromConfig.isEmpty()) {
            return fromConfig;
        }
        return lang.guiTextList(player, "button-lore." + buttonKey, List.of());
    }

    private ItemStack buttonItem(String materialKey, String fallbackKey, String name) {
        return buttonItem(materialKey, fallbackKey, name, List.of());
    }

    private ItemStack buttonItem(String materialKey, String fallbackKey, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(MaterialResolver.resolve(materialKey, fallbackKey));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextFormatter.component(name));
            if (loreLines != null && !loreLines.isEmpty()) {
                meta.lore(loreLines.stream().map(TextFormatter::component).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatPrice(Player player, EffectDefinition definition) {
        if (definition.price() <= 0) {
            return lang.guiText(player, "texts.effect-item.price-free", "&7Price: &aFree");
        }
        return lang.guiText(player, "texts.effect-item.price-paid", "&7Price: &f{price} coins")
                .replace("{price}", String.format("%,d", definition.price()));
    }
}
