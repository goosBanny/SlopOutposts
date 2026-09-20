package me.goosbanny.outposts.api.pipeline;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Predicate condition evaluated before an action is executed.
 */
@FunctionalInterface
public interface ActionCondition {

    /**
     * Evaluates whether the condition passes.
     *
     * @param arena   arena context
     * @param context dynamic execution context variables
     * @return true if allowed to proceed
     */
    boolean evaluate(@NotNull OutpostArena arena, @NotNull Map<String, Object> context);
}
