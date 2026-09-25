package me.goosbanny.outposts.core.arena;

import me.goosbanny.outposts.Outposts;
import me.goosbanny.outposts.api.arena.ArenaState;
import me.goosbanny.outposts.api.arena.ArenaView;
import me.goosbanny.outposts.api.arena.ArenaViewSnapshot;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.event.OutpostCaptureEvent;
import me.goosbanny.outposts.api.event.OutpostContestEvent;
import me.goosbanny.outposts.api.event.OutpostLostEvent;
import me.goosbanny.outposts.api.event.OutpostPreShiftEvent;
import me.goosbanny.outposts.api.event.OutpostProgressChangeEvent;
import me.goosbanny.outposts.api.event.OutpostShiftEvent;
import me.goosbanny.outposts.api.mechanics.CaptureModeEngine;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.api.pipeline.ActionPipeline;
import me.goosbanny.outposts.api.pipeline.ActionTrigger;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.anticheese.AntiCheeseValidator;
import me.goosbanny.outposts.core.feedback.AudioCueManager;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standard thread-safe implementation of OutpostArena.
 * Features hysteresis buffers, debounce stabilization, Folia-safe region queries,
 * and lazily-evaluated immutable DTO snapshots.
 */
public class DefaultOutpostArena implements OutpostArena {

    private final String id;
    private final Component displayName;
    private final String serializedDisplayName;
    private ArenaGeometry geometry;
    private final ArenaMechanicsConfig mechanicsConfig;
    private final ArenaMultipliers multipliers;
    private final TeamRosterProvider teamProvider;
    private final AntiCheeseValidator antiCheeseValidator;
    private final LangManager langManager;
    private final ActionPipeline actionPipeline;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final OccupancyMode occupancyMode;
    private final long rewardIntervalSeconds;
    private final AudioCueManager audioCueManager = new AudioCueManager();
    private CaptureModeEngine captureEngine;
    private volatile boolean active = false;

    // Mutable state
    private ArenaState state = ArenaState.LOCKED;
    private double progress = 0.0;
    private String controllerTeamId = null;
    private String controllerTeamName = null;
    private String cappingTeamId = null;
    private String cappingTeamName = null;
    private int capperCount = 0;
    private boolean isContested = false;
    private long lockoutRemainingSeconds = 0;
    private long timeControlledSeconds = 0;
    private long tickCounter = 0;
    private int lastAnnouncedMilestone = 0;

    // Debounce and Hysteresis
    private int contestDebounceSeconds = 0;
    private long invaderHoldSeconds = 0;

    // Cached immutable snapshot for concurrent readers (PAPI, GUIs, etc.)
    private final AtomicReference<ArenaViewSnapshot> cachedSnapshot = new AtomicReference<>();
    private volatile boolean snapshotDirty = true;

    // Reusable instance collections
    private final List<Player> validCappers = new ArrayList<>(16);
    private final Map<String, List<Player>> teamsPresent = new HashMap<>(8);
    private final List<String> presentTeamIds = new ArrayList<>(8);
    private final Map<String, Object> reusableContext = new HashMap<>(4);
    private final Set<UUID> playersInZone = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> LAST_CAPTURE_TIMES = new ConcurrentHashMap<>();

    // Dynamic changing outpost state & language overrides
    private DynamicLocationConfig dynamicLocationConfig = DynamicLocationConfig.createDisabled();
    private ArenaRegion currentRegion = null;
    private long nextShiftSeconds = 0;
    private int activationGraceRemainingSeconds = 0;
    private final Set<Integer> dispatchedShiftWarnings = new HashSet<>();
    private final Map<String, String> customLangOverrides = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private boolean boundingParticlesEnabled = false;
    private String clearingTeamName = null;

    private Integer customBossbarRange = null;
    private Integer customBoundaryRenderDistance = null;
    private Boolean customPreventChorusFruit = null;
    private Boolean customPreventElytraFlight = null;
    private Boolean customPreventBlockBreak = null;
    private Boolean customPreventBlockPlace = null;
    private String customTeamProvider = null;

    public DefaultOutpostArena(
            @NotNull String id,
            @NotNull Component displayName,
            @NotNull ArenaGeometry geometry,
            @NotNull ArenaMechanicsConfig mechanicsConfig,
            @NotNull ArenaMultipliers multipliers,
            @NotNull TeamRosterProvider teamProvider,
            @NotNull AntiCheeseValidator antiCheeseValidator,
            @NotNull ActionPipeline actionPipeline,
            @NotNull CaptureModeEngine captureEngine,
            @NotNull LangManager langManager
    ) {
        this(id, displayName, geometry, mechanicsConfig, multipliers, teamProvider, antiCheeseValidator, actionPipeline, captureEngine, langManager, OccupancyMode.TEAM, 30L);
    }

    public DefaultOutpostArena(
            @NotNull String id,
            @NotNull Component displayName,
            @NotNull ArenaGeometry geometry,
            @NotNull ArenaMechanicsConfig mechanicsConfig,
            @NotNull ArenaMultipliers multipliers,
            @NotNull TeamRosterProvider teamProvider,
            @NotNull AntiCheeseValidator antiCheeseValidator,
            @NotNull ActionPipeline actionPipeline,
            @NotNull CaptureModeEngine captureEngine,
            @NotNull LangManager langManager,
            @NotNull OccupancyMode occupancyMode
    ) {
        this(id, displayName, geometry, mechanicsConfig, multipliers, teamProvider, antiCheeseValidator, actionPipeline, captureEngine, langManager, occupancyMode, 30L);
    }

