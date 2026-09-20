package me.goosbanny.outposts.api.mechanics;

/**
 * Team assignment policies for the Tug of War capture engine.
 */
public enum TugOfWarTeamAssignment {
    /**
     * Symmetrical duel between the first two opposing factions to contest the pad.
     * Third-party factions cannot pull until ownership or neutrality resets.
     */
    FIRST_TWO_FACTIONS,

    /**
     * Automatic balanced matchmaking into RED vs BLUE teams.
     * Ideal for Solo / FFA / minigame arenas.
     */
    AUTO_RED_BLUE
}
