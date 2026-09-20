package me.goosbanny.outposts.api.pipeline;

/**
 * Event triggers that can execute action pipelines in an arena.
 */
public enum ActionTrigger {
    ON_CAPTURE,
    ON_LOST,
    ON_TICK_REWARD,
    ON_CONTEST
}
