package me.goosbanny.outposts.core.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Threading and scheduling abstraction layer providing native Folia region-scheduler compatibility
 * with seamless fallback to Paper/Spigot single-thread schedulers.
 */
public class FoliaCompatScheduler {

    private static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
        IS_FOLIA = folia;
    }

    private final Plugin plugin;

    public FoliaCompatScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if the server environment is running Folia.
     */
    public static boolean isFolia() {
        return IS_FOLIA;
    }

    /**
     * Runs a task globally (or on the main thread if not Folia).
     */
    public void runGlobal(@NotNull Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Runs a repeating task globally.
     */
    public TaskHandle runGlobalTimer(long initialDelayTicks, long periodTicks, @NotNull Consumer<TaskHandle> taskConsumer) {
        if (IS_FOLIA) {
            final TaskHandle[] handleHolder = new TaskHandle[1];
            ScheduledTask foliaTask =
                    Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                            plugin,
                            t -> taskConsumer.accept(handleHolder[0]),
                            Math.max(1, initialDelayTicks),
                            Math.max(1, periodTicks)
                    );
            handleHolder[0] = new TaskHandle(foliaTask::cancel, foliaTask::isCancelled);
            return handleHolder[0];
        } else {
            final TaskHandle[] handleHolder = new TaskHandle[1];
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> taskConsumer.accept(handleHolder[0]),
                    initialDelayTicks,
                    periodTicks
            );
            handleHolder[0] = new TaskHandle(bukkitTask::cancel, bukkitTask::isCancelled);
            return handleHolder[0];
        }
    }

    /**
     * Runs a task at a specific world chunk location on the owning thread region.
     */
    public void runAtLocation(@NotNull Location location, @NotNull Runnable runnable) {
        if (location.getWorld() == null) {
            runGlobal(runnable);
            return;
        }
        if (IS_FOLIA) {
            Bukkit.getRegionScheduler().execute(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Runs a repeating task at a specific world chunk location on the owning thread region.
     */
    public TaskHandle runTimerAtLocation(@NotNull World world, int chunkX, int chunkZ, long initialDelayTicks, long periodTicks, @NotNull Consumer<TaskHandle> taskConsumer) {
        if (IS_FOLIA) {
            final TaskHandle[] handleHolder = new TaskHandle[1];
            ScheduledTask foliaTask =
                    Bukkit.getRegionScheduler().runAtFixedRate(
                            plugin,
                            world,
                            chunkX,
                            chunkZ,
                            t -> taskConsumer.accept(handleHolder[0]),
                            Math.max(1, initialDelayTicks),
                            Math.max(1, periodTicks)
                    );
            handleHolder[0] = new TaskHandle(foliaTask::cancel, foliaTask::isCancelled);
            return handleHolder[0];
        } else {
            final TaskHandle[] handleHolder = new TaskHandle[1];
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> taskConsumer.accept(handleHolder[0]),
                    initialDelayTicks,
                    periodTicks
            );
            handleHolder[0] = new TaskHandle(bukkitTask::cancel, bukkitTask::isCancelled);
            return handleHolder[0];
        }
    }

    /**
     * Runs a task bound to an entity (using Paper's EntityScheduler).
     * Prevents cross-region threading exceptions when interacting with remote players.
     */
    public void runForEntity(@NotNull Entity entity, @NotNull Runnable runnable) {
        try {
            entity.getScheduler().execute(plugin, runnable, null, 1L);
        } catch (NoSuchMethodError | Exception e) {
            // Fallback to global scheduler or main thread
            runGlobal(runnable);
        }
    }

    /**
     * Checks if an entity is owned by the current thread region (always true on Paper/Spigot).
     */
    public static boolean isOwnedByCurrentRegion(@NotNull Entity entity) {
        if (!IS_FOLIA) return true;
        try {
            return Bukkit.isOwnedByCurrentRegion(entity);
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Checks if a location is owned by the current thread region (always true on Paper/Spigot).
     */
    public static boolean isOwnedByCurrentRegion(@NotNull Location location) {
        if (!IS_FOLIA) return true;
        if (location.getWorld() == null) return false;
        try {
            return Bukkit.isOwnedByCurrentRegion(location);
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Runs a task asynchronously.
     */
    public void runAsync(@NotNull Runnable runnable) {
        try {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } catch (NoSuchMethodError | Exception e) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    /**
     * Unified handle to cancel scheduled timer tasks.
     */
    public static class TaskHandle {
        private final Runnable cancelAction;
        private final BooleanSupplier isCancelledAction;

        public TaskHandle(Runnable cancelAction, BooleanSupplier isCancelledAction) {
            this.cancelAction = cancelAction;
            this.isCancelledAction = isCancelledAction;
        }

        public void cancel() {
            cancelAction.run();
        }

        public boolean isCancelled() {
            return isCancelledAction.getAsBoolean();
        }
    }
}
