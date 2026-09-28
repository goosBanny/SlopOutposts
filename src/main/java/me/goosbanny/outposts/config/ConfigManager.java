package me.goosbanny.outposts.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Manages plugin configuration loading, defaults, and hot-reloading via BoostedYAML.
 */
public class ConfigManager {

    private final Plugin plugin;

    private YamlDocument rootConfig;
    private YamlDocument schedulesConfig;

    private int engineTickFrequency;
    private int telemetryTickFrequency;
    private int boundaryRenderDistance;

    private boolean maxOutpostsEnabled;
    private int maxOutpostsLimit;
    private int maxOutpostsCooldownSeconds;

    private boolean preventChorusFruit;
    private boolean preventElytraFlight;
    private boolean preventBlockBreak;
    private boolean preventBlockPlace;

    private List<String> blockedCommands = Collections.emptyList();
    private String preferredTeamProvider;
    private double shopGuiMultiplierCap;
    private OccupancyMode defaultOccupancyMode;
    private int bossbarRangeBlocks = 0;
    private CaptureModeType defaultCaptureMode = CaptureModeType.STANDARD_HILL;
    private boolean townyUseNations = false;

    public ConfigManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File configFile = new File(dataFolder, "config.yml");
        File schedulesFile = new File(dataFolder, "schedules.yml");

        try {
            this.rootConfig = BoostedYamlFactory.createRootDocument(configFile, plugin.getResource("config.yml"));
            this.schedulesConfig = BoostedYamlFactory.createRootDocument(schedulesFile, plugin.getResource("schedules.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load plugin configurations with BoostedYAML: " + e.getMessage());
            e.printStackTrace();
        }

        if (rootConfig == null) {
            return;
        }

        this.engineTickFrequency = rootConfig.getInt("system.engine_tick_frequency", 20);
        this.telemetryTickFrequency = rootConfig.contains("system.display_update_interval_ticks")
                ? rootConfig.getInt("system.display_update_interval_ticks", 5)
                : rootConfig.getInt("system.telemetry_tick_frequency", 5);

        this.bossbarRangeBlocks = rootConfig.contains("display.bossbar.range_blocks")
                ? rootConfig.getInt("display.bossbar.range_blocks", 0)
                : 0;
        this.boundaryRenderDistance = rootConfig.contains("display.boundary_render_distance")
                ? rootConfig.getInt("display.boundary_render_distance", 48)
                : rootConfig.getInt("spatial.boundary_render_distance", 48);

        this.maxOutpostsEnabled = rootConfig.contains("gameplay.max_outposts_per_team.enabled")
                ? rootConfig.getBoolean("gameplay.max_outposts_per_team.enabled", true)
                : rootConfig.getBoolean("anti_lag.max_outposts_per_team.enabled", true);
        this.maxOutpostsLimit = rootConfig.contains("gameplay.max_outposts_per_team.limit")
                ? rootConfig.getInt("gameplay.max_outposts_per_team.limit", 2)
                : rootConfig.getInt("anti_lag.max_outposts_per_team.limit", 2);
        this.maxOutpostsCooldownSeconds = rootConfig.contains("gameplay.max_outposts_per_team.cooldown_seconds")
                ? rootConfig.getInt("gameplay.max_outposts_per_team.cooldown_seconds", 15)
                : rootConfig.getInt("anti_lag.max_outposts_per_team.cooldown_seconds", 15);

        this.preventChorusFruit = rootConfig.contains("gameplay.combat_restrictions.prevent_chorus_fruit")
                ? rootConfig.getBoolean("gameplay.combat_restrictions.prevent_chorus_fruit", true)
                : rootConfig.getBoolean("anti_lag.combat_restrictions.prevent_chorus_fruit", true);
        this.preventElytraFlight = rootConfig.contains("gameplay.combat_restrictions.prevent_elytra_flight")
                ? rootConfig.getBoolean("gameplay.combat_restrictions.prevent_elytra_flight", true)
                : rootConfig.getBoolean("anti_lag.combat_restrictions.prevent_elytra_flight", true);
        this.preventBlockBreak = rootConfig.contains("gameplay.combat_restrictions.prevent_block_break")
                ? rootConfig.getBoolean("gameplay.combat_restrictions.prevent_block_break", false)
                : rootConfig.getBoolean("anti_lag.combat_restrictions.prevent_block_break", false);
        this.preventBlockPlace = rootConfig.contains("gameplay.combat_restrictions.prevent_block_place")
                ? rootConfig.getBoolean("gameplay.combat_restrictions.prevent_block_place", false)
                : rootConfig.getBoolean("anti_lag.combat_restrictions.prevent_block_place", false);

        this.blockedCommands = rootConfig.contains("gameplay.blocked_commands")
                ? rootConfig.getStringList("gameplay.blocked_commands")
                : (rootConfig.contains("anti_lag.blocked_commands") ? rootConfig.getStringList("anti_lag.blocked_commands") : List.of());

        this.preferredTeamProvider = rootConfig.getString("hooks.team_provider", "AUTO");
        this.townyUseNations = rootConfig.getBoolean("hooks.towny_use_nations", false);
        this.shopGuiMultiplierCap = rootConfig.getDouble("hooks.shopguiplus_multiplier_cap", 2.5);
        String defaultOccStr = rootConfig.getString("defaults.occupancy_mode", "SOLO");
        this.defaultOccupancyMode = OccupancyMode.fromString(defaultOccStr);
        String defaultCapStr = rootConfig.getString("defaults.capture_mode", "STANDARD_HILL").toUpperCase();
        try {
            this.defaultCaptureMode = CaptureModeType.valueOf(defaultCapStr);
        } catch (IllegalArgumentException e) {
            this.defaultCaptureMode = CaptureModeType.STANDARD_HILL;
        }

        // Ensure outposts folder exists
        File outpostsFolder = new File(plugin.getDataFolder(), "outposts");
        if (!outpostsFolder.exists()) {
            outpostsFolder.mkdirs();
        }
    }

