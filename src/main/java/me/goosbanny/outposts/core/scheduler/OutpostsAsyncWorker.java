package me.goosbanny.outposts.core.scheduler;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated single-threaded background worker for Outposts.
 * Offloads UI/HUD rendering, MiniMessage string compiling, async tasks,
 * and message dispatch off the main/region server threads.
 */
public class OutpostsAsyncWorker {

    private final ScheduledExecutorService executor;

    public OutpostsAsyncWorker() {
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "outposts-async-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void execute(@NotNull Runnable task) {
        if (!executor.isShutdown()) {
            executor.execute(task);
        }
    }

    public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable task, long initialDelay, long period, @NotNull TimeUnit unit) {
        return executor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
