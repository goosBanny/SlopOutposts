package me.goosbanny.outposts.core.pipeline;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.pipeline.ActionPipeline;
import me.goosbanny.outposts.api.pipeline.ActionTrigger;
import me.goosbanny.outposts.api.pipeline.ArenaAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard implementation of ActionPipeline, storing and executing registered action chains per trigger.
 */
public class DefaultActionPipeline implements ActionPipeline {

    private final Map<ActionTrigger, List<ArenaAction>> triggerActions = new ConcurrentHashMap<>();

    public void setActionsForTrigger(@NotNull ActionTrigger trigger, @NotNull List<ArenaAction> actions) {
        triggerActions.put(trigger, new ArrayList<>(actions));
    }

    public void addAction(@NotNull ActionTrigger trigger, @NotNull ArenaAction action) {
        triggerActions.computeIfAbsent(trigger, k -> new ArrayList<>()).add(action);
    }

    public List<ArenaAction> getActions(@NotNull ActionTrigger trigger) {
        return triggerActions.getOrDefault(trigger, Collections.emptyList());
    }

    @Override
    public void dispatch(@NotNull ActionTrigger trigger, @NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
        List<ArenaAction> actions = triggerActions.get(trigger);
        if (actions != null && !actions.isEmpty()) {
            executeActions(actions, arena, context);
        }
    }

    @Override
    public void executeActions(@NotNull List<ArenaAction> actions, @NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
        for (ArenaAction action : actions) {
            try {
                action.execute(arena, context);
            } catch (Exception e) {
                // Log or gracefully prevent one action from breaking the rest
            }
        }
    }
}
