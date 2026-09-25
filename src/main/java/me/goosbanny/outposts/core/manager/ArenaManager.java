package me.goosbanny.outposts.core.manager;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central lifecycle and runtime registry for all loaded Outpost arenas.
 * Provides native Folia multi-threaded region scheduling with seamless Paper fallback.
 */
public class ArenaManager {

    private final SpatialGridManager spatialGridManager;
    private final FoliaCompatScheduler scheduler;
    private final Map<String, OutpostArena> arenaMap = new ConcurrentHashMap<>();
    private final Map<String, FoliaCompatScheduler.TaskHandle> arenaTaskMap = new ConcurrentHashMap<>();

    private FoliaCompatScheduler.TaskHandle globalGameLoopTask;
    private boolean ticking = false;
    private long tickPeriod = 20L;

    public ArenaManager(@NotNull SpatialGridManager spatialGridManager, @NotNull FoliaCompatScheduler scheduler) {
        this.spatialGridManager = spatialGridManager;
        this.scheduler = scheduler;
    }

    public synchronized void registerArena(@NotNull OutpostArena arena) {
        String key = arena.getId().toLowerCase();
        arenaMap.put(key, arena);
        spatialGridManager.registerArena(arena);

        if (ticking && FoliaCompatScheduler.isFolia()) {
            startArenaRegionTask(arena);
        }
    }

    /**
     * Re-indexes spatial chunks and reschedules Folia region tasks when an arena shifts locations.
     */
    public synchronized void notifyArenaRelocated(@NotNull OutpostArena arena, @NotNull Runnable geometryUpdate) {
        spatialGridManager.unregisterArena(arena);
        geometryUpdate.run();
        spatialGridManager.registerArena(arena);

        if (ticking && FoliaCompatScheduler.isFolia()) {
            String key = arena.getId().toLowerCase();
            FoliaCompatScheduler.TaskHandle oldTask = arenaTaskMap.remove(key);
            if (oldTask != null) {
                oldTask.cancel();
            }
            startArenaRegionTask(arena);
        }
    }

    @Nullable
    public synchronized OutpostArena unregisterArena(@NotNull String id) {
        String key = id.toLowerCase();
        FoliaCompatScheduler.TaskHandle task = arenaTaskMap.remove(key);
        if (task != null) {
            task.cancel();
        }

        OutpostArena arena = arenaMap.remove(key);
        if (arena != null) {
            spatialGridManager.unregisterArena(arena);
        }
        return arena;
    }

    @Nullable
    public OutpostArena getArena(@NotNull String id) {
        return arenaMap.get(id.toLowerCase());
    }

    @NotNull
    public Collection<OutpostArena> getArenas() {
        return Collections.unmodifiableCollection(arenaMap.values());
    }

    public boolean hasArena(@NotNull String id) {
        return arenaMap.containsKey(id.toLowerCase());
    }

    public long getTickPeriod() {
        return tickPeriod;
    }

    /**
     * Starts the arena game loop across all registered arenas.
     *
     * @param frequencyTicks tick interval (e.g. 20 ticks for 1 second)
     */
    public synchronized void startTicking(long frequencyTicks) {
        if (ticking) return;
        ticking = true;
        this.tickPeriod = Math.max(1, frequencyTicks);

        if (FoliaCompatScheduler.isFolia()) {
            for (OutpostArena arena : arenaMap.values()) {
                startArenaRegionTask(arena);
            }
        } else {
            globalGameLoopTask = scheduler.runGlobalTimer(tickPeriod, tickPeriod, handle -> {
                for (OutpostArena arena : arenaMap.values()) {
                    try {
                        arena.tickGameLoop();
                    } catch (Exception e) {
                        // Prevent single arena crash from aborting loop
                    }
                }
            });
        }
    }

    private void startArenaRegionTask(@NotNull OutpostArena arena) {
        String key = arena.getId().toLowerCase();
        FoliaCompatScheduler.TaskHandle existing = arenaTaskMap.remove(key);
        if (existing != null) {
            existing.cancel();
        }

        World world = Bukkit.getWorld(arena.getWorldName());
        if (world == null) {
            // World not yet loaded; fallback to global timer until world becomes available
            FoliaCompatScheduler.TaskHandle fallback = scheduler.runGlobalTimer(tickPeriod, tickPeriod, handle -> {
                World w = Bukkit.getWorld(arena.getWorldName());
                if (w != null) {
                    handle.cancel();
                    startArenaRegionTask(arena);
                }
            });
            arenaTaskMap.put(key, fallback);
            return;
        }

        int centerChunkX = ((arena.getMinX() + arena.getMaxX()) / 2) >> 4;
        int centerChunkZ = ((arena.getMinZ() + arena.getMaxZ()) / 2) >> 4;

        FoliaCompatScheduler.TaskHandle handle = scheduler.runTimerAtLocation(
                world, centerChunkX, centerChunkZ, tickPeriod, tickPeriod,
                taskHandle -> {
                    try {
                        arena.tickGameLoop();
                    } catch (Exception ignored) {
                    }
                }
        );
        arenaTaskMap.put(key, handle);
    }

    /**
     * Stops the arena game loop.
     */
    public synchronized void stopTicking() {
        if (!ticking) return;
        ticking = false;

        if (globalGameLoopTask != null) {
            globalGameLoopTask.cancel();
            globalGameLoopTask = null;
        }

        for (FoliaCompatScheduler.TaskHandle handle : arenaTaskMap.values()) {
            if (handle != null) {
                handle.cancel();
            }
        }
        arenaTaskMap.clear();
    }

    public synchronized void clear() {
        stopTicking();
        for (OutpostArena arena : arenaMap.values()) {
            spatialGridManager.unregisterArena(arena);
        }
        arenaMap.clear();
    }
}
