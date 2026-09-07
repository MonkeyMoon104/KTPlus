package com.monkey.ktplus.gui;

import com.monkey.ktplus.access.effect.EffectAccessService;
import com.monkey.ktplus.access.effect.EffectSelectionFeedback;
import com.monkey.ktplus.access.effect.EffectSelectionService;
import com.monkey.ktplus.common.gui.GuiBackend;
import com.monkey.ktplus.common.gui.GuiCompatibility;
import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.economy.EconomyService;
import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.monkey.ktplus.gui.inventory.PlayerInventoryGuard;
import com.monkey.ktplus.gui.inventory.PlayerInventorySnapshot;
import com.monkey.ktplus.user.UserService;
import com.monkey.ktplus.util.OnceLogger;
import com.monkey.ktplus.util.text.TextFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;
import com.monkey.ktplus.gui.action.GuiAction;
import com.monkey.ktplus.gui.backend.vanilla.VanillaInventoryHolder;
import com.monkey.ktplus.gui.effect.EffectGuiPagePopulator;
import com.monkey.ktplus.gui.effect.EffectGuiSortMode;
import com.monkey.ktplus.gui.layout.GuiLayout;
import com.monkey.ktplus.gui.session.GuiOpenResult;
import com.monkey.ktplus.gui.session.GuiSession;
import com.monkey.ktplus.gui.session.GuiSessionRegistry;
import com.monkey.ktplus.gui.window.GuiTitleCompat;
import com.monkey.ktplus.gui.window.GuiWindowBridge;
import com.monkey.ktplus.gui.window.GuiWindowHandle;

public final class EffectGuiService {
    private final GuiSessionRegistry sessions = new GuiSessionRegistry();
    private final Map<UUID, GuiWindowHandle> windowHandles = new ConcurrentHashMap<>();
    private final Map<UUID, EnumMap<EffectCategory, EffectGuiSortMode>> sortPreferences =
            new ConcurrentHashMap<>();
    private final Set<UUID> suppressCloseRestore = ConcurrentHashMap.newKeySet();
    private final PlayerInventoryGuard inventoryGuard;
    private ConfigSnapshot config;
    private EffectRegistry registry;
    private final UserService userService;
    private final EconomyService economyService;
    private final EffectAccessService accessService;
    private final EffectSelectionService selectionService;
    private final GuiBackend backend;
    private final OnceLogger onceLogger;
    private EffectGuiPagePopulator populator;

    public EffectGuiService(
            ConfigSnapshot config,
            EffectRegistry registry,
            UserService userService,
            EconomyService economyService,
            EffectAccessService accessService,
            EffectSelectionService selectionService,
            PlayerInventoryGuard inventoryGuard,
            GuiBackend backend,
            OnceLogger onceLogger) {
        this.config = Objects.requireNonNull(config, "config");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.userService = Objects.requireNonNull(userService, "userService");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.accessService = Objects.requireNonNull(accessService, "accessService");
        this.selectionService = Objects.requireNonNull(selectionService, "selectionService");
        this.inventoryGuard = Objects.requireNonNull(inventoryGuard, "inventoryGuard");
        this.backend = Objects.requireNonNull(backend, "backend");
        this.onceLogger = Objects.requireNonNull(onceLogger, "onceLogger");
        this.populator = new EffectGuiPagePopulator(config, userService, economyService, accessService);
    }

    public void reload(ConfigSnapshot config, EffectRegistry registry) {
        closeAll();
        this.config = Objects.requireNonNull(config, "config");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.populator = populator.withConfig(this.config);
    }

