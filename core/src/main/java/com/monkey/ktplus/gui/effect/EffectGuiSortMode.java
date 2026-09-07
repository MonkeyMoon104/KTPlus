package com.monkey.ktplus.gui.effect;

import com.monkey.ktplus.access.effect.EffectAccessService;
import com.monkey.ktplus.economy.EconomyService;
import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.user.UserService;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.entity.Player;

public enum EffectGuiSortMode {
    NONE("&7None &8(default order)", "None"),
    NAME_ASC("&eName &8(&aA → Z&8)", "Name A → Z"),
    NAME_DESC("&eName &8(&aZ → A&8)", "Name Z → A"),
    PRICE_ASC("&ePrice &8(&aLow → High&8)", "Price ↑"),
    PRICE_DESC("&ePrice &8(&aHigh → Low&8)", "Price ↓"),
    FREE_FIRST("&eFree first &8→ &aprice", "Free first"),
    UNLOCKED_FIRST("&eUnlocked first", "Unlocked first"),
    LOCKED_FIRST("&eLocked first", "Locked first"),
    AFFORDABLE_FIRST("&eAffordable first", "Affordable first"),
    SELECTED_FIRST("&eSelected first", "Selected first"),
    HEAVY_FIRST("&eHeavy first", "Heavy first"),
    ID_ASC("&eID &8(&aA → Z&8)", "ID A → Z");

    private final String loreLabel;
    private final String shortLabel;

    EffectGuiSortMode(String loreLabel, String shortLabel) {
        this.loreLabel = loreLabel;
        this.shortLabel = shortLabel;
    }

    public String loreLabel() {
        return loreLabel;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public EffectGuiSortMode next() {
        EffectGuiSortMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public EffectGuiSortMode previous() {
        EffectGuiSortMode[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }

    public Comparator<KillEffect> comparator(
            Player player,
            EffectAccessService accessService,
            EconomyService economyService,
            UserService userService) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(accessService, "accessService");
        Objects.requireNonNull(economyService, "economyService");
        Objects.requireNonNull(userService, "userService");

        Comparator<KillEffect> byName = Comparator.comparing(
                effect -> effect.definition().displayName().toLowerCase(Locale.ROOT));
        Comparator<KillEffect> byPrice = Comparator.comparingInt(effect -> effect.definition().price());
        Comparator<KillEffect> byId = Comparator.comparing(
                effect -> effect.definition().id().toLowerCase(Locale.ROOT));
        String selectedId = userService.selectedEffect(player).orElse(null);
        long balance = economyService.balance(player);

        return switch (this) {
            case NONE -> (left, right) -> 0;
            case NAME_ASC -> byName.thenComparing(byId);
            case NAME_DESC -> byName.reversed().thenComparing(byId);
            case PRICE_ASC -> byPrice.thenComparing(byName);
            case PRICE_DESC -> byPrice.reversed().thenComparing(byName);
            case FREE_FIRST -> Comparator.comparingInt((KillEffect effect) -> effect.definition().price() <= 0 ? 0 : 1)
                    .thenComparing(byPrice)
                    .thenComparing(byName);
            case UNLOCKED_FIRST -> Comparator.comparingInt(
                            (KillEffect effect) -> accessService.canActivate(player, effect.definition()) ? 0 : 1)
                    .thenComparing(byName);
            case LOCKED_FIRST -> Comparator.comparingInt(
                            (KillEffect effect) -> accessService.canActivate(player, effect.definition()) ? 1 : 0)
                    .thenComparing(byName);
            case AFFORDABLE_FIRST -> Comparator.comparingInt((KillEffect effect) -> {
                        EffectDefinition def = effect.definition();
                        if (accessService.canActivate(player, def)) {
                            return 0;
                        }
                        return def.price() <= balance ? 1 : 2;
                    })
                    .thenComparing(byPrice)
                    .thenComparing(byName);
            case SELECTED_FIRST -> Comparator.comparingInt((KillEffect effect) -> {
                        if (selectedId == null) {
                            return 1;
                        }
                        return selectedId.equalsIgnoreCase(effect.definition().id()) ? 0 : 1;
                    })
                    .thenComparing(byName);
            case HEAVY_FIRST -> Comparator.comparingInt((KillEffect effect) -> effect.definition().heavy() ? 0 : 1)
                    .thenComparing(byName);
            case ID_ASC -> byId.thenComparing(byName);
        };
    }
}
