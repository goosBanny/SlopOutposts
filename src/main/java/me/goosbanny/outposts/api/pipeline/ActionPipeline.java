package me.goosbanny.outposts.api.pipeline;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Registry and dispatcher for outpost actions and triggers.
 */
public interface ActionPipeline {

    /**
     * Dispatches all actions configured for a specific trigger on the arena.
     *
     * @param trigger the event trigger
     * @param arena   the outpost arena
     * @param context contextual parameters
     */
    void dispatch(@NotNull ActionTrigger trigger, @NotNull OutpostArena arena, @NotNull Map<String, Object> context);

    /**
     * Executes a list of action nodes in sequence.
     *
     * @param actions list of actions
     * @param arena   the outpost arena
     * @param context contextual parameters
     */
    void executeActions(@NotNull List<ArenaAction> actions, @NotNull OutpostArena arena, @NotNull Map<String, Object> context);
}
