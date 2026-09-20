package me.goosbanny.outposts.core.schedule;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.manager.ArenaManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Automated Cron Scheduling Engine with Overtime extensions and pre-event warnings.
 * Controls active time windows for Outpost arenas according to schedules.yml.
 */
public class ScheduleManager {

    public static class ScheduleEntry {
        private final String id;
        private final String arenaId;
        private final CronExpression cron;
        private final long durationMinutes;
        private final boolean overtimeEnabled;
        private final long maxOvertimeMinutes;
        private final List<BroadcastWarning> warnings;

        // Runtime state
        private volatile boolean active = false;
        private volatile boolean inOvertime = false;
        private volatile long activeEndMillis = 0;
        private volatile ZonedDateTime nextScheduledStart;
        private final Set<Integer> dispatchedWarningsForCycle = new HashSet<>();

        public ScheduleEntry(
                String id,
                String arenaId,
                CronExpression cron,
                long durationMinutes,
                boolean overtimeEnabled,
                long maxOvertimeMinutes,
                List<BroadcastWarning> warnings
        ) {
            this.id = id;
            this.arenaId = arenaId;
            this.cron = cron;
            this.durationMinutes = Math.max(1, durationMinutes);
            this.overtimeEnabled = overtimeEnabled;
            this.maxOvertimeMinutes = Math.max(0, maxOvertimeMinutes);
            this.warnings = warnings != null ? warnings : Collections.emptyList();
            checkInitialWindow(ZonedDateTime.now(ZoneOffset.UTC), System.currentTimeMillis());
        }

        public void checkInitialWindow(@NotNull ZonedDateTime now, long nowMillis) {
            for (int m = 0; m < durationMinutes; m++) {
                ZonedDateTime candidate = now.minusMinutes(m);
                if (cron.matches(candidate)) {
                    this.active = true;
                    this.inOvertime = false;
                    this.activeEndMillis = nowMillis + (durationMinutes - m) * 60_000L;
                    return;
                }
            }
            this.active = false;
            calculateNextStart();
        }

        public void calculateNextStart() {
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            this.nextScheduledStart = cron.nextMatching(now);
            this.dispatchedWarningsForCycle.clear();
        }

        public String getId() { return id; }
        public String getArenaId() { return arenaId; }
        public CronExpression getCron() { return cron; }
        public long getDurationMinutes() { return durationMinutes; }
        public boolean isOvertimeEnabled() { return overtimeEnabled; }
        public long getMaxOvertimeMinutes() { return maxOvertimeMinutes; }
        public List<BroadcastWarning> getWarnings() { return warnings; }
        public boolean isActive() { return active; }
        public boolean isInOvertime() { return inOvertime; }
        public long getActiveEndMillis() { return activeEndMillis; }
        public ZonedDateTime getNextScheduledStart() { return nextScheduledStart; }
    }

    public record BroadcastWarning(int minutesBefore, String message) {}

    private final ArenaManager arenaManager;
    private final LangManager langManager;
    private final Logger logger;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, ScheduleEntry> scheduleMap = new ConcurrentHashMap<>();
    private final Map<String, ScheduleEntry> arenaIdIndex = new ConcurrentHashMap<>();

    public ScheduleManager(
            @NotNull ArenaManager arenaManager,
            @NotNull LangManager langManager,
            @NotNull Logger logger
    ) {
        this.arenaManager = arenaManager;
        this.langManager = langManager;
        this.logger = logger;
    }

