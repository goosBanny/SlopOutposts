package me.goosbanny.outposts.api.arena;

import net.kyori.adventure.text.Component;

/**
 * Thread-safe read-only snapshot DTO for PlaceholderAPI expansions, GUIs, webhooks, and external queries.
 */
public interface ArenaView {

    /**
     * Arena ID.
     */
    String getId();

    /**
     * Arena display name.
     */
    Component getDisplayName();

    /**
     * Current lifecycle state of the arena.
     */
    ArenaState getState();

    /**
     * Capture progress percentage (0.0 to 100.0).
     */
    double getProgress();

    /**
     * Identifier or display name of the controlling team, or null if neutral.
     */
    String getControllerTeam();

    /**
     * Identifier or display name of the currently capping team, or null if none.
     */
    String getCappingTeam();

    /**
     * Number of valid cappers currently standing inside the zone.
     */
    int getCapperCount();

    /**
     * True if two or more opposing teams contest the zone.
     */
    boolean isContested();

    /**
     * Lockout seconds remaining after a capture, or 0 if unlocked.
     */
    long getLockoutRemainingSeconds();

    /**
     * Occupancy mode: TEAM or SOLO.
     */
    OccupancyMode getOccupancyMode();
}
