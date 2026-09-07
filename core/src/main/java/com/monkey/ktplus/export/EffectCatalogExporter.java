package com.monkey.ktplus.export;

import com.monkey.ktplus.effects.api.EffectDefinition;
import com.monkey.ktplus.effects.api.KillEffect;
import com.monkey.ktplus.effects.registry.EffectRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class EffectCatalogExporter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JavaPlugin plugin;

    public EffectCatalogExporter(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void export(EffectRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        try {
            File directory = new File(plugin.getDataFolder(), "export");
            if (!directory.exists() && !directory.mkdirs()) {
                plugin.getLogger().warning("[Export] Unable to create export directory");
                return;
            }
            ObjectNode root = MAPPER.createObjectNode();
            root.put("plugin", "KTPlus");
            root.put("version", plugin.getPluginMeta().getVersion());
            ArrayNode effects = root.putArray("effects");
            for (KillEffect effect : registry.all()) {
                EffectDefinition definition = effect.definition();
                ObjectNode node = effects.addObject();
                node.put("id", definition.id());
                node.put("displayName", definition.displayName());
                node.put("icon", definition.icon().name());
                node.put("price", definition.price());
                node.put("heavy", definition.heavy());
                node.put("maxDurationTicks", definition.maxDurationTicks());
                node.put("permission", definition.permissionNode());
            }
            File target = new File(directory, "effects-catalog.json");
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(target, root);
            plugin.getLogger().info("[Export] Wrote " + target.getName() + " (" + effects.size() + " effects)");
        } catch (Throwable error) {
            plugin.getLogger().warning("[Export] Catalog export failed: " + error.getMessage());
        }
    }
}
