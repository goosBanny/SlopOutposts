package me.goosbanny.outposts.core.pipeline.actions;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.pipeline.ArenaAction;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bulletproof action that dispatches console commands targeting:
 * - PLAYER:     The player who initiated the capture or trigger
 * - LEADER:     The leader/owner of the controlling faction
 * - TEAM:       All online members of the controlling faction
 * - ZONE:       All players currently inside the outpost boundary
 * - TEAM_ZONE:  Teammates currently inside the outpost boundary
 */
public class ConsoleCommandAction implements ArenaAction {

    private final String commandTemplate;
    private final String target;
    private final TeamRosterProvider teamProvider;
    private final FoliaCompatScheduler scheduler;

    public ConsoleCommandAction(
            @NotNull String commandTemplate,
            @Nullable String target,
            @NotNull TeamRosterProvider teamProvider,
            @NotNull FoliaCompatScheduler scheduler
    ) {
        this.commandTemplate = commandTemplate;
        this.target = target != null ? target.toUpperCase() : "PLAYER";
        this.teamProvider = teamProvider;
        this.scheduler = scheduler;
    }

    @Override
    public @NotNull String getType() {
        return "COMMAND_CONSOLE";
    }

    @Override
    public void execute(@NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
        if ("PER_TEAM".equalsIgnoreCase(target) || "TEAM_CONSOLE".equalsIgnoreCase(target)) {
            dispatchGlobal(arena, context, (String) context.get("player"));
            return;
        }

        List<Player> targets = me.goosbanny.outposts.core.pipeline.ActionTargetResolver.resolvePlayers(
                target, "PLAYER", arena, context, teamProvider
        );

        if (!targets.isEmpty()) {
            for (Player p : targets) {
                if (p != null && p.isOnline()) {
                    dispatchForPlayer(p, arena, context);
                }
            }
        } else {
            String playerName = (String) context.get("player");
            dispatchGlobal(arena, context, playerName);
        }
    }

    private void dispatchForPlayer(@NotNull Player player, @NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
        scheduler.runForEntity(player, () -> {
            String cmd = format(commandTemplate, arena, context, player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        });
    }

    private void dispatchGlobal(@NotNull OutpostArena arena, @NotNull Map<String, Object> context, @Nullable String playerName) {
        scheduler.runGlobal(() -> {
            String cmd = format(commandTemplate, arena, context, playerName != null ? playerName : "");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        });
    }

    private String format(String template, OutpostArena arena, Map<String, Object> context, String playerName) {
        String cmd = template;
        cmd = cmd.replace("<id>", arena.getId());
        cmd = cmd.replace("<name>", net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(arena.getDisplayName()));
        String team = (String) context.getOrDefault("team", arena.getControllerTeamName());
        if (team != null) {
            cmd = cmd.replace("%team%", team).replace("<team>", team);
        }
        if (playerName != null) {
            cmd = cmd.replace("%player%", playerName).replace("<player>", playerName);
        }
        String prevTeam = (String) context.get("previous_team");
        if (prevTeam != null) {
            cmd = cmd.replace("%previous_team%", prevTeam).replace("<previous_team>", prevTeam);
        }
        String invader = (String) context.get("invader");
        if (invader != null) {
            cmd = cmd.replace("%invader%", invader).replace("<invader>", invader);
        }
        Object held = context.get("seconds_held");
        if (held != null) {
            cmd = cmd.replace("<seconds_held>", String.valueOf(held));
        }
        cmd = cmd.replace("%percent%", String.format("%.1f", arena.getProgress()));
        cmd = cmd.replace("%world%", arena.getWorldName());
        return cmd;
    }
}
