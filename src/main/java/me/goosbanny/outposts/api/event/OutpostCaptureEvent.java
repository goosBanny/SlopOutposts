package me.goosbanny.outposts.api.event;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired when a team successfully captures an outpost (hits 100% progress).
 * Can be cancelled to prevent ownership transition.
 */
public class OutpostCaptureEvent extends OutpostEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String teamId;
    private final String teamName;
    private final UUID capturer;
    private boolean cancelled;

    public OutpostCaptureEvent(@NotNull OutpostArena arena, @NotNull String teamId, @NotNull String teamName, @Nullable UUID capturer) {
        super(arena);
        this.teamId = teamId;
        this.teamName = teamName;
        this.capturer = capturer;
    }

    @NotNull
    public String getTeamId() {
        return teamId;
    }

    @NotNull
    public String getTeamName() {
        return teamName;
    }

    @Nullable
    public UUID getCapturer() {
        return capturer;
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
