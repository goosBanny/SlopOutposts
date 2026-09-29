package me.goosbanny.outposts.core.pipeline;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.pipeline.ArenaAction;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Decorator action that filters execution based on secondary hold intervals (e.g. every 300 seconds / 5 minutes).
 */
public class IntervalFilteredAction implements ArenaAction {

    private final ArenaAction delegate;
    private final int intervalSeconds;

    public IntervalFilteredAction(@NotNull ArenaAction delegate, int intervalSeconds) {
        this.delegate = delegate;
        this.intervalSeconds = intervalSeconds;
    }

    @Override
    public @NotNull String getType() {
        return delegate.getType();
    }

    @NotNull
    public ArenaAction getDelegate() {
        return delegate;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    @Override
    public void execute(@NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
        if (intervalSeconds > 0) {
            Object heldObj = context.get("seconds_held");
            if (heldObj instanceof Number num) {
                long secondsHeld = num.longValue();
                if (secondsHeld <= 0 || secondsHeld % intervalSeconds != 0) {
                    return;
                }
            }
        }
        delegate.execute(arena, context);
    }
}
