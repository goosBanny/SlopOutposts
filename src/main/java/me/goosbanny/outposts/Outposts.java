package me.goosbanny.outposts;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.command.OutpostCommand;
import me.goosbanny.outposts.command.OutpostWandListener;
import me.goosbanny.outposts.command.SystemDoctor;
import me.goosbanny.outposts.config.ArenaSerializer;
import me.goosbanny.outposts.config.ConfigManager;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.anticheese.AntiCheeseConfig;
import me.goosbanny.outposts.core.anticheese.AntiCheeseListener;
import me.goosbanny.outposts.core.anticheese.AntiCheeseValidator;
import me.goosbanny.outposts.core.arena.DefaultOutpostArena;
import me.goosbanny.outposts.core.feedback.ActionBarManager;
import me.goosbanny.outposts.core.feedback.AudioCueManager;
import me.goosbanny.outposts.core.feedback.BossBarManager;
import me.goosbanny.outposts.core.feedback.PerimeterRenderer;
import me.goosbanny.outposts.core.manager.ArenaManager;
import me.goosbanny.outposts.core.manager.SpatialGridManager;
import me.goosbanny.outposts.core.anticheese.OutpostWarpProtectionListener;
import me.goosbanny.outposts.core.multipliers.MultiplierListener;
import java.util.Set;
import java.util.UUID;
import me.goosbanny.outposts.core.schedule.ScheduleManager;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import me.goosbanny.outposts.core.scheduler.OutpostsAsyncWorker;
import me.goosbanny.outposts.hook.placeholder.OutpostsPlaceholderExpansion;
import me.goosbanny.outposts.hook.shop.ShopGUIPlusHookListener;
import me.goosbanny.outposts.hook.team.TeamHookManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * Outposts v1.0.0 — Territory Control & Arena Engine
 * Main plugin entrypoint and orchestrator.
 */
public final class Outposts extends JavaPlugin {

    private static Outposts instance;

    private FoliaCompatScheduler scheduler;
    private ConfigManager configManager;
    private LangManager langManager;
    private SpatialGridManager spatialGridManager;
    private TeamHookManager teamHookManager;
    private ArenaSerializer arenaSerializer;
    private ArenaManager arenaManager;

    private BossBarManager bossBarManager;
    private ActionBarManager actionBarManager;
    private AudioCueManager audioCueManager;
    private PerimeterRenderer perimeterRenderer;
    private ScheduleManager scheduleManager;

    private OutpostWandListener wandListener;
    private SystemDoctor systemDoctor;
    private OutpostWarpProtectionListener warpProtectionListener;
    private OutpostsAsyncWorker asyncWorker;
    private FoliaCompatScheduler.TaskHandle displayTask;

    public static Outposts getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        long startMillis = System.currentTimeMillis();

        getLogger().info("=================================================");
        getLogger().info(" Outposts (v" + getDescription().getVersion() + ") — Territory Arena Engine");
        getLogger().info(" Platform: " + (FoliaCompatScheduler.isFolia() ? "Folia (Threaded Regions)" : "Paper/Bukkit"));
        getLogger().info("=================================================");

        // 1. Schedulers & Configuration & Localization
        this.scheduler = new FoliaCompatScheduler(this);
        this.asyncWorker = new OutpostsAsyncWorker();
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();

        this.langManager = new LangManager(this);
        langManager.load();

        // 2. Spatial Grid & Team Hooks
        this.spatialGridManager = new SpatialGridManager();
        this.teamHookManager = new TeamHookManager(getLogger());
        teamHookManager.detectAndInitialize(configManager.getPreferredTeamProvider());

        // 3. Serializer & Arena Manager
        this.arenaSerializer = new ArenaSerializer(
                teamHookManager.getRosterProvider(),
                teamHookManager.getEconomyProvider(),
                scheduler,
                langManager
        );
        this.arenaManager = new ArenaManager(spatialGridManager, scheduler);

        // 4. Feedback & Visual Managers
        this.bossBarManager = new BossBarManager(
                teamHookManager.getRosterProvider(),
                langManager,
                configManager.getBoundaryRenderDistance()
        );
        this.actionBarManager = new ActionBarManager(langManager);
        this.audioCueManager = new AudioCueManager();
        this.perimeterRenderer = new PerimeterRenderer(configManager.getBoundaryRenderDistance());

        // 5. Automated Scheduling Engine
        this.scheduleManager = new ScheduleManager(arenaManager, langManager, getLogger());

