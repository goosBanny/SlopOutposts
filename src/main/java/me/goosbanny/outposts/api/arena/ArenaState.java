package me.goosbanny.outposts.api.arena;

/**
 * Represents the lifecycle and operational state of an Outpost arena.
 */
public enum ArenaState {
    /**
     * Arena has no controlling team and is waiting for cappers.
     */
    NEUTRAL,

    /**
     * Arena is actively being captured from neutral by an un-contested team.
     */
    CAPTURING,

    /**
     * Arena is held by a controlling team with full benefits active.
     */
    CONTROLLED,

    /**
     * Two or more rival teams/alliances are currently inside the capture zone.
     */
    CONTESTED,

    /**
     * Arena is temporarily locked following a capture or scheduled downtime.
     */
    LOCKED
}
