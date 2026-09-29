package me.goosbanny.outposts.core.pipeline.actions;

import me.goosbanny.outposts.Outposts;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.pipeline.ArenaAction;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.core.pipeline.ActionTargetResolver;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Action that sends Adventure MiniMessage formatted chat messages to target players or teams.
 */
public class MessageAction implements ArenaAction {

    private final List<String> messageTemplates;
    private final String target;
    private final TeamRosterProvider teamProvider;
    private final FoliaCompatScheduler scheduler;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageAction(
            @NotNull List<String> messageTemplates,
            @Nullable String target,
            @NotNull TeamRosterProvider teamProvider,
            @NotNull FoliaCompatScheduler scheduler
    ) {
        this.messageTemplates = new ArrayList<>(messageTemplates);
        this.target = target;
        this.teamProvider = teamProvider;
        this.scheduler = scheduler;
    }

    public MessageAction(
            @NotNull String singleMessage,
            @Nullable String target,
            @NotNull TeamRosterProvider teamProvider,
            @NotNull FoliaCompatScheduler scheduler
    ) {
        this(Collections.singletonList(singleMessage), target, teamProvider, scheduler);
    }

    @Override
    public @NotNull String getType() {
        return "MESSAGE";
    }

    @NotNull
    public List<String> getMessageTemplates() {
        return Collections.unmodifiableList(messageTemplates);
    }

    @Nullable
    public String getTarget() {
        return target;
    }

    @Override
    public void execute(@NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
        if (messageTemplates.isEmpty()) return;

        List<Player> targets = ActionTargetResolver.resolvePlayers(target, "CONTROLLER", arena, context, teamProvider);
        if (targets.isEmpty()) return;

        List<Component> components = new ArrayList<>(messageTemplates.size());
        for (String raw : messageTemplates) {
            String formatted = formatTokens(raw, arena, context);
            components.add(miniMessage.deserialize(formatted));
        }

        for (Player p : targets) {
            if (p != null && p.isOnline()) {
                scheduler.runForEntity(p, () -> {
                    for (Component c : components) {
                        p.sendMessage(c);
                    }
                });
            }
        }
    }

    private String formatTokens(String raw, OutpostArena arena, Map<String, Object> context) {
        String msg = raw;
        String prefix = Outposts.getInstance() != null && Outposts.getInstance().getLangManager() != null
                ? Outposts.getInstance().getLangManager().getPrefix()
                : "<#E13148><bold>OUTPOSTS</bold></#E13148><!bold> <gray>▶</gray> ";
        msg = msg.replace("<prefix>", prefix);
        msg = msg.replace("<name>", miniMessage.serialize(arena.getDisplayName()));
        msg = msg.replace("<id>", arena.getId());

        boolean isSolo = arena.getOccupancyMode() == OccupancyMode.SOLO;
        String team = (String) context.getOrDefault("team", arena.getControllerTeamName());
        String player = (String) context.get("player");
        if (player == null && isSolo && team != null) {
            player = team;
        }

        if (team != null) {
            msg = msg.replace("<team>", team).replace("%team%", team);
        }
        if (player != null) {
            msg = msg.replace("<player>", player).replace("%player%", player);
        }

        String prevTeam = (String) context.get("previous_team");
        if (prevTeam != null) {
            msg = msg.replace("<previous_team>", prevTeam).replace("%previous_team%", prevTeam);
        }

        String invader = (String) context.get("invader");
        if (invader != null) {
            msg = msg.replace("<invader>", invader).replace("%invader%", invader);
        }

        String invaderTeam = (String) context.get("invader_team");
        if (invaderTeam != null) {
            msg = msg.replace("<invader_team>", invaderTeam).replace("%invader_team%", invaderTeam);
        }

        Object held = context.get("seconds_held");
        if (held != null) {
            msg = msg.replace("<seconds_held>", String.valueOf(held));
        }

        return msg;
    }
}
