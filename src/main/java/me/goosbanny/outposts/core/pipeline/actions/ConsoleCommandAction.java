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
        String controllerId = (String) context.getOrDefault("team_id", arena.getControllerTeamId());

        boolean isSolo = arena.getOccupancyMode() == OccupancyMode.SOLO;

        switch (target) {
            case "PER_TEAM", "TEAM_CONSOLE" -> {
                // Dispatches once to global console on behalf of the team
                dispatchGlobal(arena, context, (String) context.get("player"));
            }
            case "LEADER" -> {
                if (controllerId == null) return;
                if (isSolo) {
                    try {
                        Player soloPlayer = Bukkit.getPlayer(UUID.fromString(controllerId));
                        if (soloPlayer != null && soloPlayer.isOnline()) {
                            dispatchForPlayer(soloPlayer, arena, context);
                            return;
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                List<Player> members = teamProvider.getOnlineMembersById(controllerId);
                if (!members.isEmpty()) {
                    Player sample = members.get(0);
                    UUID leaderUuid = teamProvider.getTeamLeader(sample);
                    if (leaderUuid != null) {
                        Player leader = Bukkit.getPlayer(leaderUuid);
                        if (leader != null && leader.isOnline()) {
                            dispatchForPlayer(leader, arena, context);
                            return;
                        }
                    }
                }
                // Fallback if leader is offline or UUID is string
                dispatchGlobal(arena, context, null);
            }
            case "PER_PLAYER", "TEAM", "TEAM_ONLINE" -> {
                if (controllerId == null) return;
                if (isSolo) {
                    try {
                        Player soloPlayer = Bukkit.getPlayer(UUID.fromString(controllerId));
                        if (soloPlayer != null && soloPlayer.isOnline()) {
                            dispatchForPlayer(soloPlayer, arena, context);
                            return;
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                List<Player> members = teamProvider.getOnlineMembersById(controllerId);
                for (Player member : members) {
                    if (member != null && member.isOnline()) {
                        dispatchForPlayer(member, arena, context);
                    }
                }
            }
            case "ZONE" -> {
                Location center = arena.getCenterLocation();
                if (center != null && center.getWorld() != null) {
                    World world = center.getWorld();
                    for (Player p : world.getPlayers()) {
                        if (arena.isWithinBounds(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ())) {
                            dispatchForPlayer(p, arena, context);
                        }
                    }
                }
            }
            case "TEAM_ZONE" -> {
                if (controllerId == null) return;
                Location center = arena.getCenterLocation();
                if (center != null && center.getWorld() != null) {
                    World world = center.getWorld();
                    for (Player p : world.getPlayers()) {
                        if (arena.isWithinBounds(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ())) {
                            boolean isMember = isSolo
                                    ? controllerId.equalsIgnoreCase(p.getUniqueId().toString())
                                    : controllerId.equalsIgnoreCase(teamProvider.getTeamId(p));
                            if (isMember) {
                                dispatchForPlayer(p, arena, context);
                            }
                        }
                    }
                }
            }
            default -> { // PLAYER or single console dispatch
                String playerName = (String) context.get("player");
                Player player = playerName != null ? Bukkit.getPlayer(playerName) : null;
                if (player != null && player.isOnline()) {
                    dispatchForPlayer(player, arena, context);
                } else {
                    dispatchGlobal(arena, context, playerName);
                }
            }
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
        String team = (String) context.getOrDefault("team", arena.getControllerTeamName());
        if (team != null) {
            cmd = cmd.replace("%team%", team).replace("<team>", team);
        }
        if (playerName != null) {
            cmd = cmd.replace("%player%", playerName).replace("<player>", playerName);
        }
        cmd = cmd.replace("%percent%", String.format("%.1f", arena.getProgress()));
        cmd = cmd.replace("%world%", arena.getWorldName());
        return cmd;
    }
}
