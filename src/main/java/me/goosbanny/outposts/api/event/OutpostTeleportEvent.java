package me.goosbanny.outposts.api.event;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player teleports to an outpost via command or portal.
 */
public class OutpostTeleportEvent extends OutpostEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private Location targetLocation;
    private boolean cancelled;

    public OutpostTeleportEvent(@NotNull OutpostArena arena, @NotNull Player player, @NotNull Location targetLocation) {
        super(arena);
        this.player = player;
        this.targetLocation = targetLocation;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Location getTargetLocation() {
        return targetLocation;
    }

    public void setTargetLocation(@NotNull Location targetLocation) {
        this.targetLocation = targetLocation;
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

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