    public void reloadAll() {
        loadConfig();
    }

    public int getEngineTickFrequency() { return engineTickFrequency; }
    public int getTelemetryTickFrequency() { return telemetryTickFrequency; }
    public int getDisplayUpdateIntervalTicks() { return telemetryTickFrequency; }
    public int getBossbarRangeBlocks() { return bossbarRangeBlocks; }
    public int getBoundaryRenderDistance() { return boundaryRenderDistance; }
    public boolean isMaxOutpostsEnabled() { return maxOutpostsEnabled; }
    public int getMaxOutpostsLimit() { return maxOutpostsLimit; }
    public int getMaxOutpostsCooldownSeconds() { return maxOutpostsCooldownSeconds; }
    public boolean isPreventChorusFruit() { return preventChorusFruit; }
    public boolean isPreventElytraFlight() { return preventElytraFlight; }
    public boolean isPreventBlockBreak() { return preventBlockBreak; }
    public boolean isPreventBlockPlace() { return preventBlockPlace; }
    public List<String> getBlockedCommands() { return Collections.unmodifiableList(blockedCommands); }
    public String getPreferredTeamProvider() { return preferredTeamProvider; }
    public boolean isTownyUseNations() { return townyUseNations; }
    public double getShopGuiMultiplierCap() { return shopGuiMultiplierCap; }
    public double getShopGuiPlusMultiplierCap() { return shopGuiMultiplierCap; }
    public OccupancyMode getDefaultOccupancyMode() { return defaultOccupancyMode; }
    public CaptureModeType getDefaultCaptureMode() { return defaultCaptureMode; }
    public YamlDocument getRootConfig() { return rootConfig; }
    public YamlDocument getSchedulesConfig() { return schedulesConfig; }
}
