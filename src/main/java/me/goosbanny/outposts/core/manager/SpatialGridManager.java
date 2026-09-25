package me.goosbanny.outposts.core.manager;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.goosbanny.outposts.api.arena.OutpostArena;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Zero-allocation spatial grid indexer for Outpost arenas.
 * Leverages FastUtil primitive bitwise maps and primitive AABB collision math.
 * Fully multi-world isolated and thread-safe.
 */
public class SpatialGridManager {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    // Maps world name -> packed chunk key -> set of arenas overlapping that chunk
    private final Map<String, Long2ObjectOpenHashMap<Set<OutpostArena>>> worldToChunkArenas = new HashMap<>();

    /**
     * Bitwise packing of chunk X and Z into a 64-bit primitive long.
     *
     * @param chunkX chunk coordinate X
     * @param chunkZ chunk coordinate Z
     * @return packed 64-bit primitive key
     */
    public static long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    /**
     * Extracts chunk X from a packed 64-bit chunk key.
     */
    public static int getChunkX(long chunkKey) {
        return (int) (chunkKey & 0xFFFFFFFFL);
    }

    /**
     * Extracts chunk Z from a packed 64-bit chunk key.
     */
    public static int getChunkZ(long chunkKey) {
        return (int) ((chunkKey >>> 32) & 0xFFFFFFFFL);
    }

    /**
     * Registers an outpost arena across all chunks spanned by its cuboid boundaries.
     *
     * @param arena arena to register
     */
    public void registerArena(@NotNull OutpostArena arena) {
        int minChunkX = arena.getMinX() >> 4;
        int maxChunkX = arena.getMaxX() >> 4;
        int minChunkZ = arena.getMinZ() >> 4;
        int maxChunkZ = arena.getMaxZ() >> 4;
        String world = arena.getWorldName().toLowerCase();

        lock.writeLock().lock();
        try {
            Long2ObjectOpenHashMap<Set<OutpostArena>> worldMap = worldToChunkArenas.computeIfAbsent(world, k -> new Long2ObjectOpenHashMap<>());
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    long key = getChunkKey(cx, cz);
                    worldMap.computeIfAbsent(key, k -> new HashSet<>()).add(arena);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deregisters an outpost arena from all indexed chunks.
     *
     * @param arena arena to remove
     */
    public void unregisterArena(@NotNull OutpostArena arena) {
        int minChunkX = arena.getMinX() >> 4;
        int maxChunkX = arena.getMaxX() >> 4;
        int minChunkZ = arena.getMinZ() >> 4;
        int maxChunkZ = arena.getMaxZ() >> 4;
        String world = arena.getWorldName().toLowerCase();

        lock.writeLock().lock();
        try {
            Long2ObjectOpenHashMap<Set<OutpostArena>> worldMap = worldToChunkArenas.get(world);
            if (worldMap != null) {
                for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                    for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                        long key = getChunkKey(cx, cz);
                        Set<OutpostArena> wSet = worldMap.get(key);
                        if (wSet != null) {
                            wSet.remove(arena);
                            if (wSet.isEmpty()) {
                                worldMap.remove(key);
                            }
                        }
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Zero-allocation primitive AABB point query to determine which arena contains a location.
     *
     * @param worldName world identifier
     * @param x         block X
     * @param y         block Y
     * @param z         block Z
     * @return matching OutpostArena or null
     */
    @Nullable
    public OutpostArena getArenaAt(String worldName, int x, int y, int z) {
        if (worldName == null) return null;
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long chunkKey = getChunkKey(chunkX, chunkZ);
        String worldKey = worldName.toLowerCase();

        lock.readLock().lock();
        try {
            Long2ObjectOpenHashMap<Set<OutpostArena>> worldMap = worldToChunkArenas.get(worldKey);
            if (worldMap == null) {
                return null;
            }
            Set<OutpostArena> arenas = worldMap.get(chunkKey);
            if (arenas == null || arenas.isEmpty()) {
                return null;
            }

            for (OutpostArena arena : arenas) {
                if (!arena.getWorldName().equalsIgnoreCase(worldName)) {
                    continue;
                }
                // Primitive AABB test
                if (x >= arena.getMinX() && x <= arena.getMaxX()
                        && y >= arena.getMinY() && y <= arena.getMaxY()
                        && z >= arena.getMinZ() && z <= arena.getMaxZ()) {
                    return arena;
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns all arenas that overlap the given chunk in a specific world.
     */
    @NotNull
    public Set<OutpostArena> getArenasInChunk(@NotNull String worldName, int chunkX, int chunkZ) {
        long chunkKey = getChunkKey(chunkX, chunkZ);
        String worldKey = worldName.toLowerCase();
        lock.readLock().lock();
        try {
            Long2ObjectOpenHashMap<Set<OutpostArena>> worldMap = worldToChunkArenas.get(worldKey);
            if (worldMap == null) return Collections.emptySet();
            Set<OutpostArena> arenas = worldMap.get(chunkKey);
            if (arenas == null || arenas.isEmpty()) {
                return Collections.emptySet();
            }
            return new HashSet<>(arenas);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns all arenas that overlap the given chunk across all worlds (legacy fallback).
     */
    @NotNull
    public Set<OutpostArena> getArenasInChunk(int chunkX, int chunkZ) {
        long chunkKey = getChunkKey(chunkX, chunkZ);
        lock.readLock().lock();
        try {
            Set<OutpostArena> result = new HashSet<>();
            for (Long2ObjectOpenHashMap<Set<OutpostArena>> worldMap : worldToChunkArenas.values()) {
                Set<OutpostArena> arenas = worldMap.get(chunkKey);
                if (arenas != null) {
                    result.addAll(arenas);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears all spatial indexes.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            worldToChunkArenas.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