        // 6. Tooling & Listeners
        this.wandListener = new OutpostWandListener(this, langManager);
        this.systemDoctor = new SystemDoctor(arenaManager, spatialGridManager, teamHookManager, scheduleManager);

        // Extract default south outpost sample if outposts directory is empty
        extractDefaultOutposts();

        // Load all arenas from files first
        loadAllArenas();

        // Load and sync schedules against all loaded arenas
        scheduleManager.loadSchedules(configManager.getSchedulesConfig());

        // Register Bukkit event listeners
        AntiCheeseValidator globalAntiCheese = new AntiCheeseValidator(AntiCheeseConfig.createDefault());
        getServer().getPluginManager().registerEvents(
                new AntiCheeseListener(
                        globalAntiCheese,
                        spatialGridManager,
                        langManager,
                        configManager.getBlockedCommands(),
                        configManager.isPreventBlockBreak(),
                        configManager.isPreventBlockPlace(),
                        configManager.isPreventChorusFruit()
                ),
                this
        );
        getServer().getPluginManager().registerEvents(
                new MultiplierListener(arenaManager, spatialGridManager, teamHookManager.getRosterProvider()),
                this
        );
        this.warpProtectionListener = new OutpostWarpProtectionListener(langManager);
        getServer().getPluginManager().registerEvents(warpProtectionListener, this);
        getServer().getPluginManager().registerEvents(wandListener, this);

        // 7. Hook ShopGUI+ if installed
        if (getServer().getPluginManager().isPluginEnabled("ShopGUIPlus")) {
            getServer().getPluginManager().registerEvents(
                    new ShopGUIPlusHookListener(
                            arenaManager,
                            teamHookManager.getRosterProvider(),
                            configManager.getShopGuiPlusMultiplierCap()
                    ),
                    this
            );
            getLogger().info("[Hooks] Hooked into ShopGUI+ (sell multiplier enabled, cap: " + configManager.getShopGuiPlusMultiplierCap() + "x).");
        }

