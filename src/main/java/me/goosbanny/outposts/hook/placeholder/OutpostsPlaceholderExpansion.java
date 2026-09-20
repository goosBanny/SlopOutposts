package me.goosbanny.outposts.hook.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.goosbanny.outposts.api.arena.ArenaState;
import me.goosbanny.outposts.api.arena.ArenaView;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.arena.DefaultOutpostArena;
import me.goosbanny.outposts.core.feedback.ActionBarManager;
import me.goosbanny.outposts.core.manager.ArenaManager;
import me.goosbanny.outposts.core.manager.SpatialGridManager;
import me.goosbanny.outposts.core.schedule.ScheduleManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * PlaceholderAPI expansion providing thread-safe, fully modular %outpost_<...>% placeholders.
 * Fallbacks and state chips are configured via lang.yml.
 */
public class OutpostsPlaceholderExpansion extends PlaceholderExpansion {

    private final ArenaManager arenaManager;
    private final SpatialGridManager spatialGridManager;
    private final TeamRosterProvider teamProvider;
    private final LangManager langManager;
    private final ScheduleManager scheduleManager;
    private final String pluginVersion;

    public OutpostsPlaceholderExpansion(
            @NotNull ArenaManager arenaManager,
            @NotNull SpatialGridManager spatialGridManager,
            @NotNull TeamRosterProvider teamProvider,
            @NotNull LangManager langManager,
            @Nullable ScheduleManager scheduleManager,
            @NotNull String pluginVersion
    ) {
        this.arenaManager = arenaManager;
        this.spatialGridManager = spatialGridManager;
        this.teamProvider = teamProvider;
        this.langManager = langManager;
        this.scheduleManager = scheduleManager;
        this.pluginVersion = pluginVersion;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "outpost";
    }

    @Override
    public @NotNull String getAuthor() {
        return "goosBanny";
    }

