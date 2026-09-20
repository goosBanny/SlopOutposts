package me.goosbanny.outposts.api.arena;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines whether an outpost arena is fought over and controlled by entire teams/factions
 * or by individual solo players.
 */
public enum OccupancyMode {
    /**
     * Fought over and controlled by teams or factions (BetterTeams, Factions, Towny, ZelTeams, Scoreboards).
     */
    TEAM,

    /**
     * Fought over and controlled by individual solo players.
     * Every player competes for themselves regardless of team affiliation.
     */
    SOLO;

    /**
     * Safely parses an OccupancyMode from string, defaulting to TEAM if invalid or null.
     */
    @NotNull
    public static OccupancyMode fromString(@Nullable String value) {
        if (value == null) return TEAM;
        try {
            return OccupancyMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TEAM;
        }
    }
}