    public void closeAll() {
        for (UUID playerId : new ArrayList<>(sessions.activePlayerIds())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                finishSession(player, null);
            }
        }
        sessions.clear();
        windowHandles.clear();
        sortPreferences.clear();
        suppressCloseRestore.clear();
    }

    public GuiSessionRegistry sessions() {
        return sessions;
    }

    public PlayerInventoryGuard inventoryGuard() {
        return inventoryGuard;
    }

    public GuiBackend backend() {
        return backend;
    }

    public void open(Player player, int ignoredPage) {
        open(player);
    }

    public void open(Player player) {
        open(player, EffectCategory.COMMON, 0, true);
    }

    public void finishSession(Player player, @Nullable UUID sessionId) {
        Objects.requireNonNull(player, "player");
        if (suppressCloseRestore.contains(player.getUniqueId())) {
            return;
        }
        boolean closed;
        if (sessionId != null) {
            closed = sessions.removeIfCurrent(player, sessionId);
        } else {
            closed = sessions.get(player).isPresent();
            if (closed) {
                sessions.remove(player);
            }
        }
        if (closed) {
            windowHandles.remove(player.getUniqueId());
            sortPreferences.remove(player.getUniqueId());
            inventoryGuard.restore(player);
        }
    }

    public void handle(Player player, GuiAction action) {
        handle(player, action, null);
    }

    public void handle(Player player, GuiAction action, @Nullable ClickType click) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(action, "action");
        switch (action.type()) {
            case CATEGORY:
                if (action.category() != null) {
                    changeCategory(player, action.category());
                }
                break;
            case SCROLL_UP:
            case SCROLL_DOWN:
                if (action.category() != null) {
                    refresh(player, action.category(), action.scrollOffset());
                }
                break;
            case CLEAR:
                userService.clearEffect(player);
                player.sendMessage(config.message("effect-cleared"));
                refresh(player, currentCategory(player), currentScroll(player));
                break;
            case CLOSE:
                player.closeInventory();
                break;
            case BALANCE:
                player.sendMessage(config.message("killcoins-balance")
                        .replace("%player%", player.getName())
                        .replace("%balance%", Long.toString(economyService.balance(player))));
                break;
            case FILTER:
                if (action.category() != null) {
                    boolean backward = click != null && click.isRightClick();
                    cycleFilter(player, action.category(), backward);
                }
                break;
            case EFFECT:
                String effectId = action.effectId();
                if (effectId != null && action.category() != null) {
                    selectEffect(player, effectId, action.category(), action.scrollOffset());
                }
                break;
        }
    }

    private void cycleFilter(Player player, EffectCategory category, boolean backward) {
        EffectGuiSortMode current = sortMode(player, category);
        EffectGuiSortMode next = backward ? current.previous() : current.next();
        sortPreferences
                .computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(EffectCategory.class))
                .put(category, next);
        refresh(player, category, 0);
    }

    private void open(Player player, EffectCategory category, int scrollOffset, boolean captureInventory) {
        Objects.requireNonNull(player, "player");
        PlayerInventorySnapshot snapshot;
        if (captureInventory) {
            if (!inventoryGuard.begin(player)) {
                return;
            }
            snapshot = inventoryGuard.activeSnapshot(player).orElseThrow(IllegalStateException::new);
        } else {
            snapshot = sessions.get(player)
                    .map(GuiSession::inventorySnapshot)
                    .orElseGet(() -> inventoryGuard.activeSnapshot(player).orElse(null));
            if (snapshot == null) {
                open(player, category, scrollOffset, true);
                return;
            }
        }
        openWithSnapshot(player, category, scrollOffset, snapshot);
    }

    private void changeCategory(Player player, EffectCategory category) {
        GuiSession session = sessions.get(player).orElse(null);
        if (session == null) {
            open(player, category, 0, true);
            return;
        }
        if (session.category() == category) {
            return;
        }
        refreshInPlace(player, category, 0);
        GuiWindowHandle window = windowHandles.get(player.getUniqueId());
        if (window != null) {
            window.updateTitle(player, GuiTitleCompat.fusedTitle(config, category));
        }
    }

    private void refresh(Player player, EffectCategory category, int scrollOffset) {
        if (sessions.get(player).isPresent()
                && player.getOpenInventory().getTopInventory().getSize() >= config.guiRows() * 9) {
            refreshInPlace(player, category, scrollOffset);
            return;
        }
        open(player, category, scrollOffset, false);
    }

    private void refreshInPlace(Player player, EffectCategory category, int scrollOffset) {
        GuiSession previous = sessions.get(player).orElse(null);
        if (previous == null) {
            open(player, category, scrollOffset, false);
            return;
        }
        List<KillEffect> categoryEffects = effectsForCategory(player, category);
        List<Integer> effectSlots = config.guiEffectSlots();
        int visibleCount = Math.max(1, effectSlots.size());
        int maxScroll = Math.max(0, categoryEffects.size() - visibleCount);
        int safeScroll = Math.max(0, Math.min(scrollOffset, maxScroll));
        List<KillEffect> visibleEffects = slice(categoryEffects, safeScroll, visibleCount);
        GuiLayout layout = GuiLayout.of(config.guiRows(), config.guiCategorySlots());
        EffectGuiSortMode sortMode = sortMode(player, category);
        GuiSession session = new GuiSession(
                previous.id(),
                player.getUniqueId(),
                category,
                safeScroll,
                sortMode,
                previous.inventorySnapshot());
        GuiWindowHandle window = windowHandles.get(player.getUniqueId());
        UUID playerId = player.getUniqueId();
        if (window != null) {
            suppressCloseRestore.add(playerId);
        }
        populator.populateBottomCategoryBar(session, player, layout, category);
        if (window != null) {
            window.refreshTop(
                    player,
                    writer -> populator.populateTop(
                            session,
                            player,
                            visibleEffects,
                            safeScroll,
                            categoryEffects.size(),
                            layout,
                            writer));
        } else {
            org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
            populator.populateTop(
                    session,
                    player,
                    visibleEffects,
                    safeScroll,
                    categoryEffects.size(),
                    layout,
                    (slot, item) -> top.setItem(slot, item));
        }
        sessions.put(session);
        if (window != null) {
            scheduleBottomBarRestore(player);
        }
    }

    private void scheduleBottomBarRestore(Player player) {
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(EffectGuiService.class);
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            suppressCloseRestore.remove(playerId);
            if (!player.isOnline()) {
                return;
            }
            sessions.get(player).ifPresent(session -> {
                GuiLayout layout = GuiLayout.of(config.guiRows(), config.guiCategorySlots());
                populator.populateBottomCategoryBar(session, player, layout, session.category());
            });
        });
    }

    private void openWithSnapshot(
            Player player, EffectCategory category, int scrollOffset, PlayerInventorySnapshot snapshot) {
        sessions.remove(player);
        List<KillEffect> categoryEffects = effectsForCategory(player, category);
        List<Integer> effectSlots = config.guiEffectSlots();
        int visibleCount = Math.max(1, effectSlots.size());
        int maxScroll = Math.max(0, categoryEffects.size() - visibleCount);
        int safeScroll = Math.max(0, Math.min(scrollOffset, maxScroll));
        List<KillEffect> visibleEffects = slice(categoryEffects, safeScroll, visibleCount);
        GuiLayout layout = GuiLayout.of(config.guiRows(), config.guiCategorySlots());
        EffectGuiSortMode sortMode = sortMode(player, category);
        GuiSession session = new GuiSession(
                UUID.randomUUID(), player.getUniqueId(), category, safeScroll, sortMode, snapshot);
        populator.populateBottomCategoryBar(session, player, layout, category);
        try {
            GuiOpenResult libraryResult = GuiWindowBridge.open(
                    backend,
                    player,
                    session,
                    sessions,
                    config,
                    populator,
                    visibleEffects,
                    categoryEffects.size(),
                    safeScroll,
                    layout,
                    this::finishSession);
            if (libraryResult.opened()) {
                libraryResult.windowHandle().ifPresent(handle -> windowHandles.put(player.getUniqueId(), handle));
                return;
            }
            onceLogger.warnOnce(
                    "gui-library-fallback",
                    "Library GUI unavailable; using vanilla inventory fallback",
                    libraryResult.failure());
            openVanilla(player, session, visibleEffects, categoryEffects.size(), safeScroll, layout);
        } catch (RuntimeException failure) {
            finishSession(player, session.id());
            onceLogger.warnOnce("gui-open-failure", "GUI open failed", failure);
        }
    }

    private void openVanilla(
            Player player,
            GuiSession session,
            List<KillEffect> visibleEffects,
            int totalInCategory,
            int scrollOffset,
            GuiLayout layout) {
        String title = GuiTitleCompat.fusedTitle(config, session.category());
        Inventory inventory =
                Bukkit.createInventory(
                        new VanillaInventoryHolder(session.id()), layout.size(), TextFormatter.component(title));
        populator.populateTop(
                session,
                player,
                visibleEffects,
                scrollOffset,
                totalInCategory,
                layout,
                (slot, item) -> inventory.setItem(slot, item));
        sessions.put(session);
        windowHandles.put(player.getUniqueId(), GuiWindowHandle.vanilla(player));
        player.openInventory(inventory);
    }

    private void selectEffect(Player player, String effectId, EffectCategory category, int scrollOffset) {
        KillEffect effect = registry.find(effectId).orElse(null);
        if (effect == null) {
            player.sendMessage(config.message("unknown-effect").replace("%effect%", effectId));
            return;
        }
        EffectDefinition definition = effect.definition();
        EffectSelectionService.Outcome outcome = selectionService.select(player, definition);
        for (String message : EffectSelectionFeedback.messages(config, outcome, definition)) {
            player.sendMessage(message);
        }
        if (outcome == EffectSelectionService.Outcome.DENIED_PERMISSION
                || outcome == EffectSelectionService.Outcome.DENIED_FUNDS) {
            return;
        }
        refresh(player, category, scrollOffset);
    }

    private List<KillEffect> effectsForCategory(Player player, EffectCategory category) {
        EffectGuiSortMode mode = sortMode(player, category);
        return registry.all().stream()
                .filter(effect -> effect.definition().category() == category)
                .sorted(mode.comparator(player, accessService, economyService, userService))
                .collect(Collectors.toList());
    }

    private EffectGuiSortMode sortMode(Player player, EffectCategory category) {
        EnumMap<EffectCategory, EffectGuiSortMode> modes = sortPreferences.get(player.getUniqueId());
        if (modes == null) {
            return EffectGuiSortMode.NONE;
        }
        return modes.getOrDefault(category, EffectGuiSortMode.NONE);
    }

    private static List<KillEffect> slice(List<KillEffect> effects, int offset, int count) {
        if (offset >= effects.size()) {
            return Collections.emptyList();
        }
        int end = Math.min(effects.size(), offset + count);
        return effects.subList(offset, end);
    }

    private EffectCategory currentCategory(Player player) {
        return sessions.get(player).map(GuiSession::category).orElse(EffectCategory.COMMON);
    }

    private int currentScroll(Player player) {
        return sessions.get(player).map(GuiSession::scrollOffset).orElse(0);
    }

    private static String formatLibraryFailure(@Nullable Throwable failure) {
        if (failure == null) {
            return "unknown";
        }
        return GuiCompatibility.formatOpenFailure(failure);
    }
}
