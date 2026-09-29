package me.goosbanny.outposts.core.pipeline;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Centralized utility to resolve target players for any outpost action execution.
 * Guarantees zero redundancy across MessageAction, TitleAction, ActionBarAction, SoundAction, and ConsoleCommandAction.
 */
public final class ActionTargetResolver {

    private ActionTargetResolver() {}

    /**
     * Resolves the list of online players matching the specified target selector.
     *
     * @param targetRaw    raw target string from configuration (e.g. "CONTROLLER", "PREVIOUS_CONTROLLER", "CAPPER", "ZONE")
     * @param defaultTarget default fallback selector if targetRaw is null or blank
     * @param arena        active outpost arena
     * @param context      trigger context map
     * @param teamProvider team roster provider
     * @return non-null, distinct list of target players currently online
     */
    @NotNull
    public static List<Player> resolvePlayers(
            @Nullable String targetRaw,
            @NotNull String defaultTarget,
            @NotNull OutpostArena arena,
            @NotNull Map<String, Object> context,
            @NotNull TeamRosterProvider teamProvider
    ) {
        if (Bukkit.getServer() == null) {
            return Collections.emptyList();
        }

        String selector = (targetRaw != null && !targetRaw.isBlank())
                ? targetRaw.trim().toUpperCase()
                : defaultTarget.toUpperCase();

        Set<Player> resolved = new LinkedHashSet<>();
        boolean isSolo = arena.getOccupancyMode() == OccupancyMode.SOLO;

        switch (selector) {
            case "CONTROLLER", "CONTROLLER_TEAM", "DEFENDING_TEAM", "DEFENDER",
                 "CONTROLLING_TEAM", "TEAM", "TEAM_ONLINE", "PER_PLAYER" -> {
                String controllerId = (String) context.getOrDefault("team_id", arena.getControllerTeamId());
                resolveTeamOrPlayer(controllerId, isSolo, teamProvider, resolved);
            }

            case "PREVIOUS_CONTROLLER", "PREVIOUS_TEAM", "LOST_TEAM", "PREV_CONTROLLER" -> {
                String prevTeamId = (String) context.get("previous_team_id");
                if (prevTeamId == null) {
                    prevTeamId = (String) context.get("team_id");
                }
                resolveTeamOrPlayer(prevTeamId, isSolo, teamProvider, resolved);
            }

            case "CAPPER", "PLAYER", "ATTACKER", "CAPPING_PLAYER" -> {
                Object playerObj = context.get("player");
                if (playerObj instanceof String playerName) {
                    Player p = Bukkit.getPlayerExact(playerName);
                    if (p == null) p = Bukkit.getPlayer(playerName);
                    if (p != null && p.isOnline()) {
                        resolved.add(p);
                    }
                } else if (playerObj instanceof Player p && p.isOnline()) {
                    resolved.add(p);
                } else if (context.get("player_uuid") instanceof UUID uuid) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) resolved.add(p);
                }
            }

            case "CAPPER_TEAM", "CAPPING_TEAM" -> {
                String capperTeamId = (String) context.get("capper_team_id");
                if (capperTeamId == null) {
                    capperTeamId = arena.getCappingTeamId();
                }
                if (capperTeamId == null) {
                    capperTeamId = (String) context.get("team_id");
                }
                resolveTeamOrPlayer(capperTeamId, isSolo, teamProvider, resolved);
            }

            case "LEADER" -> {
                String teamId = (String) context.getOrDefault("team_id", arena.getControllerTeamId());
                if (teamId != null) {
                    if (isSolo) {
                        Player solo = lookupPlayer(teamId);
                        if (solo != null) resolved.add(solo);
                    } else {
                        List<Player> members = teamProvider.getOnlineMembersById(teamId);
                        if (!members.isEmpty()) {
                            UUID leaderUuid = teamProvider.getTeamLeader(members.get(0));
                            if (leaderUuid != null) {
                                Player leader = Bukkit.getPlayer(leaderUuid);
                                if (leader != null && leader.isOnline()) {
                                    resolved.add(leader);
                                }
                            }
                        }
                    }
                }
            }

            case "ZONE" -> {
                Location center = arena.getCenterLocation();
                World world = center != null ? center.getWorld() : Bukkit.getWorld(arena.getWorldName());
                if (world != null) {
                    for (Player p : world.getPlayers()) {
                        if (p.isOnline() && arena.isWithinBounds(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ())) {
                            resolved.add(p);
                        }
                    }
                }
            }

            case "CONTROLLER_ZONE", "TEAM_ZONE" -> {
                String controllerId = (String) context.getOrDefault("team_id", arena.getControllerTeamId());
                if (controllerId != null) {
                    Location center = arena.getCenterLocation();
                    World world = center != null ? center.getWorld() : Bukkit.getWorld(arena.getWorldName());
                    if (world != null) {
                        for (Player p : world.getPlayers()) {
                            if (p.isOnline() && arena.isWithinBounds(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ())) {
                                boolean isMember = isSolo
                                        ? controllerId.equalsIgnoreCase(p.getUniqueId().toString())
                                        : controllerId.equalsIgnoreCase(teamProvider.getTeamId(p));
                                if (isMember) {
                                    resolved.add(p);
                                }
                            }
                        }
                    }
                }
            }

            case "GLOBAL", "ALL", "SERVER" -> {
                resolved.addAll(Bukkit.getOnlinePlayers());
            }

            default -> {
                // Fallback: try resolving as context player, or controller
                Object playerObj = context.get("player");
                if (playerObj instanceof String playerName) {
                    Player p = Bukkit.getPlayerExact(playerName);
                    if (p != null && p.isOnline()) resolved.add(p);
                }
                if (resolved.isEmpty()) {
                    String controllerId = (String) context.getOrDefault("team_id", arena.getControllerTeamId());
                    resolveTeamOrPlayer(controllerId, isSolo, teamProvider, resolved);
                }
            }
        }

        return new ArrayList<>(resolved);
    }

    private static void resolveTeamOrPlayer(
            @Nullable String identifier,
            boolean isSolo,
            @NotNull TeamRosterProvider teamProvider,
            @NotNull Set<Player> resolved
    ) {
        if (identifier == null) return;

        if (isSolo) {
            Player p = lookupPlayer(identifier);
            if (p != null) resolved.add(p);
            return;
        }

        // Try team members
        List<Player> members = teamProvider.getOnlineMembersById(identifier);
        if (!members.isEmpty()) {
            resolved.addAll(members);
            return;
        }

        // Fallback: might be UUID or player name in hybrid configurations
        Player fallback = lookupPlayer(identifier);
        if (fallback != null) resolved.add(fallback);
    }

    private static Player lookupPlayer(@NotNull String raw) {
        try {
            UUID uuid = UUID.fromString(raw);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) return p;
        } catch (IllegalArgumentException ignored) {}

        Player p = Bukkit.getPlayerExact(raw);
        if (p != null && p.isOnline()) return p;
        p = Bukkit.getPlayer(raw);
        return (p != null && p.isOnline()) ? p : null;
    }
}
