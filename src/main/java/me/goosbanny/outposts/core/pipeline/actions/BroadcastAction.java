package me.goosbanny.outposts.core.pipeline.actions;

import me.goosbanny.outposts.Outposts;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.pipeline.ArenaAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Action that broadcasts a MiniMessage string to the entire server.
 */
public class BroadcastAction implements ArenaAction {

    private final String messageTemplate;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public BroadcastAction(@NotNull String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    @Override
    public @NotNull String getType() {
        return "BROADCAST";
    }

    @Override
    public void execute(@NotNull OutpostArena arena, @NotNull Map<String, Object> context) {
        String msg = messageTemplate;
        String prefix = Outposts.getInstance() != null && Outposts.getInstance().getLangManager() != null
                ? Outposts.getInstance().getLangManager().getPrefix()
                : "<#E13148><bold>OUTPOSTS</bold></#E13148><!bold> <gray>▶</gray> ";
        msg = msg.replace("<prefix>", prefix);
        msg = msg.replace("<name>", MiniMessage.miniMessage().serialize(arena.getDisplayName()) + "<!bold>");
        msg = msg.replace("<id>", arena.getId());

        boolean isSolo = arena.getOccupancyMode() == OccupancyMode.SOLO;
        String team = (String) context.getOrDefault("team", arena.getControllerTeamName());
        String player = (String) context.get("player");
        if (player == null && isSolo && team != null) {
            player = team;
        }

        if (isSolo) {
            String subject = player != null ? player : (team != null ? team : "Player");
            msg = msg.replace("The <#98fc98><team></#98fc98> faction", "Player <#98fc98>" + subject + "</#98fc98>");
            msg = msg.replace("The <#98fc98><team></#98fc98> team", "Player <#98fc98>" + subject + "</#98fc98>");
            msg = msg.replace("the <#98fc98><team></#98fc98> faction", "player <#98fc98>" + subject + "</#98fc98>");
            msg = msg.replace("the <#98fc98><team></#98fc98> team", "player <#98fc98>" + subject + "</#98fc98>");
            msg = msg.replace("The <team> faction", "Player " + subject);
            msg = msg.replace("The <team> team", "Player " + subject);
            msg = msg.replace("the <team> faction", "player " + subject);
            msg = msg.replace("the <team> team", "player " + subject);
            msg = msg.replace("<team> faction", "Player " + subject);
            msg = msg.replace("<team> team", "Player " + subject);
            msg = msg.replace("<team>", subject);
        } else {
            if (team != null) {
                msg = msg.replace("<team>", team);
            }
        }

        if (player != null) {
            msg = msg.replace("%player%", player);
            msg = msg.replace("<player>", player);
        }

        Component component = miniMessage.deserialize(msg);
        Bukkit.broadcast(component);
    }
}
