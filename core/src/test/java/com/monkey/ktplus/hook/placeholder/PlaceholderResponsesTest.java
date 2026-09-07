package com.monkey.ktplus.hook.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.monkey.ktplus.effects.api.EffectCategory;
import com.monkey.ktplus.effects.api.EffectDefinition;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class PlaceholderResponsesTest {
    @Test
    void globalAndPlayerPlaceholdersResolve() {
        EffectDefinition cloud = new EffectDefinition(
                "cloud", "Cloud", Material.STONE, "CLOUD", EffectCategory.COMMON, 500, 500, false, 40L);
        FakeSource source = new FakeSource(cloud);
        Player player = dummyPlayer();

        assertEquals("4.0.0", PlaceholderResponses.resolve("version", null, source));
        assertEquals("KTPlus", PlaceholderResponses.resolve("name", null, source));
        assertEquals("MonkeyMoon104", PlaceholderResponses.resolve("author", null, source));
        assertEquals("ktplus", PlaceholderResponses.resolve("identifier", null, source));
        assertEquals("1", PlaceholderResponses.resolve("effect_count", null, source));
        assertEquals("KILLCOINS", PlaceholderResponses.resolve("provider", null, source));
        assertEquals("yes", PlaceholderResponses.resolve("economy_enabled", null, source));

        assertEquals("4200", PlaceholderResponses.resolve("balance", player, source));
        assertEquals("4,200", PlaceholderResponses.resolve("coins_formatted", player, source));
        assertEquals("cloud", PlaceholderResponses.resolve("selected", player, source));
        assertEquals("Cloud", PlaceholderResponses.resolve("selected_name", player, source));
        assertEquals("500", PlaceholderResponses.resolve("selected_price", player, source));
        assertEquals("common", PlaceholderResponses.resolve("selected_category", player, source));
        assertEquals("yes", PlaceholderResponses.resolve("has_selected", player, source));
        assertEquals("yes", PlaceholderResponses.resolve("owns_cloud", player, source));
        assertEquals("yes", PlaceholderResponses.resolve("unlocked_cloud", player, source));
        assertEquals("500", PlaceholderResponses.resolve("price_cloud", player, source));
        assertEquals("Cloud", PlaceholderResponses.resolve("name_cloud", player, source));
        assertEquals("common", PlaceholderResponses.resolve("category_cloud", player, source));
        assertEquals("yes", PlaceholderResponses.resolve("is_selected_cloud", player, source));
        assertEquals("1", PlaceholderResponses.resolve("unlocked_count", player, source));
        assertNull(PlaceholderResponses.resolve("unknown", player, source));
        assertNull(PlaceholderResponses.resolve("balance", null, source));
    }

    private static Player dummyPlayer() {
        return (Player) Proxy.newProxyInstance(
                PlaceholderResponsesTest.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> {
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) {
                        return false;
                    }
                    if (type == int.class) {
                        return 0;
                    }
                    if (type == long.class) {
                        return 0L;
                    }
                    if (type == double.class) {
                        return 0D;
                    }
                    if (type == float.class) {
                        return 0F;
                    }
                    return null;
                });
    }

    private static final class FakeSource implements PlaceholderResponses.PlaceholderSource {
        private final EffectDefinition cloud;

        private FakeSource(EffectDefinition cloud) {
            this.cloud = cloud;
        }

        @Override
        public String identifier() {
            return "ktplus";
        }

        @Override
        public String pluginName() {
            return "KTPlus";
        }

        @Override
        public String authors() {
            return "MonkeyMoon104";
        }

        @Override
        public String version() {
            return "4.0.0";
        }

        @Override
        public String providerId() {
            return "KILLCOINS";
        }

        @Override
        public boolean economyEnabled() {
            return true;
        }

        @Override
        public int effectCount() {
            return 1;
        }

        @Override
        public long balance(Player player) {
            return 4200L;
        }

        @Override
        public Optional<String> selectedEffectId(Player player) {
            return Optional.of("cloud");
        }

        @Override
        public Optional<EffectDefinition> effect(String id) {
            if (id != null && id.toLowerCase(Locale.ROOT).equals("cloud")) {
                return Optional.of(cloud);
            }
            return Optional.empty();
        }

        @Override
        public boolean ownsOrFree(Player player, EffectDefinition definition) {
            return true;
        }

        @Override
        public boolean canActivate(Player player, EffectDefinition definition) {
            return true;
        }

        @Override
        public int unlockedCount(Player player) {
            return 1;
        }
    }
}
