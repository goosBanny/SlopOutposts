package me.goosbanny.outposts.api.event;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when capture progress changes on an arena.
 */
public class OutpostProgressChangeEvent extends OutpostEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final double oldProgress;
    private final double newProgress;

    public OutpostProgressChangeEvent(@NotNull OutpostArena arena, double oldProgress, double newProgress) {
        super(arena);
        this.oldProgress = oldProgress;
        this.newProgress = newProgress;
    }

    public double getOldProgress() {
        return oldProgress;
    }

    public double getNewProgress() {
        return newProgress;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
