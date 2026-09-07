package com.monkey.ktplus.access.effect;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.EffectDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class EffectSelectionFeedback {
    private EffectSelectionFeedback() {}

    public static List<String> messages(
            ConfigSnapshot config, EffectSelectionService.Outcome outcome, EffectDefinition definition) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(definition, "definition");
        if (outcome == EffectSelectionService.Outcome.DENIED_PERMISSION) {
            return Collections.singletonList(config.message("no-permission"));
        }
        if (outcome == EffectSelectionService.Outcome.DENIED_FUNDS) {
            return Collections.singletonList(config.message("not-enough-coins")
                    .replace("%price%", Integer.toString(definition.price())));
        }
        List<String> messages = new ArrayList<String>(2);
        if (outcome == EffectSelectionService.Outcome.PURCHASED_AND_SELECTED) {
            messages.add(config.message("effect-purchased").replace("%effect%", definition.displayName()));
        }
        messages.add(config.message("effect-selected").replace("%effect%", definition.displayName()));
        return messages;
    }
}
