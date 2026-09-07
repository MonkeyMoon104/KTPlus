package com.monkey.ktplus.logging;

import com.monkey.ktplus.config.ConfigSnapshot;
import com.monkey.ktplus.effects.api.CategoryDefinition;
import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class EffectBootReport {
    private static final int IDS_PER_LINE = 4;

    private EffectBootReport() {}

    public static void log(BootLogger boot, ConfigSnapshot config, EffectRegistry registry, int builtInCount) {
        List<KillEffect> effects = new ArrayList<>(registry.all());
        int total = effects.size();
        int customCount = Math.max(0, total - builtInCount);
        int heavy = 0;
        EnumMap<EffectCategory, Integer> byCategory = new EnumMap<>(EffectCategory.class);
        for (EffectCategory category : EffectCategory.ordered()) {
            byCategory.put(category, 0);
        }
        for (KillEffect effect : effects) {
            if (effect.definition().heavy()) {
                heavy++;
            }
            byCategory.merge(effect.definition().category(), 1, Integer::sum);
        }

        boot.success("Effects", "Registry ready");
        boot.detail("Effects", "total=" + total);
        boot.detail("Effects", "built-in=" + builtInCount);
        boot.detail("Effects", "custom=" + customCount);
        boot.detail("Effects", "heavy=" + heavy);
        boot.detail("Effects", "light=" + (total - heavy));

        for (Map.Entry<EffectCategory, Integer> entry : byCategory.entrySet()) {
            boot.detail("Effects", "count " + entry.getKey().configId() + "=" + entry.getValue());
        }

        for (EffectCategory category : EffectCategory.ordered()) {
            CategoryDefinition definition = config.categoryDefinition(category);
            int count = byCategory.getOrDefault(category, 0);
            List<String> ids = effects.stream()
                    .filter(effect -> effect.definition().category() == category)
                    .map(effect -> effect.definition().id())
                    .sorted()
                    .collect(Collectors.toList());

            boot.detail("Category", category.configId() + " display=" + stripColor(definition.displayName()));
            boot.detail("Category", category.configId() + " icon=" + definition.tabIconKey());
            boot.detail("Category", category.configId() + " min-price=" + definition.minPrice());
            boot.detail("Category", category.configId() + " effects=" + count);

            if (ids.isEmpty()) {
                boot.detail("Category", category.configId() + " ids=none");
                continue;
            }
            for (int i = 0; i < ids.size(); i += IDS_PER_LINE) {
                int end = Math.min(ids.size(), i + IDS_PER_LINE);
                boot.detail(
                        "Category",
                        category.configId() + " ids=" + String.join(", ", ids.subList(i, end)));
            }
        }
    }

    private static String stripColor(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replaceAll("(?i)&[0-9a-fk-or]", "")
                .replaceAll("(?i)§[0-9a-fk-or]", "")
                .trim();
    }
}
