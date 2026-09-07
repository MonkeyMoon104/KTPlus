package com.monkey.ktplus.lib.relocate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class LibraryRelocationRules {
    public static final String RULES_VERSION = "10";
    public static final String ROOT = "com.monkey.ktplus.libs";

    private static final Map<String, String> RULES = buildRules();

    private LibraryRelocationRules() {}

    public static Map<String, String> rules() {
        return RULES;
    }

    public static String relocateClassName(String originalClassName) {
        Objects.requireNonNull(originalClassName, "originalClassName");
        if (originalClassName.isEmpty()) {
            return originalClassName;
        }
        String bestFrom = null;
        String bestTo = null;
        for (Map.Entry<String, String> entry : RULES.entrySet()) {
            String from = entry.getKey();
            if (originalClassName.equals(from) || originalClassName.startsWith(from + ".")) {
                if (bestFrom == null || from.length() > bestFrom.length()) {
                    bestFrom = from;
                    bestTo = entry.getValue();
                }
            }
        }
        if (bestFrom == null) {
            return originalClassName;
        }
        return bestTo + originalClassName.substring(bestFrom.length());
    }

    private static Map<String, String> buildRules() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("com.github.retrooper", ROOT + ".packetevents");
        map.put("io.github.retrooper", ROOT + ".packetevents");
        return Collections.unmodifiableMap(map);
    }
}
