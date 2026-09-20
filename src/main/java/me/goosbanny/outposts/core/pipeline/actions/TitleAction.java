package me.goosbanny.outposts.core.pipeline.actions;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.pipeline.ArenaAction;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Action that sends Adventure Titles & Subtitles to target players or teams.
 */
public class TitleAction implements ArenaAction {

    private final String titleText;
    private final String subtitleText;
    private final String target;
    private final TeamRosterProvider teamProvider;
    private final FoliaCompatScheduler scheduler;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public TitleAction(
            @NotNull String titleText,
            @NotNull String subtitleText,
            @Nullable String target,
            @NotNull TeamRosterProvider teamProvider,
            @NotNull FoliaCompatScheduler scheduler
    ) {
        this.titleText = titleText;
        this.subtitleText = subtitleText;
        this.target = target;
        this.teamProvider = teamProvider;
        this.scheduler = scheduler;
    }

    @Override
    public @NotNull String getType() {
        return "TITLE";
    }

    @Override
    public void execute(@NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
        Component titleComp = miniMessage.deserialize(format(titleText, arena, context));
        Component subComp = miniMessage.deserialize(format(subtitleText, arena, context));
        Title title = Title.title(titleComp, subComp, Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500)));

        if ("DEFENDING_TEAM".equalsIgnoreCase(target) || "CONTROLLER_TEAM".equalsIgnoreCase(target) || "CONTROLLING_TEAM".equalsIgnoreCase(target)) {
            String controllerId = arena.getControllerTeamId();
            if (controllerId == null) return;

            if (arena.getOccupancyMode() == OccupancyMode.SOLO) {
                try {
                    UUID uuid = UUID.fromString(controllerId);
                    Player soloPlayer = Bukkit.getPlayer(uuid);
                    if (soloPlayer != null && soloPlayer.isOnline()) {
                        scheduler.runForEntity(soloPlayer, () -> soloPlayer.showTitle(title));
                    }
                } catch (IllegalArgumentException ignored) {}
                return;
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (controllerId.equals(teamProvider.getTeamId(p))) {
                    scheduler.runForEntity(p, () -> p.showTitle(title));
                }
            }
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
                scheduler.runForEntity(p, () -> p.showTitle(title));
            }
        }
    }

    private String format(String raw, OutpostArena arena, Map<String, Object> context) {
        String msg = raw.replace("<name>", MiniMessage.miniMessage().serialize(arena.getDisplayName()));
        msg = msg.replace("<id>", arena.getId());
        String team = (String) context.getOrDefault("team", arena.getControllerTeamName());
        if (team != null) {
            msg = msg.replace("<team>", team).replace("%team%", team);
        }
        return msg;
    }
}
