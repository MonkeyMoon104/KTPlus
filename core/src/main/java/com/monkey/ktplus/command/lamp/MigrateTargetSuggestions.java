package com.monkey.ktplus.command.lamp;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

public final class MigrateTargetSuggestions implements SuggestionProvider<BukkitCommandActor> {
    private static volatile Supplier<Collection<String>> source = Collections::emptyList;

    public static void bind(Supplier<Collection<String>> supplier) {
        source = Objects.requireNonNull(supplier, "supplier");
    }

    public static void clear() {
        source = Collections::emptyList;
    }

    @Override
    public Collection<String> getSuggestions(ExecutionContext<BukkitCommandActor> context) {
        Objects.requireNonNull(context, "context");
        Collection<String> targets = source.get();
        return targets == null ? Collections.emptyList() : targets;
    }
}
