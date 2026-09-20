package me.goosbanny.outposts.api.event;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player's capture contribution is rejected by the anti-cheese engine.
 */
public class OutpostAntiCheeseTriggerEvent extends OutpostEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Reason code for anti-cheese disqualification.
     */
    public enum ViolationType {
        NO_LINE_OF_SIGHT,
        COMBAT_PAUSED,
        COMBAT_RESET,
        FLYING,
        ELYTRA_GLIDE,
        GODMODE,
        VANISHED,
        ALLIED_STALL,
        TEAM_LIMIT_EXCEEDED
    }

    private final Player player;
    private final ViolationType violationType;

    public OutpostAntiCheeseTriggerEvent(@NotNull OutpostArena arena, @NotNull Player player, @NotNull ViolationType violationType) {
        super(arena);
        this.player = player;
        this.violationType = violationType;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public ViolationType getViolationType() {
        return violationType;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
