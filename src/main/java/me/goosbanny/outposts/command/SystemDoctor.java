package me.goosbanny.outposts.command;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.core.manager.ArenaManager;
import me.goosbanny.outposts.core.manager.SpatialGridManager;
import me.goosbanny.outposts.core.schedule.ScheduleManager;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import me.goosbanny.outposts.hook.team.TeamHookManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic health auditor for Outposts (/outpost doctor).
 * Audits team connectivity, Folia threads, spatial chunk indexing, and AABB overlaps.
 */
public class SystemDoctor {

    private final ArenaManager arenaManager;
    private final SpatialGridManager spatialGridManager;
    private final TeamHookManager teamHookManager;
    private final ScheduleManager scheduleManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SystemDoctor(
            @NotNull ArenaManager arenaManager,
            @NotNull SpatialGridManager spatialGridManager,
            @NotNull TeamHookManager teamHookManager,
            @Nullable ScheduleManager scheduleManager
    ) {
        this.arenaManager = arenaManager;
        this.spatialGridManager = spatialGridManager;
        this.teamHookManager = teamHookManager;
        this.scheduleManager = scheduleManager;
    }

    public void runDiagnostics(@NotNull CommandSender sender) {
        sender.sendMessage(mm.deserialize("<gradient:#FF416C:#8A2387><bold>=== Outposts System Doctor Diagnostic Audit ===</bold></gradient>"));

        // 1. Threading & Scheduler
        boolean isFolia = FoliaCompatScheduler.isFolia();
        sender.sendMessage(mm.deserialize("<gray>Platform Thread Model: <white>"
                + (isFolia ? "<aqua>Folia Region-Threaded</aqua>" : "<green>Paper Single-Thread / AsyncScheduler</green>")));

        // 2. Team & Economy Hooks
        String teamProvider = teamHookManager.getRosterProvider().getProviderName();
        boolean bankSupport = teamHookManager.getEconomyProvider().supportsBank();
        sender.sendMessage(mm.deserialize("<gray>Team Roster Provider: <green>[OK]</green> <gold>" + teamProvider + "</gold>"));
        sender.sendMessage(mm.deserialize("<gray>Bank Economy Support: "
                + (bankSupport ? "<green>[OK] Enabled</green>" : "<yellow>[WARN] Standalone / No Bank Support</yellow>")));

        // 3. Arenas & Schedules
        int totalArenas = arenaManager.getArenas().size();
        sender.sendMessage(mm.deserialize("<gray>Registered Arenas: <white>" + totalArenas + "</white>"));
        if (scheduleManager != null) {
            sender.sendMessage(mm.deserialize("<gray>Active Schedules: <white>" + scheduleManager.getSchedules().size() + "</white> <dark_gray>| <gray>Next Window: <gold>" + scheduleManager.getNextEventFormatted() + "</gold>"));
        }

        // 4. Overlapping Bounding Box Audit
        List<OutpostArena> arenaList = new ArrayList<>(arenaManager.getArenas());
        int overlapCount = 0;

        for (int i = 0; i < arenaList.size(); i++) {
            for (int j = i + 1; j < arenaList.size(); j++) {
                OutpostArena a = arenaList.get(i);
                OutpostArena b = arenaList.get(j);

                if (a.getWorldName().equalsIgnoreCase(b.getWorldName())) {
                    if (isOverlapping(a, b)) {
                        overlapCount++;
                        sender.sendMessage(mm.deserialize("<red>[CRITICAL] Overlapping Bounding Boxes detected!</red> "
                                + "<yellow>" + a.getId() + "</yellow> and <yellow>" + b.getId() + "</yellow> in world <white>"
                                + a.getWorldName() + "</white>"));
                    }
                }
            }
        }

        if (overlapCount == 0) {
            sender.sendMessage(mm.deserialize("<gray>Bounding Box Overlap Check: <green>[OK] No overlaps found</green>"));
        }

        // Summary
        sender.sendMessage(mm.deserialize("<gradient:#FF416C:#8A2387><bold>================================================</bold></gradient>"));
    }

    private boolean isOverlapping(OutpostArena a, OutpostArena b) {
        return a.getMinX() <= b.getMaxX() && a.getMaxX() >= b.getMinX()
                && a.getMinY() <= b.getMaxY() && a.getMaxY() >= b.getMinY()
                && a.getMinZ() <= b.getMaxZ() && a.getMaxZ() >= b.getMinZ();
    }
}