    public DefaultOutpostArena(
            @NotNull String id,
            @NotNull Component displayName,
            @NotNull ArenaGeometry geometry,
            @NotNull ArenaMechanicsConfig mechanicsConfig,
            @NotNull ArenaMultipliers multipliers,
            @NotNull TeamRosterProvider teamProvider,
            @NotNull AntiCheeseValidator antiCheeseValidator,
            @NotNull ActionPipeline actionPipeline,
            @NotNull CaptureModeEngine captureEngine,
            @NotNull LangManager langManager,
            @NotNull OccupancyMode occupancyMode,
            long rewardIntervalSeconds
    ) {
        this.id = id;
        this.displayName = displayName;
        this.serializedDisplayName = miniMessage.serialize(displayName);
        this.geometry = geometry;
        this.mechanicsConfig = mechanicsConfig;
        this.multipliers = multipliers;
        this.teamProvider = teamProvider;
        this.antiCheeseValidator = antiCheeseValidator;
        this.actionPipeline = actionPipeline;
        this.captureEngine = captureEngine;
        this.langManager = langManager;
        this.occupancyMode = occupancyMode != null ? occupancyMode : OccupancyMode.TEAM;
        this.rewardIntervalSeconds = Math.max(1, rewardIntervalSeconds);
        if (mechanicsConfig.getMode() == CaptureModeType.TUG_OF_WAR && this.controllerTeamId == null) {
            this.progress = mechanicsConfig.getNeutralAnchorPercent();
            this.state = ArenaState.NEUTRAL;
        }
        updateSnapshot();
    }

    @Override
    public String getSerializedDisplayName() {
        return serializedDisplayName;
    }

    @Override
    public OccupancyMode getOccupancyMode() {
        return occupancyMode;
    }

    @Override
    public String getId() { return id; }

    @Override
    public Component getDisplayName() { return displayName; }

    @Override
    public String getWorldName() { return geometry.getWorldName(); }

    @Override
    public int getMinX() { return geometry.getMinX(); }

    @Override
    public int getMinY() { return geometry.getMinY(); }

    @Override
    public int getMinZ() { return geometry.getMinZ(); }

    @Override
    public int getMaxX() { return geometry.getMaxX(); }

    @Override
    public int getMaxY() { return geometry.getMaxY(); }

    @Override
    public int getMaxZ() { return geometry.getMaxZ(); }

    @Override
    public CaptureModeType getCaptureModeType() { return mechanicsConfig.getMode(); }

    @Override
    public double getMultiplier(String key) { return multipliers.getMultiplier(key); }

    @Override
    public ArenaState getState() { return state; }

    @Override
    public double getProgress() { return progress; }

    @Override
    public @Nullable String getControllerTeamId() { return controllerTeamId; }

    @Override
    public @Nullable String getControllerTeamName() { return controllerTeamName; }

    @Override
    public @Nullable String getCappingTeamId() { return cappingTeamId; }

    public @Nullable String getCappingTeamName() { return cappingTeamName; }

    @Override
    public int getCapperCount() { return capperCount; }

    @Override
    public boolean isContested() { return isContested; }

    @Override
    public boolean isLocked() { return lockoutRemainingSeconds > 0; }

    @Override
    public long getLockoutRemainingSeconds() { return lockoutRemainingSeconds; }

    public long getTimeControlledSeconds() { return timeControlledSeconds; }

    @Override
    public boolean isBoundingParticlesEnabled() {
        return boundingParticlesEnabled;
    }

    @Override
    public void setBoundingParticlesEnabled(boolean enabled) {
        this.boundingParticlesEnabled = enabled;
    }

    public int getBossbarRangeBlocks() {
        if (customBossbarRange != null) return customBossbarRange;
        me.goosbanny.outposts.Outposts plugin = me.goosbanny.outposts.Outposts.getInstance();
        return plugin != null ? plugin.getConfigManager().getBossbarRangeBlocks() : 0;
    }

    public void setCustomBossbarRange(@Nullable Integer range) {
        this.customBossbarRange = range;
    }

    public int getBoundaryRenderDistance() {
        if (customBoundaryRenderDistance != null) return customBoundaryRenderDistance;
        me.goosbanny.outposts.Outposts plugin = me.goosbanny.outposts.Outposts.getInstance();
        return plugin != null ? plugin.getConfigManager().getBoundaryRenderDistance() : 48;
    }

    public void setCustomBoundaryRenderDistance(@Nullable Integer dist) {
        this.customBoundaryRenderDistance = dist;
    }

    public boolean isPreventChorusFruit() {
        if (customPreventChorusFruit != null) return customPreventChorusFruit;
        me.goosbanny.outposts.Outposts plugin = me.goosbanny.outposts.Outposts.getInstance();
        return plugin != null && plugin.getConfigManager().isPreventChorusFruit();
    }

    public void setCustomPreventChorusFruit(@Nullable Boolean prevent) {
        this.customPreventChorusFruit = prevent;
    }

    public boolean isPreventElytraFlight() {
        if (customPreventElytraFlight != null) return customPreventElytraFlight;
        me.goosbanny.outposts.Outposts plugin = me.goosbanny.outposts.Outposts.getInstance();
        return plugin != null && plugin.getConfigManager().isPreventElytraFlight();
    }