        // 8. Hook PlaceholderAPI if installed
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new OutpostsPlaceholderExpansion(
                    arenaManager,
                    spatialGridManager,
                    teamHookManager.getRosterProvider(),
                    langManager,
                    scheduleManager,
                    getDescription().getVersion()
            ).register();
            getLogger().info("[Hooks] Registered Outposts PlaceholderAPI expansion (%outpost_...%).");
        }

        // 9. Commands & Tab Completion
        OutpostCommand commandHandler = new OutpostCommand(this, systemDoctor, langManager);
        PluginCommand cmd = getCommand("outpost");
        if (cmd != null) {
            cmd.setExecutor(commandHandler);
            cmd.setTabCompleter(commandHandler);
        }

        // 10. Register Player Quit Cleanup (Folia memory leak prevention)
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                if (bossBarManager != null) {
                    bossBarManager.handlePlayerQuit(event.getPlayer().getUniqueId());
                }
            }
        }, this);

        // 11. Start Game Loop & Display Loop
        arenaManager.startTicking(configManager.getEngineTickFrequency());
        startDisplayLoop();

        long elapsed = System.currentTimeMillis() - startMillis;
        getLogger().info("Successfully loaded " + arenaManager.getArenas().size() + " outpost arenas in " + elapsed + "ms!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down Outposts territory engine...");

        if (displayTask != null) {
            displayTask.cancel();
            displayTask = null;
        }

        if (asyncWorker != null) {
            asyncWorker.shutdown();
        }

        if (bossBarManager != null) {
            bossBarManager.hideAll();
        }

        if (arenaManager != null) {
            arenaManager.stopTicking();
            arenaManager.clear();
        }

        if (spatialGridManager != null) {
            spatialGridManager.clear();
        }

        getLogger().info("Outposts safely disabled.");
    }

    private void startDisplayLoop() {
        long period = Math.max(1L, configManager.getTelemetryTickFrequency());
        displayTask = scheduler.runGlobalTimer(period, period, handle -> {
            // Offload schedule ticks and actionbars completely to single async worker
            asyncWorker.execute(() -> {
                if (scheduleManager != null) {
                    try {
                        scheduleManager.tick();
                    } catch (Exception e) {
                        getLogger().warning("[Schedules] Error during schedule tick: " + e.getMessage());
                    }
                }

                for (OutpostArena arena : arenaManager.getArenas()) {
                    if (!arena.isActive()) {
                        actionBarManager.clearArena(arena.getId());
                        continue;
                    }
                    try {
                        actionBarManager.renderTelemetry(arena);
                    } catch (Exception ignored) {
                    }
                }
            });

            for (OutpostArena arena : arenaManager.getArenas()) {
                if (!arena.isActive()) {
                    bossBarManager.removeArenaBar(arena.getId());
                    continue;
                }
                try {
                    Location center = arena.getCenterLocation();
                    if (center != null && center.getWorld() != null) {
                        if (FoliaCompatScheduler.isFolia()) {
                            scheduler.runAtLocation(center, () -> {
                                Set<UUID> nearby = bossBarManager.findNearbyPlayerUuids(arena);
                                asyncWorker.execute(() -> bossBarManager.updateArenaBar(arena, nearby));
                                perimeterRenderer.renderPerimeter(arena);
                            });
                        } else {
                            Set<UUID> nearby = bossBarManager.findNearbyPlayerUuids(arena);
                            asyncWorker.execute(() -> bossBarManager.updateArenaBar(arena, nearby));
                            perimeterRenderer.renderPerimeter(arena);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            wandListener.tickVisuals();
        });
    }

    private void extractDefaultOutposts() {
        File folder = new File(getDataFolder(), "outposts");
        if (folder.exists()) {
            return;
        }
        folder.mkdirs();
        for (String fileName : List.of("south.yml", "default.yml")) {
            File targetFile = new File(folder, fileName);
            if (!targetFile.exists()) {
                try (InputStream in = getResource("outposts/" + fileName)) {
                    if (in != null) {
                        Files.copy(in, targetFile.toPath());
                        getLogger().info("Extracted default demo outpost: outposts/" + fileName);
                    }
                } catch (Exception e) {
                    getLogger().warning("Could not extract default " + fileName + ": " + e.getMessage());
                }
            }
        }
    }

    public void loadAllArenas() {
        File folder = new File(getDataFolder(), "outposts");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                OutpostArena arena = arenaSerializer.loadFromFile(file);
                if (arena != null) {
                    if (arena instanceof DefaultOutpostArena def) {
                        arena.setActive(def.getMechanicsConfig().isAutoStart());
                    } else {
                        arena.setActive(false);
                    }
                    arenaManager.registerArena(arena);
                    getLogger().info("Loaded arena '" + arena.getId() + "' [" + arena.getCaptureModeType() + "]");
                }
            } catch (Exception e) {
                getLogger().severe("Failed to load outpost arena from " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public void reload() {
        configManager.loadConfig();
        langManager.load();
        teamHookManager.detectAndInitialize(configManager.getPreferredTeamProvider());

        // Re-read configuration values for arenas without wiping active progress or controllers
        File folder = new File(getDataFolder(), "outposts");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    OutpostArena loaded = arenaSerializer.loadFromFile(file);
                    if (loaded != null) {
                        OutpostArena existing = arenaManager.getArena(loaded.getId());
                        if (existing != null) {
                            if (loaded instanceof DefaultOutpostArena def) {
                                def.copyRuntimeStateFrom(existing);
                            } else {
                                loaded.setActive(existing.isActive());
                                loaded.setProgress(existing.getProgress());
                                if (existing.getControllerTeamId() != null) {
                                    loaded.setController(existing.getControllerTeamId(), existing.getControllerTeamName(), null);
                                }
                            }
                            spatialGridManager.unregisterArena(existing);
                        } else {
                            if (loaded instanceof DefaultOutpostArena def) {
                                loaded.setActive(def.getMechanicsConfig().isAutoStart());
                            } else {
                                loaded.setActive(false);
                            }
                        }
                        arenaManager.registerArena(loaded);
                    }
                } catch (Exception e) {
                    getLogger().warning("Error reloading arena " + file.getName() + ": " + e.getMessage());
                }
            }
        }
        if (scheduleManager != null) {
            scheduleManager.loadSchedules(configManager.getSchedulesConfig());
        }
    }

    public ArenaManager getArenaManager() { return arenaManager; }
    public SpatialGridManager getSpatialGridManager() { return spatialGridManager; }
    public TeamHookManager getTeamHookManager() { return teamHookManager; }
    public ArenaSerializer getArenaSerializer() { return arenaSerializer; }
    public OutpostWandListener getWandListener() { return wandListener; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public LangManager getLangManager() { return langManager; }
    public ScheduleManager getScheduleManager() { return scheduleManager; }
    public OutpostWarpProtectionListener getWarpProtectionListener() { return warpProtectionListener; }
    public OutpostsAsyncWorker getAsyncWorker() { return asyncWorker; }
}
