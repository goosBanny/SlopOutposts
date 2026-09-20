package me.goosbanny.outposts.api.mechanics;

/**
 * Supported capture strategies for Outpost arenas.
 */
public enum CaptureModeType {
    /**
     * Classical King-of-the-Hill: progress rises 0% -> 100%. Invaders knock defenders down to 0% first.
     */
    STANDARD_HILL,

    /**
     * Symmetrical 50% neutral anchor: teams pull progress toward extremes based on active player ratio.
     */
    TUG_OF_WAR,

    /**
     * Holding a secured point accumulates tickets toward a target victory score.
     */
    TICKET_ACCUMULATION,

    /**
     * Captures bleed progress back to 0% when the zone is vacated.
     */
    PASSIVE_DECAY
}
