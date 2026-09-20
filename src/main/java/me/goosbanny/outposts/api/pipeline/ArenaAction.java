package me.goosbanny.outposts.api.pipeline;

import me.goosbanny.outposts.api.arena.OutpostArena;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * An executable action configured on an outpost trigger (e.g., console command, broadcast, bank deposit).
 */
public interface ArenaAction {

    /**
     * Unique action type identifier (e.g., "BROADCAST", "COMMAND_CONSOLE", "FACTION_BANK_DEPOSIT").
     */
    @NotNull
    String getType();

    /**
     * Executes the action with provided context.
     *
     * @param arena   arena in which the trigger fired
     * @param context contextual parameters (e.g. "team", "player", "amount")
     */
    void execute(@NotNull OutpostArena arena, @NotNull Map<String, Object> context);
}
