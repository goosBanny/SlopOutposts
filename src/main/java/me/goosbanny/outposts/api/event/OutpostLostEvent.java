package me.goosbanny.outposts.api.event;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a team loses control of an outpost.
 */
public class OutpostLostEvent extends OutpostEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String lostTeamId;
    private final String lostTeamName;

    public OutpostLostEvent(@NotNull OutpostArena arena, @Nullable String lostTeamId, @Nullable String lostTeamName) {
        super(arena);
        this.lostTeamId = lostTeamId;
        this.lostTeamName = lostTeamName;
    }

    @Nullable
    public String getLostTeamId() {
        return lostTeamId;
    }

    @Nullable
    public String getLostTeamName() {
        return lostTeamName;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
