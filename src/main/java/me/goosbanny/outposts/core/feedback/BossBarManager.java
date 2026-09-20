package me.goosbanny.outposts.core.feedback;

import me.goosbanny.outposts.api.arena.ArenaState;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.arena.DefaultOutpostArena;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic Adventure Chromatic BossBars for Outpost arenas.
 * All title templates are configured via lang.yml.
 */
public class BossBarManager {

    private static final ThreadLocal<Set<UUID>> NEARBY_PLAYERS_BUFFER = ThreadLocal.withInitial(HashSet::new);
    private final TeamRosterProvider teamProvider;
    private final LangManager langManager;
    private final int renderDistanceBlocks;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, BossBar> arenaBossBars = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> viewersPerArena = new ConcurrentHashMap<>();

    public BossBarManager(@NotNull TeamRosterProvider teamProvider, @NotNull LangManager langManager, int renderDistanceBlocks) {
        this.teamProvider = teamProvider;
        this.langManager = langManager;
        this.renderDistanceBlocks = Math.max(16, renderDistanceBlocks);
    }

    /**
     * Updates the BossBar display and viewer subscription for an arena.
     */
    public void updateArenaBar(@NotNull OutpostArena arena) {
        if (!arena.isActive()) {
            removeArenaBar(arena.getId());
            return;
        }

        BossBar bar = arenaBossBars.computeIfAbsent(arena.getId(), id -> BossBar.bossBar(
                Component.empty(),
                0.0f,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
        ));

        // Determine progress (0.0f to 1.0f)
        float progress = (float) Math.max(0.0, Math.min(1.0, arena.getProgress() / 100.0));
        bar.progress(progress);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("name", miniMessage.serialize(arena.getDisplayName()));
        tokens.put("progress", String.format("%.1f", arena.getProgress()));

        String controllerDisplay = resolveParticipantName(arena.getControllerTeamName(), arena.getControllerTeamId());
        tokens.put("team", controllerDisplay != null ? controllerDisplay : langManager.getPlaceholder("no_controller", "No Controller"));

        String cappingName = null;
        String clearingName = null;
        if (arena instanceof DefaultOutpostArena def) {
            cappingName = def.getCappingTeamName();
            clearingName = def.getClearingTeamName();
        }
        String cappingDisplay = resolveParticipantName(cappingName, arena.getCappingTeamId());
        tokens.put("capping", cappingDisplay != null ? cappingDisplay : langManager.getPlaceholder("no_capper", "None"));
        tokens.put("clearing", clearingName != null ? clearingName : (cappingDisplay != null ? cappingDisplay : ""));

        tokens.put("cappers", String.valueOf(arena.getCapperCount()));
        tokens.put("competing", String.valueOf(arena.getCapperCount()));
        tokens.put("seconds", String.valueOf(arena.getLockoutRemainingSeconds()));
        tokens.put("warmup", String.valueOf(arena.getActivationGraceRemainingSeconds()));
        if (arena.getCurrentRegion() != null) {
            tokens.put("region", miniMessage.serialize(arena.getCurrentRegion().getDisplayName()));
        }

        // Determine Chromatic Styling & String Suppression
        String stateKey;
        BossBar.Color barColor;
        if (arena.isWarmingUp()) {
            stateKey = "warmup";
            barColor = BossBar.Color.PURPLE;
        } else if (arena.getState() == ArenaState.LOCKED) {
            stateKey = "locked";
            barColor = BossBar.Color.BLUE;
        } else if (arena.isContested()) {
            stateKey = "contested";
            barColor = BossBar.Color.RED;
        } else if (arena.getState() == ArenaState.CONTROLLED) {
            stateKey = "controlled";
            barColor = BossBar.Color.GREEN;
        } else if (arena.getState() == ArenaState.CAPTURING) {
            stateKey = "capturing";
            barColor = BossBar.Color.YELLOW;
        } else {
            stateKey = "neutral";
            barColor = BossBar.Color.WHITE;
        }

        String langPath = "telemetry.bossbar." + stateKey;
        if (langManager.isSuppressed(langPath, arena) || langManager.isSuppressed("telemetry.bossbar", arena)) {
            removeArenaBar(arena.getId());
            return;
        }

        bar.color(barColor);
        bar.name(langManager.get(langPath, arena, tokens));

        // Manage nearby viewers safely via world.getPlayers() (Folia safe, zero cross-region chunk access)
        Set<UUID> currentViewers = viewersPerArena.computeIfAbsent(arena.getId(), k -> ConcurrentHashMap.newKeySet());
        Set<UUID> nearbyPlayers = NEARBY_PLAYERS_BUFFER.get();
        nearbyPlayers.clear();

        Location center = arena.getCenterLocation();
        if (center != null && center.getWorld() != null) {
            double rSquared = (double) renderDistanceBlocks * renderDistanceBlocks;
            List<Player> candidates = new ArrayList<>();
            try {
                for (Entity e : center.getWorld().getNearbyEntities(center, renderDistanceBlocks, renderDistanceBlocks, renderDistanceBlocks, entity -> entity instanceof Player)) {
                    if (e instanceof Player p && p.isOnline() && !p.isDead()) {
                        if (FoliaCompatScheduler.isFolia() && !FoliaCompatScheduler.isOwnedByCurrentRegion(p)) {
                            continue;
                        }
                        candidates.add(p);
                    }
                }
            } catch (Exception e) {
                for (Player p : center.getWorld().getPlayers()) {
                    if (p.isOnline() && !p.isDead()) {
                        if (FoliaCompatScheduler.isFolia()
                                && !FoliaCompatScheduler.isOwnedByCurrentRegion(p)) {
                            continue;
                        }
                        candidates.add(p);
                    }
                }
            }

            for (Player p : candidates) {
                Location loc = p.getLocation();
                if (loc.getWorld() != null && loc.getWorld().equals(center.getWorld()) && loc.distanceSquared(center) <= rSquared) {
                    nearbyPlayers.add(p.getUniqueId());
                    if (!currentViewers.contains(p.getUniqueId())) {
                        p.showBossBar(bar);
                        currentViewers.add(p.getUniqueId());
                    }
                }
            }
        }

        // Remove players who walked away
        Iterator<UUID> it = currentViewers.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            if (!nearbyPlayers.contains(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.hideBossBar(bar);
                }
                it.remove();
            }
        }
    }

    public void removeArenaBar(@NotNull String arenaId) {
        BossBar bar = arenaBossBars.remove(arenaId);
        Set<UUID> viewers = viewersPerArena.remove(arenaId);
        if (bar != null && viewers != null) {
            for (UUID uuid : viewers) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.hideBossBar(bar);
                }
            }
        }
    }

    public void hideAll() {
        for (Map.Entry<String, BossBar> entry : arenaBossBars.entrySet()) {
            Set<UUID> viewers = viewersPerArena.get(entry.getKey());
            if (viewers != null) {
                for (UUID uuid : viewers) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.hideBossBar(entry.getValue());
                    }
                }
            }
        }
        arenaBossBars.clear();
        viewersPerArena.clear();
    }

    /**
     * Purges disconnected players immediately to avoid memory leaks on Folia.
     */
    public void handlePlayerQuit(@NotNull UUID playerUuid) {
        for (Map.Entry<String, Set<UUID>> entry : viewersPerArena.entrySet()) {
            if (entry.getValue().remove(playerUuid)) {
                BossBar bar = arenaBossBars.get(entry.getKey());
                if (bar != null) {
                    Player p = Bukkit.getPlayer(playerUuid);
                    if (p != null) {
                        p.hideBossBar(bar);
                    }
                }
            }
        }
    }

    private String resolveParticipantName(@Nullable String name, @Nullable String id) {
        if (name != null && !name.isBlank()) {
            if (!isUuid(name)) {
                return name;
            }
            id = name;
        }
        if (id == null || id.isBlank()) return null;
        if (isUuid(id)) {
            try {
                UUID uuid = UUID.fromString(id);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) return p.getName();
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                if (op.getName() != null) return op.getName();
            } catch (Exception ignored) {
            }
        }
        return id;
    }

    private boolean isUuid(@Nullable String str) {
        if (str == null || str.length() != 36) return false;
        return str.charAt(8) == '-' && str.charAt(13) == '-' && str.charAt(18) == '-' && str.charAt(23) == '-';
    }
}
