package me.goosbanny.outposts.api.event;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.core.arena.ArenaRegion;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired immediately before a dynamic outpost shifts to a new region.
 * Can be cancelled to prevent the relocation.
 */
public class OutpostPreShiftEvent extends OutpostEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ArenaRegion fromRegion;
    private ArenaRegion toRegion;
    private boolean cancelled;

    public OutpostPreShiftEvent(@NotNull OutpostArena arena, @Nullable ArenaRegion fromRegion, @NotNull ArenaRegion toRegion) {
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

    public void setToRegion(@NotNull ArenaRegion toRegion) {
        this.toRegion = toRegion;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
