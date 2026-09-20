package me.goosbanny.outposts.api.mechanics;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Strategy interface for pluggable territory capture algorithms.
 */
public interface CaptureModeEngine {

    /**
     * Gets the mode type handled by this engine implementation.
     *
     * @return CaptureModeType
     */
    @NotNull
    CaptureModeType getType();

    /**
     * Evaluates capture progression or regression during an active game tick.
     *
     * @param arena        the arena being ticked
     * @param validCappers validated players inside the capture pad
     * @param isContested  whether two or more opposing teams contest the zone
     */
    void evaluateCapture(@NotNull OutpostArena arena, @NotNull List<Player> validCappers, boolean isContested);

    /**
     * Invoked when no valid cappers occupy the capture zone.
     * Allows mechanics like passive decay to bleed capture progress back to neutral.
     *
     * @param arena the arena being ticked
     */
    void handleAbandonment(@NotNull OutpostArena arena);
}
