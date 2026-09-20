package me.goosbanny.outposts.api.event;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.core.arena.ArenaRegion;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a dynamic outpost successfully shifts to a new region.
 */
public class OutpostShiftEvent extends OutpostEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ArenaRegion fromRegion;
    private final ArenaRegion toRegion;

    public OutpostShiftEvent(@NotNull OutpostArena arena, @Nullable ArenaRegion fromRegion, @NotNull ArenaRegion toRegion) {
        super(arena);
        this.fromRegion = fromRegion;
        this.toRegion = toRegion;
    }

    @Nullable
    public ArenaRegion getFromRegion() {
        return fromRegion;
    }

    @NotNull
    public ArenaRegion getToRegion() {
        return toRegion;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
