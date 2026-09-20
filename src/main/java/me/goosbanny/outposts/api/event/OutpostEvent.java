package me.goosbanny.outposts.api.event;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Base event class for all Outposts events.
 */
public abstract class OutpostEvent extends Event {

    protected final OutpostArena arena;

    protected OutpostEvent(@NotNull OutpostArena arena) {
        this.arena = arena;
    }

    protected OutpostEvent(@NotNull OutpostArena arena, boolean isAsync) {
        super(isAsync);
        this.arena = arena;
    }

    /**
     * Gets the arena associated with this event.
     *
     * @return OutpostArena
     */
    @NotNull
    public OutpostArena getArena() {
        return arena;
    }
}