    /**
     * Loads or reloads schedules from the provided YamlConfiguration (schedules.yml).
     */
    public void loadSchedules(@NotNull YamlConfiguration yaml) {
        scheduleMap.clear();
        arenaIdIndex.clear();
        ConfigurationSection sec = yaml.getConfigurationSection("schedules");
        if (sec == null) {
            return;
        }

        for (String key : sec.getKeys(false)) {
            ConfigurationSection item = sec.getConfigurationSection(key);
            if (item == null || !item.getBoolean("enabled", true)) {
                continue;
            }

            try {
                String arenaId = item.getString("arena", key);
                String cronStr = item.getString("cron", "0 * * * *");
                long duration = item.getLong("duration_minutes", 30);
                boolean overtime = item.getBoolean("overtime.enabled", true);
                long maxOvertime = item.getLong("overtime.max_overtime_minutes", 15);

                List<BroadcastWarning> warnings = new ArrayList<>();
                List<Map<?, ?>> warningList = item.getMapList("broadcasts");
                for (Map<?, ?> w : warningList) {
                    int mins = Integer.parseInt(String.valueOf(w.get("minutes_before")));
                    String msg = String.valueOf(w.get("message"));
                    warnings.add(new BroadcastWarning(mins, msg));
                }

                CronExpression cron = new CronExpression(cronStr);
                ScheduleEntry entry = new ScheduleEntry(key, arenaId, cron, duration, overtime, maxOvertime, warnings);
                scheduleMap.put(key.toLowerCase(), entry);
                arenaIdIndex.put(arenaId.toLowerCase(), entry);

                OutpostArena arena = arenaManager.getArena(arenaId);
                if (arena != null) {
                    arena.setActive(entry.isActive());
                }

                logger.info("[Schedules] Loaded schedule '" + key + "' for arena '" + arenaId + "' [" + cronStr + "] Active: " + entry.isActive());
            } catch (Exception e) {
                logger.warning("[Schedules] Failed to parse schedule '" + key + "': " + e.getMessage());
            }
        }
    }

    /**
     * Synchronizes an arena's active status with any applicable schedule entry.
     * If the arena is governed by a schedule, its active state will reflect the schedule window.
     * If the arena is not governed by any schedule and autoStart is true, it remains active.
     */
    public void syncArenaState(@NotNull OutpostArena arena) {
        ScheduleEntry entry = arenaIdIndex.get(arena.getId().toLowerCase());
        if (entry != null) {
            arena.setActive(entry.isActive());
        }
    }

    /**
     * Checks if the given arena ID has a scheduled entry configured.
     */
    public boolean hasSchedule(@NotNull String arenaId) {
        return arenaIdIndex.containsKey(arenaId.toLowerCase());
    }

    /**
     * Ticks schedule checks every second.
     */
    public void tick() {
        if (scheduleMap.isEmpty()) return;

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        long nowMillis = System.currentTimeMillis();

        for (ScheduleEntry entry : scheduleMap.values()) {
            OutpostArena arena = arenaManager.getArena(entry.getArenaId());
            if (arena == null) continue;

            if (!entry.isActive()) {
                // Check if cron matches right now
                if (entry.getCron().matches(now)) {
                    startEvent(entry, arena);
                    continue;
                }

                // Check pre-event broadcast warnings
                if (entry.getNextScheduledStart() != null) {
                    long minutesUntil = Duration.between(now, entry.getNextScheduledStart()).toMinutes();
                    if (minutesUntil > 0) {
                        for (BroadcastWarning warning : entry.getWarnings()) {
                            if (minutesUntil == warning.minutesBefore() && !entry.dispatchedWarningsForCycle.contains(warning.minutesBefore())) {
                                entry.dispatchedWarningsForCycle.add(warning.minutesBefore());
                                broadcastScheduleMessage(warning.message(), arena);
                            }
                        }
                    }
                }
            } else {
                // Event is currently running
                if (nowMillis >= entry.getActiveEndMillis()) {
                    long elapsedOvertime = (nowMillis - entry.getActiveEndMillis()) / 60_000L;

                    if (arena.isContested() && entry.isOvertimeEnabled() && elapsedOvertime < entry.getMaxOvertimeMinutes()) {
                        if (!entry.isInOvertime()) {
                            entry.inOvertime = true;
                            Map<String, String> tokens = new HashMap<>();
                            tokens.put("name", miniMessage.serialize(arena.getDisplayName()));
                            Bukkit.broadcast(langManager.get("broadcasts.event_overtime", tokens));
                        }
                    } else {
                        endEvent(entry, arena);
                    }
                }
            }
        }
    }

