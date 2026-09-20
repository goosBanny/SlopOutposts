package me.goosbanny.outposts.core.feedback;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.arena.DefaultOutpostArena;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders real-time telemetry action bars to players inside the outpost capture pad.
 * All state chips and progress strings are configured via lang.yml.
 */
public class ActionBarManager {

    private static final int BAR_LENGTH = 15;
    private static final char FILLED_CHAR = '█';
    private static final char EMPTY_CHAR = '░';
    private static final String[] PRECOMPUTED_BARS = new String[BAR_LENGTH + 1];

    static {
        for (int filled = 0; filled <= BAR_LENGTH; filled++) {
            StringBuilder sb = new StringBuilder();
            sb.append("<green>");
            for (int i = 0; i < filled; i++) {
                sb.append(FILLED_CHAR);
            }
            sb.append("<gray>");
            for (int i = filled; i < BAR_LENGTH; i++) {
                sb.append(EMPTY_CHAR);
            }
            PRECOMPUTED_BARS[filled] = sb.toString();
        }
    }

    private final LangManager langManager;
    private final Map<String, Set<UUID>> lastTickRecipients = new ConcurrentHashMap<>();

    public ActionBarManager(@NotNull LangManager langManager) {
        this.langManager = langManager;
    }

    /**
     * Builds a formatted segmented progress bar string via precomputed table lookup (zero-allocation).
     *
     * @param progressPercent percentage 0.0 to 100.0
     * @return colored bar string
     */
    public static String buildProgressBar(double progressPercent) {
        int filled = (int) Math.round((Math.max(0.0, Math.min(100.0, progressPercent)) / 100.0) * BAR_LENGTH);
        return PRECOMPUTED_BARS[Math.max(0, Math.min(BAR_LENGTH, filled))];
    }

    /**
     * Updates action bar telemetry for all players currently inside the arena.
     * Clears action bar immediately for players exiting the arena boundary.
     */
    public void renderTelemetry(@NotNull OutpostArena arena) {
        if (!arena.isActive()) {
            clearArena(arena.getId());
            return;
        }

        Location center = arena.getCenterLocation();
        if (center == null || center.getWorld() == null) return;

        if (langManager.isSuppressed("telemetry.actionbar", arena)) {
            clearArena(arena.getId());
            return;
        }

        World world = center.getWorld();
        double progress = arena.getProgress();
        String progressBar = buildProgressBar(progress);

        String stateStr;
        Map<String, String> stateTokens = new HashMap<>();
        stateTokens.put("seconds", String.valueOf(arena.getLockoutRemainingSeconds()));
        stateTokens.put("warmup", String.valueOf(arena.getActivationGraceRemainingSeconds()));

        if (arena.isWarmingUp()) {
            stateStr = langManager.getFormattedString("telemetry.actionbar_states.warmup", arena, "<yellow>[WARMUP %warmup%s]</yellow>", stateTokens);
        } else if (arena.isLocked()) {
            stateStr = langManager.getFormattedString("telemetry.actionbar_states.locked", arena, "<blue>[LOCKED %seconds%s]</blue>", stateTokens);
        } else if (arena.isContested()) {
            stateStr = langManager.getFormattedString("telemetry.actionbar_states.contested", arena, "<red><bold>[CONTESTED]</bold></red>", stateTokens);
        } else if (arena.getControllerTeamId() != null) {
            stateStr = langManager.getFormattedString("telemetry.actionbar_states.controlled", arena, "<green>[CONTROLLED]</green>", stateTokens);
        } else if (arena.getCappingTeamId() != null) {
            stateStr = langManager.getFormattedString("telemetry.actionbar_states.capturing", arena, "<yellow>[CAPTURING]</yellow>", stateTokens);
        } else {
            stateStr = langManager.getFormattedString("telemetry.actionbar_states.neutral", arena, "<gray>[NEUTRAL]</gray>", stateTokens);
        }

        Map<String, String> tokens = new HashMap<>();
        tokens.put("state", stateStr);
        tokens.put("progress_bar", progressBar);
        tokens.put("progress", String.format("%.1f", progress));
        tokens.put("cappers", String.valueOf(arena.getCapperCount()));
        tokens.put("competing", String.valueOf(arena.getCapperCount()));
        tokens.put("team", arena.getControllerTeamName() != null ? arena.getControllerTeamName() : langManager.getPlaceholder("no_controller", "No Controller"));
        String capping = arena instanceof DefaultOutpostArena def ? def.getCappingTeamName() : arena.getCappingTeamId();
        tokens.put("capping", capping != null ? capping : langManager.getPlaceholder("no_capper", "None"));
        tokens.put("warmup", String.valueOf(arena.getActivationGraceRemainingSeconds()));
        if (arena.getCurrentRegion() != null) {
            tokens.put("region", arena.getCurrentRegion().getRawName());
        }

        Component comp = langManager.get("telemetry.actionbar", arena, tokens);

        Set<UUID> currentRecipients = new HashSet<>();
        Set<UUID> candidates = arena instanceof DefaultOutpostArena def
                ? def.getPlayersInZone()
                : Collections.emptySet();

        for (UUID uid : candidates) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                p.sendActionBar(comp);
                currentRecipients.add(uid);
            }
        }

        // Send empty action bar immediately to players who left the capture zone
        Set<UUID> prev = lastTickRecipients.put(arena.getId(), currentRecipients);
        if (prev != null) {
            for (UUID uid : prev) {
                if (!currentRecipients.contains(uid)) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p != null && p.isOnline()) {
                        p.sendActionBar(Component.empty());
                    }
                }
            }
        }
    }

    /**
     * Clears action bars for any remaining viewers of this arena.
     */
    public void clearArena(@NotNull String arenaId) {
        Set<UUID> prev = lastTickRecipients.remove(arenaId);
        if (prev != null) {
            for (UUID uid : prev) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null && p.isOnline()) {
                    p.sendActionBar(Component.empty());
                }
            }
        }
    }
}