    public void setCustomPreventElytraFlight(@Nullable Boolean prevent) {
        this.customPreventElytraFlight = prevent;
    }

    public boolean isPreventBlockBreak() {
        if (customPreventBlockBreak != null) return customPreventBlockBreak;
        me.goosbanny.outposts.Outposts plugin = me.goosbanny.outposts.Outposts.getInstance();
        return plugin != null && plugin.getConfigManager().isPreventBlockBreak();
    }

    public void setCustomPreventBlockBreak(@Nullable Boolean prevent) {
        this.customPreventBlockBreak = prevent;
    }

    public boolean isPreventBlockPlace() {
        if (customPreventBlockPlace != null) return customPreventBlockPlace;
        me.goosbanny.outposts.Outposts plugin = me.goosbanny.outposts.Outposts.getInstance();
        return plugin != null && plugin.getConfigManager().isPreventBlockPlace();
    }

    public void setCustomPreventBlockPlace(@Nullable Boolean prevent) {
        this.customPreventBlockPlace = prevent;
    }

    public @Nullable String getCustomTeamProvider() {
        return customTeamProvider;
    }

    public void setCustomTeamProvider(@Nullable String provider) {
        this.customTeamProvider = provider;
    }

    public Set<UUID> getPlayersInZone() {
        return Collections.unmodifiableSet(playersInZone);
    }

    public @Nullable String getClearingTeamName() {
        return clearingTeamName;
    }

    public void setClearingTeamName(@Nullable String clearingTeamName) {
        this.clearingTeamName = clearingTeamName;
    }

    @Override
    public @Nullable Location getCenterLocation() { return geometry.getCenterLocation(); }

    @Override
    public @Nullable Location getWarpLocation() { return geometry.getWarpLocation(); }

