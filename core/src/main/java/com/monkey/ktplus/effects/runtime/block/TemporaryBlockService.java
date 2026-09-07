package com.monkey.ktplus.effects.runtime.block;

import com.monkey.ktplus.storage.repository.TemporaryBlockRepository;
import com.monkey.ktplus.storage.repository.TemporaryBlockRepository.StoredTemporaryBlock;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jspecify.annotations.Nullable;

public final class TemporaryBlockService {
    private final Map<String, TemporaryBlockChange> active = new ConcurrentHashMap<String, TemporaryBlockChange>();
    private final @Nullable TemporaryBlockRepository repository;

    public TemporaryBlockService() {
        this(null);
    }

    public TemporaryBlockService(@Nullable TemporaryBlockRepository repository) {
        this.repository = repository;
    }

    public void restorePersisted() {
        if (repository == null) {
            return;
        }
        for (StoredTemporaryBlock stored : repository.loadAll()) {
            tryRestoreStored(stored);
        }
    }

    public void restoreWorld(World world) {
        Objects.requireNonNull(world, "world");
        if (repository == null) {
            return;
        }
        for (StoredTemporaryBlock stored : repository.loadAll()) {
            if (!world.getName().equals(stored.worldName())) {
                continue;
            }
            tryRestoreStored(stored);
        }
    }

    public TemporaryBlockChange change(Block block, Material material) {
        return change(block, material, false);
    }

    public TemporaryBlockChange change(Block block, Material material, boolean applyPhysics) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(material, "material");
        String key = key(block);
        TemporaryBlockChange existing = active.get(key);
        if (existing != null) {
            block.setType(material, applyPhysics);
            return existing;
        }
        Material original = block.getType();
        String payload = PersistedBlockPayload.capture(block);
        TemporaryBlockChange change = new TemporaryBlockChange(key, block, block.getState());
        active.put(key, change);
        if (repository != null) {
            repository.saveAsync(block.getLocation(), original.name(), payload);
        }
        block.setType(material, applyPhysics);
        return change;
    }

    public boolean isTemporary(Block block) {
        return active.containsKey(key(block));
    }

    public void restore(TemporaryBlockChange change) {
        Objects.requireNonNull(change, "change");
        if (active.remove(change.key()) != null) {
            change.restore();
            if (repository != null) {
                repository.removeAsync(change.block().getLocation());
            }
        }
    }

    public void restoreAll() {
        Collection<TemporaryBlockChange> changes = active.values();
        for (TemporaryBlockChange change : changes) {
            change.restore();
            if (repository != null) {
                repository.remove(change.block().getLocation());
            }
        }
        active.clear();
    }

    public void shutdown() {
        if (repository != null) {
            repository.drainAndShutdown();
        }
    }

    private void tryRestoreStored(StoredTemporaryBlock stored) {
        Location location = stored.toLocation();
        if (location == null) {
            return;
        }
        PersistedBlockPayload.restore(location.getBlock(), stored.material(), stored.blockData());
        if (repository != null) {
            repository.remove(location);
        }
    }

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
