package com.monkey.ktplus.effects.registry;

import com.monkey.ktplus.effects.api.KillEffect;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EffectRegistry {
    private final Map<String, KillEffect> effects = new LinkedHashMap<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();

    public void clear() {
        effects.clear();
        aliases.clear();
    }

    public void register(KillEffect effect, String... effectAliases) {
        String id = normalize(effect.definition().id());
        if (effects.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate effect id: " + id);
        }
        effects.put(id, effect);
        for (String alias : effectAliases) {
            aliases.put(normalize(alias), id);
        }
    }

    public Optional<KillEffect> find(String idOrAlias) {
        String key = normalize(idOrAlias);
        String id = aliases.getOrDefault(key, key);
        return Optional.ofNullable(effects.get(id));
    }

    public Collection<KillEffect> all() {
        return new ArrayList<>(effects.values());
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
