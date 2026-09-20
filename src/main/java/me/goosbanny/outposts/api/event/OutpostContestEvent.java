package me.goosbanny.outposts.api.event;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when an outpost transitions into or out of a CONTESTED state.
 */
public class OutpostContestEvent extends OutpostEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean contested;

    public OutpostContestEvent(@NotNull OutpostArena arena, boolean contested) {
        super(arena);
        this.contested = contested;
    }

    public boolean isContested() {
        return contested;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
