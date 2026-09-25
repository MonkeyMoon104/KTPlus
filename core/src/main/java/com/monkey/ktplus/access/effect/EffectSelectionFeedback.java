package com.monkey.ktplus.access.effect;

import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.lang.LangService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

public final class EffectSelectionFeedback {
    private EffectSelectionFeedback() {}

    public static List<String> messages(
            LangService lang,
            @Nullable CommandSender sender,
            EffectSelectionService.Outcome outcome,
            EffectDefinition definition) {
        Objects.requireNonNull(lang, "lang");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(definition, "definition");
        String displayName = lang.effectName(sender, definition.id(), definition.displayName());
        if (outcome == EffectSelectionService.Outcome.DENIED_PERMISSION) {
            return Collections.singletonList(lang.message(sender, "no-permission"));
        }
        if (outcome == EffectSelectionService.Outcome.DENIED_FUNDS) {
            return Collections.singletonList(lang.message(sender, "not-enough-coins")
                    .replace("%price%", Integer.toString(definition.price())));
        }
        List<String> messages = new ArrayList<String>(2);
        if (outcome == EffectSelectionService.Outcome.PURCHASED_AND_SELECTED) {
            messages.add(lang.message(sender, "effect-purchased").replace("%effect%", displayName));
        }
        messages.add(lang.message(sender, "effect-selected").replace("%effect%", displayName));
        return messages;
    }
}
