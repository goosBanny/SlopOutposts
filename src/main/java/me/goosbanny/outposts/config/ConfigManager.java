package me.goosbanny.outposts.config;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * Manages plugin configuration loading, defaults, and hot-reloading.
 */
public class ConfigManager {

    private final Plugin plugin;

    private int engineTickFrequency;
    private int telemetryTickFrequency;
    private int proximityChunkRadius;
    private int boundaryRenderDistance;

    private boolean maxOutpostsEnabled;
    private int maxOutpostsLimit;
    private int maxOutpostsCooldownSeconds;

    private boolean preventChorusFruit;
    private boolean preventElytraFlight;
    private boolean preventBlockBreak;
    private boolean preventBlockPlace;

    private List<String> blockedCommands;
    private String preferredTeamProvider;
    private double shopGuiMultiplierCap;
    private OccupancyMode defaultOccupancyMode;
    private YamlConfiguration schedulesConfig;

    public ConfigManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        File schedulesFile = new File(plugin.getDataFolder(), "schedules.yml");
        if (!schedulesFile.exists()) {
            try {
                plugin.saveResource("schedules.yml", false);
            } catch (Exception ignored) {
            }
        }
        this.schedulesConfig = YamlConfiguration.loadConfiguration(schedulesFile);

        this.engineTickFrequency = config.getInt("system.engine_tick_frequency", 20);
        this.telemetryTickFrequency = config.contains("system.display_update_interval_ticks")
                ? config.getInt("system.display_update_interval_ticks", 5)
                : config.getInt("system.telemetry_tick_frequency", 5);

        this.proximityChunkRadius = config.getInt("spatial.proximity_chunk_radius", 3);
        this.boundaryRenderDistance = config.getInt("spatial.boundary_render_distance", 48);

        this.maxOutpostsEnabled = config.contains("gameplay.max_outposts_per_team.enabled")
                ? config.getBoolean("gameplay.max_outposts_per_team.enabled", true)
                : config.getBoolean("anti_lag.max_outposts_per_team.enabled", true);
        this.maxOutpostsLimit = config.contains("gameplay.max_outposts_per_team.limit")
                ? config.getInt("gameplay.max_outposts_per_team.limit", 2)
                : config.getInt("anti_lag.max_outposts_per_team.limit", 2);
        this.maxOutpostsCooldownSeconds = config.contains("gameplay.max_outposts_per_team.cooldown_seconds")
                ? config.getInt("gameplay.max_outposts_per_team.cooldown_seconds", 15)
                : config.getInt("anti_lag.max_outposts_per_team.cooldown_seconds", 15);

        this.preventChorusFruit = config.contains("gameplay.combat_restrictions.prevent_chorus_fruit")
                ? config.getBoolean("gameplay.combat_restrictions.prevent_chorus_fruit", true)
                : config.getBoolean("anti_lag.combat_restrictions.prevent_chorus_fruit", true);
        this.preventElytraFlight = config.contains("gameplay.combat_restrictions.prevent_elytra_flight")
                ? config.getBoolean("gameplay.combat_restrictions.prevent_elytra_flight", true)
                : config.getBoolean("anti_lag.combat_restrictions.prevent_elytra_flight", true);
        this.preventBlockBreak = config.contains("gameplay.combat_restrictions.prevent_block_break")
                ? config.getBoolean("gameplay.combat_restrictions.prevent_block_break", false)
                : config.getBoolean("anti_lag.combat_restrictions.prevent_block_break", false);
        this.preventBlockPlace = config.contains("gameplay.combat_restrictions.prevent_block_place")
                ? config.getBoolean("gameplay.combat_restrictions.prevent_block_place", false)
                : config.getBoolean("anti_lag.combat_restrictions.prevent_block_place", false);

        this.blockedCommands = config.contains("gameplay.blocked_commands")
                ? config.getStringList("gameplay.blocked_commands")
                : (config.contains("anti_lag.blocked_commands") ? config.getStringList("anti_lag.blocked_commands") : List.of());

        this.preferredTeamProvider = config.getString("hooks.team_provider", "AUTO");
        this.townyUseNations = config.getBoolean("hooks.towny_use_nations", false);
        this.shopGuiMultiplierCap = config.getDouble("hooks.shopguiplus_multiplier_cap", 2.5);
        String defaultOccStr = config.getString("defaults.occupancy_mode", "TEAM");
        this.defaultOccupancyMode = OccupancyMode.fromString(defaultOccStr);

        // Ensure outposts and schedules folders exist
        File outpostsFolder = new File(plugin.getDataFolder(), "outposts");
        if (!outpostsFolder.exists()) {
            outpostsFolder.mkdirs();
        }
    }

    private boolean townyUseNations = false;

    public int getEngineTickFrequency() { return engineTickFrequency; }
    public int getTelemetryTickFrequency() { return telemetryTickFrequency; }
    public int getDisplayUpdateIntervalTicks() { return telemetryTickFrequency; }
    public int getProximityChunkRadius() { return proximityChunkRadius; }
    public int getBoundaryRenderDistance() { return boundaryRenderDistance; }
    public boolean isMaxOutpostsEnabled() { return maxOutpostsEnabled; }
    public int getMaxOutpostsLimit() { return maxOutpostsLimit; }
    public int getMaxOutpostsCooldownSeconds() { return maxOutpostsCooldownSeconds; }
    public boolean isPreventChorusFruit() { return preventChorusFruit; }
    public boolean isPreventElytraFlight() { return preventElytraFlight; }
    public boolean isPreventBlockBreak() { return preventBlockBreak; }
    public boolean isPreventBlockPlace() { return preventBlockPlace; }
    public List<String> getBlockedCommands() { return blockedCommands; }
    public String getPreferredTeamProvider() { return preferredTeamProvider; }
    public boolean isTownyUseNations() { return townyUseNations; }
    public double getShopGuiMultiplierCap() { return shopGuiMultiplierCap; }
    public double getShopGuiPlusMultiplierCap() { return shopGuiMultiplierCap; }
    public OccupancyMode getDefaultOccupancyMode() { return defaultOccupancyMode; }
    public YamlConfiguration getSchedulesConfig() { return schedulesConfig; }
}