    private void startEvent(@NotNull ScheduleEntry entry, @NotNull OutpostArena arena) {
        entry.active = true;
        entry.inOvertime = false;
        entry.activeEndMillis = System.currentTimeMillis() + (entry.getDurationMinutes() * 60_000L);
        arena.setActive(true);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("name", miniMessage.serialize(arena.getDisplayName()));
        Bukkit.broadcast(langManager.get("broadcasts.event_started", tokens));
        logger.info("[Schedules] Started scheduled event for '" + arena.getId() + "' (Duration: " + entry.getDurationMinutes() + "m)");
    }

    private void endEvent(@NotNull ScheduleEntry entry, @NotNull OutpostArena arena) {
        entry.active = false;
        entry.inOvertime = false;
        entry.calculateNextStart();
        arena.setActive(false);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("name", miniMessage.serialize(arena.getDisplayName()));
        String winner = arena.getControllerTeamName() != null ? arena.getControllerTeamName() : langManager.getPlaceholder("none", "None");
        tokens.put("winner", winner);
        Bukkit.broadcast(langManager.get("broadcasts.event_concluded", tokens));
        logger.info("[Schedules] Concluded scheduled event for '" + arena.getId() + "'");
    }

    private void broadcastScheduleMessage(String template, OutpostArena arena) {
        String msg = template
                .replace("<prefix>", langManager.getPrefix())
                .replace("<name>", miniMessage.serialize(arena.getDisplayName()))
                .replace("<id>", arena.getId());
        Bukkit.broadcast(miniMessage.deserialize(msg));
    }

    /**
     * Returns formatted countdown or status string for PlaceholderAPI %outpost_next_event_time%.
     */
    @NotNull
    public String getNextEventFormatted() {
        if (scheduleMap.isEmpty()) {
            return langManager.getPlaceholder("none", "None");
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        long nowMillis = System.currentTimeMillis();

        // 1. If any event is in overtime
        for (ScheduleEntry entry : scheduleMap.values()) {
            if (entry.isActive() && entry.isInOvertime()) {
                return langManager.getPlaceholderState("OVERTIME", "Overtime");
            }
        }

        // 2. If any event is currently active (find soonest to conclude)
        long shortestRemaining = -1;
        for (ScheduleEntry entry : scheduleMap.values()) {
            if (entry.isActive() && !entry.isInOvertime()) {
                long remainingSec = Math.max(0, (entry.getActiveEndMillis() - nowMillis) / 1000L);
                if (shortestRemaining < 0 || remainingSec < shortestRemaining) {
                    shortestRemaining = remainingSec;
                }
            }
        }
        if (shortestRemaining >= 0) {
            return formatSeconds(shortestRemaining);
        }

        // 3. Earliest upcoming event
        ScheduleEntry earliest = null;
        Duration shortest = null;

        for (ScheduleEntry entry : scheduleMap.values()) {
            if (entry.getNextScheduledStart() != null) {
                Duration diff = Duration.between(now, entry.getNextScheduledStart());
                if (!diff.isNegative()) {
                    if (shortest == null || diff.compareTo(shortest) < 0) {
                        shortest = diff;
                        earliest = entry;
                    }
                }
            }
        }

        if (shortest != null) {
            return formatSeconds(shortest.toSeconds());
        }

        return langManager.getPlaceholder("none", "None");
    }

    private String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 24) {
            long days = hours / 24;
            hours = hours % 24;
            return String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @NotNull
    public Collection<ScheduleEntry> getSchedules() {
        return Collections.unmodifiableCollection(scheduleMap.values());
    }
}