    @Override
    public void setWarpLocation(@Nullable Location warpLocation) {
        this.geometry = new ArenaGeometry(
                geometry.getWorldName(),
                geometry.getMinX(), geometry.getMinY(), geometry.getMinZ(),
                geometry.getMaxX(), geometry.getMaxY(), geometry.getMaxZ(),
                warpLocation
        );
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.state = ArenaState.LOCKED;
            Outposts plugin = Outposts.getInstance();
            if (plugin != null) {
                if (plugin.getBossBarManager() != null) {
                    plugin.getBossBarManager().removeArenaBar(this.id);
                }
                if (plugin.getActionBarManager() != null) {
                    plugin.getActionBarManager().clearArena(this.id);
                }
            }
        } else {
            this.state = controllerTeamId != null ? ArenaState.CONTROLLED : ArenaState.NEUTRAL;
        }
        updateSnapshot();
    }

    /**
     * Silently restores a previously controlling team without firing capture events, fanfare, or actions.
     * Used during non-destructive configuration reloads.
     */
    public void restoreController(@NotNull String teamId, @NotNull String teamName) {
        this.controllerTeamId = teamId;
        this.controllerTeamName = teamName;
        this.state = ArenaState.CONTROLLED;
        updateSnapshot();
    }

    /**
     * Seamlessly copies all live runtime gameplay state from an existing arena instance.
     * Preserves controllers, partial progress, active cappers, lockouts, time controlled,
     * contestation state, particles, and dynamic regions during configuration hot-reloads.
     */
    public void copyRuntimeStateFrom(@NotNull OutpostArena existing) {
        this.active = existing.isActive();
        this.progress = existing.getProgress();
        this.controllerTeamId = existing.getControllerTeamId();
        this.controllerTeamName = existing.getControllerTeamName();
        this.cappingTeamId = existing.getCappingTeamId();
        this.cappingTeamName = existing.getCappingTeamName();
        this.lockoutRemainingSeconds = existing.getLockoutRemainingSeconds();
        this.isContested = existing.isContested();

        if (existing instanceof DefaultOutpostArena def) {
            this.state = def.state;
            this.timeControlledSeconds = def.timeControlledSeconds;
            this.clearingTeamName = def.clearingTeamName;
            this.lastAnnouncedMilestone = def.lastAnnouncedMilestone;
            this.contestDebounceSeconds = def.contestDebounceSeconds;
            this.invaderHoldSeconds = def.invaderHoldSeconds;
            this.currentRegion = def.currentRegion;
            this.nextShiftSeconds = def.nextShiftSeconds;
            this.activationGraceRemainingSeconds = def.activationGraceRemainingSeconds;
        } else {
            if (this.lockoutRemainingSeconds > 0) {
                this.state = ArenaState.LOCKED;
            } else if (this.isContested) {
                this.state = ArenaState.CONTESTED;
            } else if (this.controllerTeamId != null) {
                this.state = ArenaState.CONTROLLED;
            } else if (this.progress > 0.0) {
                this.state = ArenaState.CAPTURING;
            } else {
                this.state = ArenaState.NEUTRAL;
            }
        }
        updateSnapshot();
    }

    public void setLockoutRemainingSeconds(long v) {
        this.lockoutRemainingSeconds = Math.max(0, v);
    }

    public ArenaGeometry getGeometry() { return geometry; }

    public ArenaMechanicsConfig getMechanicsConfig() { return mechanicsConfig; }

    public ArenaMultipliers getMultipliers() { return multipliers; }

    public @NotNull CaptureModeEngine getCaptureEngine() {
        return captureEngine;
    }

    public void setCaptureEngine(@NotNull CaptureModeEngine captureEngine) {
        this.captureEngine = captureEngine;
    }

    public void setCappingTeam(@Nullable String teamId, @Nullable String teamName) {
        this.cappingTeamId = teamId;
        this.cappingTeamName = teamName;
    }

    public boolean isKnockDelayActive() {
        return controllerTeamId != null && invaderHoldSeconds < mechanicsConfig.getKnockDelaySeconds();
    }

    public long getInvaderHoldSeconds() {
        return invaderHoldSeconds;
    }

    @Override
    public boolean isWithinBounds(int x, int y, int z) {
        return geometry.isWithinBounds(x, y, z);
    }

    @Override
    public ArenaView createSnapshot() {
        if (snapshotDirty || cachedSnapshot.get() == null) {
            cachedSnapshot.set(new ArenaViewSnapshot(
                    id,
                    displayName,
                    state,
                    progress,
                    controllerTeamName != null ? controllerTeamName : controllerTeamId,
                    cappingTeamName != null ? cappingTeamName : cappingTeamId,
                    capperCount,
                    isContested,
                    lockoutRemainingSeconds,
                    occupancyMode
            ));
            snapshotDirty = false;
        }
        return cachedSnapshot.get();
    }

    private void updateSnapshot() {
        this.snapshotDirty = true;
    }

    @Override
    public void setProgress(double newProgress) {
        double clamped = Math.max(0.0, Math.min(100.0, newProgress));
        if (clamped < 1e-4) clamped = 0.0;
        if (clamped > 100.0 - 1e-4) clamped = 100.0;
        if (Math.abs(this.progress - clamped) > 0.001) {
            double old = this.progress;
            this.progress = clamped;
            Bukkit.getPluginManager().callEvent(new OutpostProgressChangeEvent(this, old, clamped));
        }
    }

    @Override
    public void setController(@NotNull String teamId, @NotNull String teamName, @Nullable UUID capturer) {
        OutpostCaptureEvent event = new OutpostCaptureEvent(this, teamId, teamName, capturer);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        // Enforce max outposts limit and capture cooldown if enabled
        Outposts plugin = Outposts.getInstance();
        if (plugin != null && plugin.getConfigManager().isMaxOutpostsEnabled()) {
            long cooldownSec = plugin.getConfigManager().getMaxOutpostsCooldownSeconds();
            if (cooldownSec > 0) {
                Long lastCapture = LAST_CAPTURE_TIMES.get(teamId.toLowerCase());
                long now = System.currentTimeMillis();
                if (lastCapture != null && (now - lastCapture) < (cooldownSec * 1000L)) {
                    if (capturer != null) {
                        Player p = Bukkit.getPlayer(capturer);
                        if (p != null && p.isOnline()) {
                            long remSec = Math.max(1, (cooldownSec * 1000L - (now - lastCapture)) / 1000L);
                            p.sendMessage(langManager.get("commands.capture_cooldown", Map.of("seconds", String.valueOf(remSec))));
                        }
                    }
                    this.progress = 0.0;
                    this.cappingTeamId = null;
                    this.cappingTeamName = null;
                    this.state = ArenaState.NEUTRAL;
                    updateSnapshot();
                    return;
                }
            }

            int limit = plugin.getConfigManager().getMaxOutpostsLimit();
            int currentCount = 0;
            for (OutpostArena arena : plugin.getArenaManager().getArenas()) {
                if (!arena.getId().equalsIgnoreCase(this.id) && teamId.equalsIgnoreCase(arena.getControllerTeamId())) {
                    currentCount++;
                }
            }
            if (currentCount >= limit) {
                if (capturer != null) {
                    Player p = Bukkit.getPlayer(capturer);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(langManager.get("commands.max_outposts_reached"));
                    }
                }
                this.progress = 0.0;
                this.cappingTeamId = null;
                this.cappingTeamName = null;
                this.state = ArenaState.NEUTRAL;
                updateSnapshot();
                return;
            }
        }

        LAST_CAPTURE_TIMES.put(teamId.toLowerCase(), System.currentTimeMillis());

        this.controllerTeamId = teamId;
        this.controllerTeamName = teamName;
        this.cappingTeamId = null;
        if (mechanicsConfig.getMode() != CaptureModeType.TUG_OF_WAR) {
            this.progress = 100.0;
        }
        this.state = ArenaState.CONTROLLED;
        this.timeControlledSeconds = 0;
        this.invaderHoldSeconds = 0;
        this.lockoutRemainingSeconds = mechanicsConfig.getLockoutSeconds();
        this.lastAnnouncedMilestone = 0;

        reusableContext.clear();
        reusableContext.put("team", teamName);
        reusableContext.put("team_id", teamId);
        if (capturer != null) {
            Player capturerPlayer = Bukkit.getPlayer(capturer);
            reusableContext.put("player", capturerPlayer != null ? capturerPlayer.getName() : capturer.toString());
        }
        actionPipeline.dispatch(ActionTrigger.ON_CAPTURE, this, reusableContext);
        audioCueManager.playCaptureFanfare(this);
        updateSnapshot();

        if (dynamicLocationConfig.isEnabled() && dynamicLocationConfig.isSwitchOnCapture()) {
            shiftToRegion(null);
        }
    }

    @Override
    public void resetToNeutral() {
        String prevTeamId = this.controllerTeamId;
        String prevTeamName = this.controllerTeamName;

        this.controllerTeamId = null;
        this.controllerTeamName = null;
        this.cappingTeamId = null;
        this.cappingTeamName = null;
        this.progress = mechanicsConfig.getMode() == CaptureModeType.TUG_OF_WAR ? mechanicsConfig.getNeutralAnchorPercent() : 0.0;
        this.state = ArenaState.NEUTRAL;
        this.timeControlledSeconds = 0;
        this.invaderHoldSeconds = 0;
        this.lastAnnouncedMilestone = 0;

        if (captureEngine instanceof me.goosbanny.outposts.core.mechanics.TugOfWarEngine tow) {
            tow.resetSides();
        }

        if (prevTeamId != null) {
            Bukkit.getPluginManager().callEvent(new OutpostLostEvent(this, prevTeamId, prevTeamName));
            reusableContext.clear();
            reusableContext.put("team", prevTeamName != null ? prevTeamName : prevTeamId);
            reusableContext.put("team_id", prevTeamId);
            actionPipeline.dispatch(ActionTrigger.ON_LOST, this, reusableContext);
        }
        updateSnapshot();
    }

    @Override
    public void tickGameLoop() {
        if (!active || !mechanicsConfig.isEnabled()) {
            return;
        }

        tickCounter++;

        // Dynamic Shifting: Activation grace / warmup period
        if (activationGraceRemainingSeconds > 0) {
            activationGraceRemainingSeconds--;
            updateSnapshot();
            return;
        }

        // Dynamic Shifting: Countdown and automatic relocation
        if (dynamicLocationConfig.isEnabled() && nextShiftSeconds > 0) {
            nextShiftSeconds--;
            int secs = (int) nextShiftSeconds;
            if (dynamicLocationConfig.getWarningSeconds().contains(secs) && !dispatchedShiftWarnings.contains(secs)) {
                dispatchedShiftWarnings.add(secs);
                dispatchShiftWarning(secs);
            }
            if (nextShiftSeconds <= 0) {
                shiftToRegion(null);
                return;
            }
        }

        // 1. Lockout countdown
        if (lockoutRemainingSeconds > 0) {
            lockoutRemainingSeconds--;
            if (lockoutRemainingSeconds <= 0) {
                lockoutRemainingSeconds = 0;
                this.state = controllerTeamId != null ? ArenaState.CONTROLLED : ArenaState.NEUTRAL;
            } else {
                this.state = ArenaState.LOCKED;
                updateSnapshot();
                return;
            }
        }

        // 2. Controlled time tracking & periodic rewards
        if (controllerTeamId != null) {
            timeControlledSeconds++;
            if (!isContested && rewardIntervalSeconds > 0 && timeControlledSeconds % rewardIntervalSeconds == 0) {
                reusableContext.clear();
                reusableContext.put("team", controllerTeamName != null ? controllerTeamName : controllerTeamId);
                reusableContext.put("team_id", controllerTeamId);
                actionPipeline.dispatch(ActionTrigger.ON_TICK_REWARD, this, reusableContext);
            }
        }

        // 3. Find nearby players safely via world.getPlayers() (Folia cross-region safe)
        World world = Bukkit.getWorld(geometry.getWorldName());
        if (world == null) {
            return;
        }

        validCappers.clear();
        for (List<Player> list : teamsPresent.values()) {
            list.clear();
        }
        teamsPresent.clear();

        List<Player> nearbyCandidates = new ArrayList<>();
        try {
            BoundingBox box = new BoundingBox(
                    geometry.getMinX(), geometry.getMinY(), geometry.getMinZ(),
                    geometry.getMaxX() + 1.0, geometry.getMaxY() + 1.0, geometry.getMaxZ() + 1.0
            );
            Collection<Entity> entities = world.getNearbyEntities(box, e -> e instanceof Player);
            for (Entity e : entities) {
                if (e instanceof Player p && p.isOnline() && !p.isDead()) {
                    if (FoliaCompatScheduler.isFolia() && !FoliaCompatScheduler.isOwnedByCurrentRegion(p)) {
                        continue;
                    }
                    nearbyCandidates.add(p);
                }
            }
        } catch (Exception e) {
            // Region-guarded fallback safe on Folia
            for (Player player : world.getPlayers()) {
                if (player.isOnline() && !player.isDead()) {
                    if (FoliaCompatScheduler.isFolia()
                            && !FoliaCompatScheduler.isOwnedByCurrentRegion(player)) {
                        continue;
                    }
                    Location loc = player.getLocation();
                    if (loc.getWorld() != null && loc.getWorld().equals(world)
                            && geometry.isWithinBounds(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                        nearbyCandidates.add(player);
                    }
                }
            }
        }

        playersInZone.clear();
        for (Player p : nearbyCandidates) {
            playersInZone.add(p.getUniqueId());
        }

        for (Player player : nearbyCandidates) {
            if (occupancyMode == OccupancyMode.TEAM && !teamProvider.hasTeam(player)) {
                continue; // In team mode, only rostered team members can capture or contest
            }
            if (antiCheeseValidator.isValidCapper(this, player)) {
                String participantId = occupancyMode == OccupancyMode.SOLO
                        ? player.getUniqueId().toString()
                        : (teamProvider.getTeamId(player) != null
                                ? teamProvider.getTeamId(player)
                                : player.getUniqueId().toString());
                validCappers.add(player);
                teamsPresent.computeIfAbsent(participantId, k -> new ArrayList<>()).add(player);
            }
        }

        this.capperCount = validCappers.size();

        // 4. Track invader presence for knock_delay_seconds grace
        if (controllerTeamId != null && !validCappers.isEmpty()) {
            boolean invaderPresent = false;
            for (String tid : teamsPresent.keySet()) {
                if (!tid.equalsIgnoreCase(controllerTeamId) && !teamProvider.areAllies(tid, controllerTeamId)) {
                    invaderPresent = true;
                    break;
                }
            }
            if (invaderPresent) {
                invaderHoldSeconds++;
            } else {
                invaderHoldSeconds = 0;
            }
        } else {
            invaderHoldSeconds = 0;
        }

        // 5. Contest Evaluation with anti-ally stalling
        boolean currentlyContested = false;
        if (teamsPresent.size() > 1) {
            if (occupancyMode == OccupancyMode.SOLO) {
                currentlyContested = true;
            } else {
                presentTeamIds.clear();
                presentTeamIds.addAll(teamsPresent.keySet());
                for (int i = 0; i < presentTeamIds.size(); i++) {
                    for (int j = i + 1; j < presentTeamIds.size(); j++) {
                        String a = presentTeamIds.get(i);
                        String b = presentTeamIds.get(j);
                        if (!teamProvider.areAllies(a, b)) {
                            currentlyContested = true;
                            break;
                        }
                    }
                    if (currentlyContested) break;
                }
            }
        }

        // 5. Contestation State Transition & Debounce Hysteresis
        if (currentlyContested) {
            // Contestation takes effect IMMEDIATELY: progress freezes instantly, HUD reflects immediately
            contestDebounceSeconds = 0;
            if (!this.isContested) {
                this.isContested = true;
                Bukkit.getPluginManager().callEvent(new OutpostContestEvent(this, true));
                reusableContext.clear();
                actionPipeline.dispatch(ActionTrigger.ON_CONTEST, this, reusableContext);
            }
        } else {
            // Leaving contestation applies debounce cooldown to prevent edge-knockback jitter
            if (this.isContested) {
                contestDebounceSeconds++;
                long cooldownSecs = Math.max(1L, (long) Math.ceil(mechanicsConfig.getStateChangeCooldownSeconds()));
                if (contestDebounceSeconds >= cooldownSecs) {
                    this.isContested = false;
                    contestDebounceSeconds = 0;
                    Bukkit.getPluginManager().callEvent(new OutpostContestEvent(this, false));
                }
            } else {
                contestDebounceSeconds = 0;
            }
        }

        // 6. Capture Mechanics evaluation
        if (validCappers.isEmpty()) {
            captureEngine.handleAbandonment(this);
        } else {
            captureEngine.evaluateCapture(this, validCappers, this.isContested);
            if (!this.isContested && progress > 0.0 && progress < 100.0) {
                audioCueManager.playProgressTick(this, validCappers);
            }
        }

        // 7. Milestone Broadcasts (25%, 50%, 75%)
        if (cappingTeamName != null && progress > 0.0 && lastAnnouncedMilestone < 75) {
            int[] milestones = {25, 50, 75};
            for (int m : milestones) {
                if (progress >= m && lastAnnouncedMilestone < m) {
                    lastAnnouncedMilestone = m;
                    Map<String, String> tokens = new HashMap<>();
                    tokens.put("name", miniMessage.serialize(displayName));
                    tokens.put("milestone", String.valueOf(m));
                    if (occupancyMode == OccupancyMode.SOLO) {
                        tokens.put("player", cappingTeamName);
                        tokens.put("team", cappingTeamName);
                        String customSolo = langManager.getRaw("broadcasts.milestone_solo", "");
                        if (!customSolo.isBlank()) {
                            Bukkit.broadcast(langManager.get("broadcasts.milestone_solo", tokens));
                        } else {
                            String msg = langManager.getRaw("broadcasts.milestone", "<prefix><white>Player <#98fc98>%team%</#98fc98> reached <#FF6C00>%milestone%%</#FF6C00> progress on <white>%name%</white>!</white>");
                            msg = msg.replace("The <#98fc98>%team%</#98fc98> team", "Player <#98fc98>%team%</#98fc98>");
                            msg = msg.replace("The %team% team", "Player %team%");
                            msg = msg.replace("<#98fc98>%team%</#98fc98> faction", "<#98fc98>%team%</#98fc98>");
                            msg = msg.replace("%team% faction", "%team%");
                            Bukkit.broadcast(langManager.formatComponent(msg, tokens));
                        }
                    } else {
                        tokens.put("team", cappingTeamName);
                        Bukkit.broadcast(langManager.get("broadcasts.milestone", tokens));
                    }
                    break;
                }
            }
        }
        if (progress < lastAnnouncedMilestone - 5.0) {
            lastAnnouncedMilestone = (int) (progress / 25) * 25;
        }

        // 8. Anti-Jitter Hysteresis Buffer
        // If controlled, defending team retains control until progress drops below (100.0 - hysteresisBufferPercent)
        if (controllerTeamId != null) {
            double threshold = mechanicsConfig.getLoseControlThreshold() - mechanicsConfig.getHysteresisBufferPercent();
            if (progress <= 0.0) {
                resetToNeutral();
            } else if (progress < threshold && !isContested && cappingTeamId != null && !cappingTeamId.equals(controllerTeamId)) {
                // Invader knocked below hysteresis threshold
            }
        }

        // 8. Update State
        if (lockoutRemainingSeconds > 0) {
            this.state = ArenaState.LOCKED;
        } else if (isContested) {
            this.state = ArenaState.CONTESTED;
        } else if (controllerTeamId != null) {
            this.state = ArenaState.CONTROLLED;
        } else if (cappingTeamId != null && progress > 0.0) {
            this.state = ArenaState.CAPTURING;
        } else {
            this.state = ArenaState.NEUTRAL;
        }

        updateSnapshot();
    }

    @Override
    public @NotNull DynamicLocationConfig getDynamicLocationConfig() {
        return dynamicLocationConfig;
    }

    public void setDynamicLocationConfig(@NotNull DynamicLocationConfig config) {
        this.dynamicLocationConfig = config;
        if (config.isEnabled() && !config.getRegions().isEmpty()) {
            if (this.currentRegion == null) {
                this.currentRegion = config.getRegions().get(0);
                this.geometry = this.currentRegion.getGeometry();
            }
            calculateNextShiftInterval();
        }
    }

    @Override
    public @Nullable ArenaRegion getCurrentRegion() {
        return currentRegion;
    }

    public void setCurrentRegion(@Nullable ArenaRegion region) {
        this.currentRegion = region;
        if (region != null) {
            this.geometry = region.getGeometry();
        }
    }

    @Override
    public long getNextShiftSeconds() {
        return dynamicLocationConfig.isEnabled() ? nextShiftSeconds : -1;
    }

    @Override
    public int getActivationGraceRemainingSeconds() {
        return activationGraceRemainingSeconds;
    }

    @Override
    public boolean isWarmingUp() {
        return activationGraceRemainingSeconds > 0;
    }

    @Override
    public @NotNull Map<String, String> getCustomLangOverrides() {
        return Collections.unmodifiableMap(customLangOverrides);
    }

    @Override
    public void setCustomLangOverrides(@NotNull Map<String, String> overrides) {
        this.customLangOverrides.clear();
        this.customLangOverrides.putAll(overrides);
    }

    @Override
    public @Nullable String getCustomLang(@NotNull String path) {
        return customLangOverrides.get(path);
    }

    public String getFormattedCoords() {
        Location center = getCenterLocation();
        if (center == null) return "0, 0, 0";
        String format = langManager.getRaw("placeholders.coords_format", this, "%x%, %y%, %z%");
        return format
                .replace("%x%", String.valueOf(center.getBlockX()))
                .replace("%y%", String.valueOf(center.getBlockY()))
                .replace("%z%", String.valueOf(center.getBlockZ()))
                .replace("%world%", geometry.getWorldName());
    }

    public void calculateNextShiftInterval() {
        dispatchedShiftWarnings.clear();
        switch (dynamicLocationConfig.getSwitchMode()) {
            case INTERVAL -> this.nextShiftSeconds = dynamicLocationConfig.getIntervalSeconds();
            case RANDOM_INTERVAL -> {
                long min = dynamicLocationConfig.getMinIntervalSeconds();
                long max = dynamicLocationConfig.getMaxIntervalSeconds();
                this.nextShiftSeconds = min + (long) (random.nextDouble() * (max - min));
            }
            case SCHEDULED_TIMESTAMPS -> {
                List<Long> stamps = dynamicLocationConfig.getScheduledTimestamps();
                long next = -1;
                for (Long stamp : stamps) {
                    if (stamp > timeControlledSeconds) {
                        next = stamp - timeControlledSeconds;
                        break;
                    }
                }
                this.nextShiftSeconds = next > 0 ? next : dynamicLocationConfig.getIntervalSeconds();
            }
            case ON_CAPTURE -> this.nextShiftSeconds = -1;
        }
    }

    private void dispatchShiftWarning(int secondsRemaining) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("name", miniMessage.serialize(displayName));
        tokens.put("id", id);
        tokens.put("seconds", String.valueOf(secondsRemaining));
        tokens.put("coords", getFormattedCoords());
        if (currentRegion != null) {
            tokens.put("region", miniMessage.serialize(currentRegion.getDisplayName()));
            tokens.put("region_name", miniMessage.serialize(currentRegion.getDisplayName()));
            tokens.put("region_id", currentRegion.getId());
        }
        Bukkit.broadcast(langManager.get("broadcasts.shift_warning", this, tokens));
    }

    @Override
    public void shiftToRegion(@Nullable ArenaRegion target) {
        if (!dynamicLocationConfig.isEnabled()) {
            return;
        }

        ArenaRegion toRegion = target != null ? target : dynamicLocationConfig.selectNextRegion(currentRegion, random);
        if (toRegion == null) {
            return;
        }

        OutpostPreShiftEvent preEvent =
                new OutpostPreShiftEvent(this, currentRegion, toRegion);
        Bukkit.getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) {
            return;
        }
        toRegion = preEvent.getToRegion();

        // 1. Gather players in old zone if player transition needed (Folia safe)
        List<Player> playersInOldZone = new ArrayList<>();
        World oldWorld = Bukkit.getWorld(geometry.getWorldName());
        if (oldWorld != null) {
            try {
                BoundingBox oldBox = new BoundingBox(
                        geometry.getMinX(), geometry.getMinY(), geometry.getMinZ(),
                        geometry.getMaxX() + 1.0, geometry.getMaxY() + 1.0, geometry.getMaxZ() + 1.0
                );
                for (Entity e : oldWorld.getNearbyEntities(oldBox, entity -> entity instanceof Player)) {
                    if (e instanceof Player p && p.isOnline() && !p.isDead()) {
                        if (FoliaCompatScheduler.isFolia() && !FoliaCompatScheduler.isOwnedByCurrentRegion(p)) {
                            continue;
                        }
                        playersInOldZone.add(p);
                    }
                }
            } catch (Exception e) {
                for (Player p : oldWorld.getPlayers()) {
                    if (p.isOnline() && !p.isDead()) {
                        if (FoliaCompatScheduler.isFolia()
                                && !FoliaCompatScheduler.isOwnedByCurrentRegion(p)) {
                            continue;
                        }
                        Location loc = p.getLocation();
                        if (geometry.isWithinBounds(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                            playersInOldZone.add(p);
                        }
                    }
                }
            }
        }

        // 2. Clear visual trackers for old zone
        Outposts plugin = Outposts.getInstance();
        if (plugin != null) {
            if (plugin.getBossBarManager() != null) {
                plugin.getBossBarManager().removeArenaBar(this.id);
            }
            if (plugin.getActionBarManager() != null) {
                plugin.getActionBarManager().clearArena(this.id);
            }
        }

        // 3. Reset live contestation, capper, and debounce state
        this.isContested = false;
        this.contestDebounceSeconds = 0;
        this.invaderHoldSeconds = 0;
        this.capperCount = 0;
        this.validCappers.clear();
        this.teamsPresent.clear();
        this.antiCheeseValidator.clearAll();

        ArenaRegion fromRegion = this.currentRegion;
        final ArenaRegion selected = toRegion;

        // 4. Re-index spatial chunks via ArenaManager
        if (plugin != null && plugin.getArenaManager() != null) {
            plugin.getArenaManager().notifyArenaRelocated(this, () -> {
                this.currentRegion = selected;
                this.geometry = selected.getGeometry();
            });
        } else {
            this.currentRegion = selected;
            this.geometry = selected.getGeometry();
        }

        // 5. Apply state transition
        switch (dynamicLocationConfig.getStateTransition()) {
            case RESET_PROGRESS -> resetToNeutral();
            case RESET_IF_UNCONTROLLED -> {
                if (controllerTeamId == null) {
                    resetToNeutral();
                } else if (dynamicLocationConfig.getPlayerTransition() == DynamicLocationConfig.PlayerTransition.NONE) {
                    this.cappingTeamId = null;
                    this.cappingTeamName = null;
                }
            }
            case KEEP_PROGRESS -> {
                if (dynamicLocationConfig.getPlayerTransition() == DynamicLocationConfig.PlayerTransition.NONE) {
                    this.cappingTeamId = null;
                    this.cappingTeamName = null;
                }
            }
        }

        // 4. Activation grace period / warmup
        this.activationGraceRemainingSeconds = dynamicLocationConfig.getActivationGraceSeconds();

        // 5. Reset timer for subsequent shift
        calculateNextShiftInterval();

        // 6. Visual & audio effects
        if (dynamicLocationConfig.isEffectsEnabled()) {
            World w = Bukkit.getWorld(geometry.getWorldName());
            Location center = getCenterLocation();
            if (w != null && center != null) {
                try {
                    Sound snd = Sound.valueOf(dynamicLocationConfig.getSoundEffect());
                    w.playSound(center, snd, 1.5f, 1.0f);
                } catch (Exception ignored) {}

                if (dynamicLocationConfig.isBeaconBeam()) {
                    for (int y = center.getBlockY(); y < Math.min(320, center.getBlockY() + 50); y += 2) {
                        w.spawnParticle(Particle.END_ROD, center.getX(), y, center.getZ(), 4, 0.2, 0.5, 0.2, 0.01);
                    }
                }
            }
        }

        // 7. Player transition (teleporting active cappers to new pad)
        if (dynamicLocationConfig.getPlayerTransition() == DynamicLocationConfig.PlayerTransition.TELEPORT_TO_NEW) {
            Location newWarp = getWarpLocation();
            if (newWarp == null) newWarp = getCenterLocation();
            if (newWarp != null) {
                for (Player p : playersInOldZone) {
                    p.teleportAsync(newWarp);
                    int invincibility = dynamicLocationConfig.getWarpInvincibilitySeconds();
                    if (invincibility > 0 && plugin != null && plugin.getWarpProtectionListener() != null) {
                        plugin.getWarpProtectionListener().applyProtection(p, invincibility);
                    }
                }
            }
        }

        // 8. Relocation broadcast with rich tokens (%coords%, %x%, %y%, %z%, %region%, %region_name%, %region_id%)
        Map<String, String> tokens = new HashMap<>();
        tokens.put("name", miniMessage.serialize(displayName));
        tokens.put("id", id);
        tokens.put("region", miniMessage.serialize(toRegion.getDisplayName()));
        tokens.put("region_name", miniMessage.serialize(toRegion.getDisplayName()));
        tokens.put("region_id", toRegion.getId());
        tokens.put("coords", getFormattedCoords());
        Location center = getCenterLocation();
        if (center != null) {
            tokens.put("x", String.valueOf(center.getBlockX()));
            tokens.put("y", String.valueOf(center.getBlockY()));
            tokens.put("z", String.valueOf(center.getBlockZ()));
            tokens.put("world", geometry.getWorldName());
        }
        Bukkit.broadcast(langManager.get("broadcasts.shifted", this, tokens));

        // 9. Fire shift event
        OutpostShiftEvent shiftEvent =
                new OutpostShiftEvent(this, fromRegion, toRegion);
        Bukkit.getPluginManager().callEvent(shiftEvent);

        updateSnapshot();
    }
}