    @Override
    public @NotNull String getVersion() {
        return pluginVersion;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        String lower = params.toLowerCase();
        String boolTrue = langManager.getPlaceholder("boolean_true", "true");
        String boolFalse = langManager.getPlaceholder("boolean_false", "false");
        String none = langManager.getPlaceholder("none", "None");

        // 1. Player-scoped placeholders
        if (player != null) {
            if ("player_inside".equals(lower)) {
                Location loc = player.getLocation();
                OutpostArena arena = spatialGridManager.getArenaAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                return arena != null ? boolTrue : boolFalse;
            }

            if ("player_zone".equals(lower)) {
                Location loc = player.getLocation();
                OutpostArena arena = spatialGridManager.getArenaAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                return arena != null ? arena.getId() : none;
            }

            if ("team_outpost_count".equals(lower)) {
                String teamId = teamProvider.getTeamId(player);
                if (teamId == null) return "0";
                int count = 0;
                for (OutpostArena arena : arenaManager.getArenas()) {
                    if (teamId.equalsIgnoreCase(arena.getControllerTeamId())) {
                        count++;
                    }
                }
                return String.valueOf(count);
            }
        }

        if ("next_event_time".equals(lower)) {
            return scheduleManager != null ? scheduleManager.getNextEventFormatted() : none;
        }

        // 2. Arena-scoped placeholders:
        // Formats supported:
        //   A: arena_<id>_<property>   e.g. arena_south_controller
        //   B: <property>_<id>         e.g. controller_team_south, progress_south
        //   C: <id>_<property>         e.g. south_controller, south_progress
        String targetArenaId = null;
        String property = null;

        if (lower.startsWith("arena_")) {
            String remainder = lower.substring(6);
            int underscore = remainder.indexOf('_');
            if (underscore > 0) {
                targetArenaId = remainder.substring(0, underscore);
                property = remainder.substring(underscore + 1);
            }
        }

        if (targetArenaId == null) {
            int firstUnderscore = lower.indexOf('_');
            if (firstUnderscore > 0) {
                String potentialId = lower.substring(0, firstUnderscore);
                if (arenaManager.getArena(potentialId) != null) {
                    targetArenaId = potentialId;
                    property = lower.substring(firstUnderscore + 1);
                }
            }
        }

        if (targetArenaId == null) {
            int lastUnderscore = lower.lastIndexOf('_');
            if (lastUnderscore > 0) {
                String potentialId = lower.substring(lastUnderscore + 1);
                if (arenaManager.getArena(potentialId) != null) {
                    targetArenaId = potentialId;
                    property = lower.substring(0, lastUnderscore);
                }
            }
        }

        if (targetArenaId == null) {
            int lastUnderscore = lower.lastIndexOf('_');
            if (lastUnderscore > 0) {
                targetArenaId = lower.substring(lastUnderscore + 1);
                property = lower.substring(0, lastUnderscore);
            }
        }

        if (targetArenaId == null || property == null) {
            return null;
        }

        OutpostArena arena = arenaManager.getArena(targetArenaId);
        if (arena == null) {
            return langManager.getPlaceholder("missing_arena", "---");
        }

        ArenaView view = arena.createSnapshot();
        if (view == null) {
            return langManager.getPlaceholder("none", "None");
        }

        switch (property) {
            case "is_active", "active":
                return view.getState() != ArenaState.LOCKED ? boolTrue : boolFalse;
            case "is_contested", "contested":
                return view.isContested() ? boolTrue : boolFalse;
            case "is_locked", "locked":
                return view.getLockoutRemainingSeconds() > 0 ? boolTrue : boolFalse;
            case "state":
                return view.getState().name();
            case "state_formatted":
                return langManager.getPlaceholderState(view.getState().name(), view.getState().name());
            case "progress_bar":
                return ActionBarManager.buildProgressBar(view.getProgress());
            case "progress", "progress_percent":
                return String.format("%.1f", view.getProgress());
            case "controller", "controller_team":
                return view.getControllerTeam() != null ? resolveParticipant(view.getControllerTeam()) : langManager.getPlaceholder("no_controller", "No Controller");
            case "capper", "capping", "capping_team":
                return view.getCappingTeam() != null ? resolveParticipant(view.getCappingTeam()) : langManager.getPlaceholder("no_capper", "None");
            case "capper_count", "cappers", "competing":
                return String.valueOf(view.getCapperCount());
            case "time_controlled":
                long seconds = arena instanceof DefaultOutpostArena def ? def.getTimeControlledSeconds() : 0;
                return formatDuration(seconds);
            case "lock_remaining":
                return formatDuration(view.getLockoutRemainingSeconds());
            case "coords":
                return arena instanceof DefaultOutpostArena def ? def.getFormattedCoords() : "0, 0, 0";
            case "x": {
                Location c = arena.getCenterLocation();
                return c != null ? String.valueOf(c.getBlockX()) : "0";
            }
            case "y": {
                Location c = arena.getCenterLocation();
                return c != null ? String.valueOf(c.getBlockY()) : "0";
            }
            case "z": {
                Location c = arena.getCenterLocation();
                return c != null ? String.valueOf(c.getBlockZ()) : "0";
            }
            case "current_region_id", "region_id":
                return arena.getCurrentRegion() != null ? arena.getCurrentRegion().getId() : "default";
            case "current_region_name", "region", "region_name":
                return arena.getCurrentRegion() != null
                        ? net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(arena.getCurrentRegion().getDisplayName())
                        : "Default";
            case "next_shift_seconds":
                return arena.getNextShiftSeconds() >= 0 ? String.valueOf(arena.getNextShiftSeconds()) : "-1";
            case "next_shift_formatted":
                return arena.getNextShiftSeconds() >= 0 ? formatDuration(arena.getNextShiftSeconds()) : none;
            case "region_count":
                return String.valueOf(arena.getDynamicLocationConfig().getRegions().size());
            case "is_shifting":
                return arena.getDynamicLocationConfig().isEnabled() ? boolTrue : boolFalse;
            case "warmup_remaining", "warmup_seconds":
                return String.valueOf(arena.getActivationGraceRemainingSeconds());
            case "is_warming_up":
                return arena.isWarmingUp() ? boolTrue : boolFalse;
            default:
                return null;
        }
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private String resolveParticipant(String raw) {
        if (raw == null || raw.isBlank()) return langManager.getPlaceholder("none", "None");
        if (raw.length() == 36 && raw.charAt(8) == '-' && raw.charAt(13) == '-' && raw.charAt(18) == '-' && raw.charAt(23) == '-') {
            try {
                UUID uuid = UUID.fromString(raw);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) return p.getName();
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op.getName() != null) return op.getName();
            } catch (Exception ignored) {
            }
        }
        return raw;
    }
}
