package com.monkey.ktplus.storage.repository;

import com.monkey.ktplus.storage.DatabaseService;
import com.monkey.ktplus.storage.schema.SchemaColumnProbe;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;

public final class TemporaryBlockRepository {
    private final DatabaseService database;
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "KTPlus-TempBlocks");
        thread.setDaemon(true);
        return thread;
    });

    public TemporaryBlockRepository(DatabaseService database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public void saveAsync(Location location, String originalMaterial, String blockData) {
        Location snapshot = location.clone();
        io.execute(() -> save(snapshot, originalMaterial, blockData));
    }

    public void removeAsync(Location location) {
        Location snapshot = location.clone();
        io.execute(() -> remove(snapshot));
    }

    public void save(Location location, String originalMaterial, String blockData) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(originalMaterial, "originalMaterial");
        Objects.requireNonNull(blockData, "blockData");
        if (location.getWorld() == null) {
            return;
        }
        remove(location);
        database.update(
                "INSERT INTO kt_temp_blocks (world, x, y, z, material, block_data) VALUES (?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setString(1, location.getWorld().getName());
                    statement.setInt(2, location.getBlockX());
                    statement.setInt(3, location.getBlockY());
                    statement.setInt(4, location.getBlockZ());
                    statement.setString(5, originalMaterial);
                    statement.setString(6, blockData);
                });
    }

    public void remove(Location location) {
        Objects.requireNonNull(location, "location");
        if (location.getWorld() == null) {
            return;
        }
        database.update(
                "DELETE FROM kt_temp_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?",
                statement -> {
                    statement.setString(1, location.getWorld().getName());
                    statement.setInt(2, location.getBlockX());
                    statement.setInt(3, location.getBlockY());
                    statement.setInt(4, location.getBlockZ());
                });
    }

    public List<StoredTemporaryBlock> loadAll() {
        SchemaColumnProbe probe = new SchemaColumnProbe(database);
        if (probe.hasColumn("kt_temp_blocks", "block_data")) {
            return database.queryList(
                    "SELECT world, x, y, z, material, block_data FROM kt_temp_blocks",
                    null,
                    resultSet -> new StoredTemporaryBlock(
                            resultSet.getString("world"),
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z"),
                            resultSet.getString("material"),
                            resultSet.getString("block_data")));
        }
        return database.queryList(
                "SELECT world, x, y, z, material FROM kt_temp_blocks",
                null,
                resultSet -> new StoredTemporaryBlock(
                        resultSet.getString("world"),
                        resultSet.getInt("x"),
                        resultSet.getInt("y"),
                        resultSet.getInt("z"),
                        resultSet.getString("material"),
                        null));
    }

    public void clearAll() {
        database.update("DELETE FROM kt_temp_blocks", null);
    }

    public void shutdown() {
        io.shutdownNow();
    }

    public void drainAndShutdown() {
        io.shutdown();
        try {
            if (!io.awaitTermination(5L, java.util.concurrent.TimeUnit.SECONDS)) {
                io.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            io.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    
    public void drainPendingWrites(long timeout, java.util.concurrent.TimeUnit unit)
            throws InterruptedException, java.util.concurrent.TimeoutException, java.util.concurrent.ExecutionException {
        java.util.concurrent.Future<?> barrier = io.submit(() -> {
        });
        barrier.get(timeout, unit);
    }

    public static final class StoredTemporaryBlock {
        private final String world;
        private final int x;
        private final int y;
        private final int z;
        private final String material;
        private final @Nullable String blockData;

        public StoredTemporaryBlock(
                String world, int x, int y, int z, String material, @Nullable String blockData) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
            this.blockData = blockData;
        }

        public String worldName() {
            return world;
        }

        public @Nullable Location toLocation() {
            World bukkitWorld = Bukkit.getWorld(world);
            if (bukkitWorld == null) {
                return null;
            }
            return new Location(bukkitWorld, x, y, z);
        }

        public Material material() {
            Material matched = Material.matchMaterial(material);
            return matched == null ? Material.AIR : matched;
        }

        public @Nullable String blockData() {
            return blockData;
        }
    }
}
