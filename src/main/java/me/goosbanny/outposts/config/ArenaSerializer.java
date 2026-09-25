package me.goosbanny.outposts.config;

import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.mechanics.CaptureModeEngine;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.api.mechanics.TugOfWarTeamAssignment;
import me.goosbanny.outposts.api.pipeline.ActionTrigger;
import me.goosbanny.outposts.api.pipeline.ArenaAction;
import me.goosbanny.outposts.api.team.TeamEconomyProvider;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import me.goosbanny.outposts.core.anticheese.AntiCheeseConfig;
import me.goosbanny.outposts.core.anticheese.AntiCheeseValidator;
import me.goosbanny.outposts.core.anticheese.CombatDamagePolicy;
import me.goosbanny.outposts.core.arena.ArenaGeometry;
import me.goosbanny.outposts.core.arena.ArenaMechanicsConfig;
import me.goosbanny.outposts.core.arena.ArenaMultipliers;
import me.goosbanny.outposts.core.arena.ArenaRegion;
import me.goosbanny.outposts.core.arena.DefaultOutpostArena;
import me.goosbanny.outposts.core.arena.DynamicLocationConfig;
import me.goosbanny.outposts.core.mechanics.PassiveDecayEngine;
import me.goosbanny.outposts.core.mechanics.StandardHillEngine;
import me.goosbanny.outposts.core.mechanics.TicketAccumulationEngine;
import me.goosbanny.outposts.core.mechanics.TugOfWarEngine;
import me.goosbanny.outposts.core.pipeline.DefaultActionPipeline;
import me.goosbanny.outposts.core.pipeline.actions.BroadcastAction;
import me.goosbanny.outposts.core.pipeline.actions.ConsoleCommandAction;
import me.goosbanny.outposts.core.pipeline.actions.FactionBankDepositAction;
import me.goosbanny.outposts.core.pipeline.actions.TeamBankDepositAction;
import me.goosbanny.outposts.core.pipeline.actions.TitleAction;
import me.goosbanny.outposts.core.pipeline.actions.SoundAction;
import me.goosbanny.outposts.core.scheduler.FoliaCompatScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Native YAML serializer and deserializer for Outpost arenas.
 */
public class ArenaSerializer {

    private final TeamRosterProvider teamProvider;
    private final TeamEconomyProvider economyProvider;
    private final FoliaCompatScheduler scheduler;
    private final LangManager langManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ArenaSerializer(
            @NotNull TeamRosterProvider teamProvider,
            @NotNull TeamEconomyProvider economyProvider,
            @NotNull FoliaCompatScheduler scheduler,
            @NotNull LangManager langManager
    ) {
        this.teamProvider = teamProvider;
        this.economyProvider = economyProvider;
        this.scheduler = scheduler;
        this.langManager = langManager;
    }

    /**
     * Loads an OutpostArena from a YAML file.
     */
    @Nullable
    public OutpostArena loadFromFile(@NotNull File file) {
        if (!file.exists() || !file.getName().endsWith(".yml")) {
            return null;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = yaml.getString("id", file.getName().replace(".yml", "")).toLowerCase();

        // 1. Meta
        String nameRaw = yaml.getString("meta.name", "<gradient:#FF416C:#8A2387><bold>" + id + "</bold></gradient>");
        Component displayName = miniMessage.deserialize(nameRaw);

        // 2. Geometry
        String worldName = yaml.getString("geometry.world", "world");
        World checkWorld = Bukkit.getWorld(worldName);
        if (checkWorld == null) {
            Bukkit.getLogger().warning("[Outposts] Warning: World '" + worldName + "' specified in outpost '" + id + "' is not loaded or does not exist!");
        }

        int minX = yaml.getInt("geometry.min.x", 0);
        int minY = yaml.getInt("geometry.min.y", 60);
        int minZ = yaml.getInt("geometry.min.z", 0);
        int maxX = yaml.getInt("geometry.max.x", 10);
        int maxY = yaml.getInt("geometry.max.y", 70);
        int maxZ = yaml.getInt("geometry.max.z", 10);

        // Support compact syntax: bounds: [minX, minY, minZ, maxX, maxY, maxZ] or min: [x,y,z] / max: [x,y,z]
        if (yaml.contains("geometry.bounds") && yaml.getList("geometry.bounds") != null) {
            List<?> bList = yaml.getList("geometry.bounds");
            if (bList.size() >= 6) {
                minX = parseIntSafe(bList.get(0), minX);
                minY = parseIntSafe(bList.get(1), minY);
                minZ = parseIntSafe(bList.get(2), minZ);
                maxX = parseIntSafe(bList.get(3), maxX);
                maxY = parseIntSafe(bList.get(4), maxY);
                maxZ = parseIntSafe(bList.get(5), maxZ);
            }
        } else {
            if (yaml.contains("geometry.min") && yaml.isList("geometry.min")) {
                List<?> mList = yaml.getList("geometry.min");
                if (mList != null && mList.size() >= 3) {
                    minX = parseIntSafe(mList.get(0), minX);
                    minY = parseIntSafe(mList.get(1), minY);
                    minZ = parseIntSafe(mList.get(2), minZ);
                }
            }
            if (yaml.contains("geometry.max") && yaml.isList("geometry.max")) {
                List<?> mList = yaml.getList("geometry.max");
                if (mList != null && mList.size() >= 3) {
                    maxX = parseIntSafe(mList.get(0), maxX);
                    maxY = parseIntSafe(mList.get(1), maxY);
                    maxZ = parseIntSafe(mList.get(2), maxZ);
                }
            }
        }

        // Auto-correct inverted coordinate bounds
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            Bukkit.getLogger().warning("[Outposts] Warning: Outpost '" + id + "' has inverted coordinate bounds. Auto-correcting.");
            int aMinX = Math.min(minX, maxX);
            int aMaxX = Math.max(minX, maxX);
            int aMinY = Math.min(minY, maxY);
            int aMaxY = Math.max(minY, maxY);
            int aMinZ = Math.min(minZ, maxZ);
            int aMaxZ = Math.max(minZ, maxZ);
            minX = aMinX; maxX = aMaxX;
            minY = aMinY; maxY = aMaxY;
            minZ = aMinZ; maxZ = aMaxZ;
        }

        boolean boundingParticles = yaml.getBoolean("geometry.bounding_particles", false);

        Location warpLoc = null;
        if (yaml.contains("geometry.warp")) {
            World w = Bukkit.getWorld(worldName);
            if (yaml.isList("geometry.warp")) {
                List<?> wList = yaml.getList("geometry.warp");
                if (wList != null && wList.size() >= 3) {
                    double wx = parseDoubleSafe(wList.get(0), 0.0);
                    double wy = parseDoubleSafe(wList.get(1), 60.0);
                    double wz = parseDoubleSafe(wList.get(2), 0.0);
                    float yaw = wList.size() >= 4 ? parseFloatSafe(wList.get(3), 0.0f) : 0.0f;
                    float pitch = wList.size() >= 5 ? parseFloatSafe(wList.get(4), 0.0f) : 0.0f;
                    warpLoc = new Location(w, wx, wy, wz, yaw, pitch);
                }
            } else if (yaml.contains("geometry.warp.x")) {
                double wx = yaml.getDouble("geometry.warp.x");
                double wy = yaml.getDouble("geometry.warp.y");
                double wz = yaml.getDouble("geometry.warp.z");
                float yaw = (float) yaml.getDouble("geometry.warp.yaw", 0.0);
                float pitch = (float) yaml.getDouble("geometry.warp.pitch", 0.0);
                warpLoc = new Location(w, wx, wy, wz, yaw, pitch);
            }
        }

        ArenaGeometry geometry = new ArenaGeometry(worldName, minX, minY, minZ, maxX, maxY, maxZ, warpLoc);

        // 3. Mechanics
        boolean enabled = yaml.getBoolean("mechanics.enabled", true);
        boolean autoStart = yaml.getBoolean("mechanics.auto_start", false);
        String occModeStr = yaml.getString("mechanics.occupancy_mode", yaml.getString("mechanics.team_mode", yaml.getString("occupancy_mode", "TEAM")));
        OccupancyMode occupancyMode = OccupancyMode.fromString(occModeStr);
        String modeStr = yaml.getString("mechanics.mode", "STANDARD_HILL").toUpperCase();
        CaptureModeType mode;
        try {
            mode = CaptureModeType.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            mode = CaptureModeType.STANDARD_HILL;
        }

        double percentPerSecond = yaml.contains("mechanics.speed.percent_per_second")
                ? yaml.getDouble("mechanics.speed.percent_per_second")
                : yaml.getDouble("mechanics.speed.percent_per_tick", 2.5);
        if (percentPerSecond <= 0.0) {
            Bukkit.getLogger().warning("[Outposts] Warning: percent_per_second in outpost '" + id + "' must be positive (" + percentPerSecond + " given). Falling back to 2.5%/s.");
            percentPerSecond = 2.5;
        }

        double uncapturePercentPerSecond = yaml.contains("mechanics.speed.uncapture_percent_per_second")
                ? yaml.getDouble("mechanics.speed.uncapture_percent_per_second")
                : percentPerSecond;
        if (uncapturePercentPerSecond <= 0.0) {
            uncapturePercentPerSecond = percentPerSecond;
        }
        double scalingPerMember = Math.max(0.0, yaml.getDouble("mechanics.speed.scaling_per_member", 0.5));
        int maxCappers = Math.max(1, yaml.getInt("mechanics.speed.max_cappers_counted", 4));

        boolean freezeContested = yaml.getBoolean("mechanics.behavior.freeze_when_contested", true);
        double loseThreshold = yaml.getDouble("mechanics.behavior.lose_control_threshold", 100.0);
        long lockoutSeconds = Math.max(0L, yaml.getLong("mechanics.behavior.lockout_seconds", 10));
        long knockDelaySeconds = Math.max(0L, yaml.getLong("mechanics.behavior.knock_delay_seconds", 5));
        boolean passiveDecayEnabled = yaml.getBoolean("mechanics.behavior.passive_decay.enabled", true);
        double passiveDecayRate = yaml.contains("mechanics.behavior.passive_decay.rate_per_second")
                ? yaml.getDouble("mechanics.behavior.passive_decay.rate_per_second")
                : yaml.getDouble("mechanics.behavior.passive_decay.rate_per_tick", 2.5);
        if (passiveDecayRate <= 0.0) {
            passiveDecayRate = 2.5;
        }
        double hysteresisBuffer = yaml.getDouble("mechanics.behavior.hysteresis_buffer_percent", 2.0);

        double stateChangeCooldownSeconds = yaml.contains("mechanics.behavior.state_change_cooldown_seconds")
                ? yaml.getDouble("mechanics.behavior.state_change_cooldown_seconds")
                : (yaml.contains("mechanics.behavior.state_change_cooldown_ticks")
                        ? yaml.getInt("mechanics.behavior.state_change_cooldown_ticks") / 20.0
                        : 1.0);

        int minCappersRequired = yaml.getInt("mechanics.mode_settings.standard_hill.min_cappers_required", yaml.getInt("mechanics.min_cappers_required", 1));

        double neutralAnchorPercent = yaml.getDouble("mechanics.mode_settings.tug_of_war.neutral_anchor_percent", 50.0);
        double contestedAdvantageScaling = yaml.getDouble("mechanics.mode_settings.tug_of_war.contested_advantage_scaling", 0.5);
        double neutralDriftRate = yaml.getDouble("mechanics.mode_settings.tug_of_war.neutral_drift_rate", passiveDecayRate);
        String tugTeamAssignmentStr = yaml.getString("mechanics.mode_settings.tug_of_war.team_assignment", "FIRST_TWO_FACTIONS").toUpperCase();
        TugOfWarTeamAssignment tugOfWarTeamAssignment;
        try {
            tugOfWarTeamAssignment = TugOfWarTeamAssignment.valueOf(tugTeamAssignmentStr);
        } catch (IllegalArgumentException e) {
            tugOfWarTeamAssignment = TugOfWarTeamAssignment.FIRST_TWO_FACTIONS;
        }
        double deadzoneBufferPercent = yaml.getDouble("mechanics.mode_settings.tug_of_war.deadzone_buffer_percent", 2.5);

        int targetTickets = yaml.getInt("mechanics.mode_settings.ticket_accumulation.target_tickets", 1000);
        double ticketsPerSecond = yaml.getDouble("mechanics.mode_settings.ticket_accumulation.tickets_per_second", 10.0);

        ArenaMechanicsConfig mechanicsConfig = new ArenaMechanicsConfig(
                enabled, autoStart, mode, percentPerSecond, uncapturePercentPerSecond, scalingPerMember, maxCappers,
                freezeContested, loseThreshold, lockoutSeconds, knockDelaySeconds,
                passiveDecayEnabled, passiveDecayRate, hysteresisBuffer, stateChangeCooldownSeconds,
                minCappersRequired, neutralAnchorPercent, contestedAdvantageScaling, neutralDriftRate,
                targetTickets, ticketsPerSecond,
                tugOfWarTeamAssignment, deadzoneBufferPercent
        );

        // 4. Anti-Cheese
        boolean los = yaml.getBoolean("mechanics.anti_cheese.line_of_sight", true);
        String combatPolicyStr = yaml.getString("mechanics.anti_cheese.combat_damage_policy", "PAUSE_CAPTURE").toUpperCase();
        CombatDamagePolicy combatPolicy;
        try {
            combatPolicy = CombatDamagePolicy.valueOf(combatPolicyStr);
        } catch (IllegalArgumentException e) {
            combatPolicy = CombatDamagePolicy.PAUSE_CAPTURE;
        }
        int combatPauseSecs = yaml.getInt("mechanics.anti_cheese.combat_pause_seconds", 5);
        boolean noGodmode = yaml.getBoolean("mechanics.anti_cheese.disallow_godmode", true);
        boolean noFlying = yaml.getBoolean("mechanics.anti_cheese.disallow_flying", true);
        boolean noElytra = yaml.getBoolean("mechanics.anti_cheese.disallow_elytra", true);
        boolean noVanish = yaml.getBoolean("mechanics.anti_cheese.disallow_vanished", true);
        boolean noAlliedStall = yaml.getBoolean("mechanics.anti_cheese.disallow_allied_stall", true);

        AntiCheeseConfig antiCheeseConfig = new AntiCheeseConfig(
                los, combatPolicy, combatPauseSecs, noGodmode, noFlying, noElytra, noVanish, noAlliedStall
        );
        AntiCheeseValidator antiCheeseValidator = new AntiCheeseValidator(antiCheeseConfig);

        // 5. Multipliers
        Map<String, Double> multMap = new HashMap<>();
        ConfigurationSection multSec = yaml.getConfigurationSection("multipliers");
        if (multSec != null) {
            for (String key : multSec.getKeys(false)) {
                multMap.put(key.toLowerCase(), multSec.getDouble(key));
            }
        } else {
            multMap.put("mob_drop_rate", 1.75);
            multMap.put("exp_drop_rate", 2.0);
            multMap.put("damage_rate", 1.10);
            multMap.put("shopgui_sell_rate", 1.35);
        }
        ArenaMultipliers multipliers = new ArenaMultipliers(multMap);

        // 6. Action Pipeline
        DefaultActionPipeline pipeline = new DefaultActionPipeline();
        loadActions(yaml, "actions.on_capture", ActionTrigger.ON_CAPTURE, pipeline);
        loadActions(yaml, "actions.on_lost", ActionTrigger.ON_LOST, pipeline);
        loadActions(yaml, "actions.on_contest", ActionTrigger.ON_CONTEST, pipeline);

        long rewardInterval = 30;
        if (yaml.contains("actions.on_tick_reward.interval_seconds")) {
            rewardInterval = yaml.getLong("actions.on_tick_reward.interval_seconds", 30);
            loadActions(yaml, "actions.on_tick_reward.actions", ActionTrigger.ON_TICK_REWARD, pipeline);
        } else if (yaml.contains("actions.on_tick_reward")) {
            loadActions(yaml, "actions.on_tick_reward", ActionTrigger.ON_TICK_REWARD, pipeline);
        }

        // 7. Dynamic Shifting Outposts Configuration
        DynamicLocationConfig dynamicConfig = DynamicLocationConfig.createDisabled();
        if (yaml.contains("dynamic_locations.enabled") && yaml.getBoolean("dynamic_locations.enabled")) {
            String modeName = yaml.getString("dynamic_locations.switch_mode", "INTERVAL").toUpperCase();
            DynamicLocationConfig.SwitchMode switchMode;
            try {
                switchMode = DynamicLocationConfig.SwitchMode.valueOf(modeName);
            } catch (Exception e) {
                switchMode = DynamicLocationConfig.SwitchMode.INTERVAL;
            }

            long interval = yaml.getLong("dynamic_locations.switch_interval_seconds", 300);
            long minInterval = yaml.getLong("dynamic_locations.min_interval_seconds", 180);
            long maxInterval = yaml.getLong("dynamic_locations.max_interval_seconds", 420);
            List<Long> timestamps = yaml.getLongList("dynamic_locations.scheduled_timestamps");
            boolean switchOnCap = yaml.getBoolean("dynamic_locations.switch_on_capture", false);
            List<Integer> warnings = yaml.getIntegerList("dynamic_locations.warning_seconds");
            if (warnings.isEmpty()) warnings = List.of(60, 30, 10, 5, 3, 2, 1);
            int grace = yaml.getInt("dynamic_locations.activation_grace_seconds", 5);
            int invincibility = yaml.getInt("dynamic_locations.warp_invincibility_seconds", 5);

            String transName = yaml.getString("dynamic_locations.state_transition", "KEEP_PROGRESS").toUpperCase();
            DynamicLocationConfig.StateTransition stateTrans;
            try {
                stateTrans = DynamicLocationConfig.StateTransition.valueOf(transName);
            } catch (Exception e) {
                stateTrans = DynamicLocationConfig.StateTransition.KEEP_PROGRESS;
            }

            String playerTransName = yaml.getString("dynamic_locations.player_transition", "NONE").toUpperCase();
            DynamicLocationConfig.PlayerTransition playerTrans;
            try {
                playerTrans = DynamicLocationConfig.PlayerTransition.valueOf(playerTransName);
            } catch (Exception e) {
                playerTrans = DynamicLocationConfig.PlayerTransition.NONE;
            }

            boolean noRepeat = yaml.getBoolean("dynamic_locations.no_immediate_repeat", true);
            boolean fx = yaml.getBoolean("dynamic_locations.effects.enabled", true);
            String sound = yaml.getString("dynamic_locations.effects.sound", "ENTITY_ENDERMAN_TELEPORT");
            boolean beacon = yaml.getBoolean("dynamic_locations.effects.beacon_beam", true);

            List<ArenaRegion> regionList = new ArrayList<>();
            List<Map<?, ?>> regMaps = yaml.getMapList("dynamic_locations.regions");
            for (Map<?, ?> rm : regMaps) {
                String rId = String.valueOf(rm.get("id"));
                String rName = rm.containsKey("name") ? String.valueOf(rm.get("name")) : null;
                int rWeight = Math.max(0, parseIntSafe(rm.get("weight"), 50));
                String rWorld = rm.containsKey("world") ? String.valueOf(rm.get("world")) : worldName;
                World checkRWorld = Bukkit.getWorld(rWorld);
                if (checkRWorld == null) {
                    Bukkit.getLogger().warning("[Outposts] Warning: World '" + rWorld + "' in dynamic region '" + rId + "' of outpost '" + id + "' is not loaded!");
                }

                int rx1 = 0, ry1 = 64, rz1 = 0, rx2 = 10, ry2 = 74, rz2 = 10;
                if (rm.containsKey("bounds") && rm.get("bounds") instanceof List<?> bList && bList.size() >= 6) {
                    rx1 = parseIntSafe(bList.get(0), 0);
                    ry1 = parseIntSafe(bList.get(1), 64);
                    rz1 = parseIntSafe(bList.get(2), 0);
                    rx2 = parseIntSafe(bList.get(3), 10);
                    ry2 = parseIntSafe(bList.get(4), 74);
                    rz2 = parseIntSafe(bList.get(5), 10);
                } else {
                    if (rm.get("min") instanceof List<?> mList && mList.size() >= 3) {
                        rx1 = parseIntSafe(mList.get(0), 0);
                        ry1 = parseIntSafe(mList.get(1), 64);
                        rz1 = parseIntSafe(mList.get(2), 0);
                    } else if (rm.get("min") instanceof Map<?, ?> minMap) {
                        rx1 = parseIntSafe(minMap.get("x"), 0);
                        ry1 = parseIntSafe(minMap.get("y"), 64);
                        rz1 = parseIntSafe(minMap.get("z"), 0);
                    }
                    if (rm.get("max") instanceof List<?> mList && mList.size() >= 3) {
                        rx2 = parseIntSafe(mList.get(0), 10);
                        ry2 = parseIntSafe(mList.get(1), 74);
                        rz2 = parseIntSafe(mList.get(2), 10);
                    } else if (rm.get("max") instanceof Map<?, ?> maxMap) {
                        rx2 = parseIntSafe(maxMap.get("x"), 10);
                        ry2 = parseIntSafe(maxMap.get("y"), 74);
                        rz2 = parseIntSafe(maxMap.get("z"), 10);
                    }
                }

                if (rx1 > rx2 || ry1 > ry2 || rz1 > rz2) {
                    int aMinX = Math.min(rx1, rx2);
                    int aMaxX = Math.max(rx1, rx2);
                    int aMinY = Math.min(ry1, ry2);
                    int aMaxY = Math.max(ry1, ry2);
                    int aMinZ = Math.min(rz1, rz2);
                    int aMaxZ = Math.max(rz1, rz2);
                    rx1 = aMinX; rx2 = aMaxX;
                    ry1 = aMinY; ry2 = aMaxY;
                    rz1 = aMinZ; rz2 = aMaxZ;
                }

                Location rWarp = null;
                if (rm.containsKey("warp")) {
                    World rw = Bukkit.getWorld(rWorld);
                    if (rm.get("warp") instanceof List<?> wList && wList.size() >= 3) {
                        double rwx = parseDoubleSafe(wList.get(0), 0.0);
                        double rwy = parseDoubleSafe(wList.get(1), 64.0);
                        double rwz = parseDoubleSafe(wList.get(2), 0.0);
                        float ryaw = wList.size() >= 4 ? parseFloatSafe(wList.get(3), 0.0f) : 0.0f;
                        float rpitch = wList.size() >= 5 ? parseFloatSafe(wList.get(4), 0.0f) : 0.0f;
                        rWarp = new Location(rw, rwx, rwy, rwz, ryaw, rpitch);
                    } else if (rm.get("warp") instanceof Map<?, ?> wMap) {
                        double rwx = parseDoubleSafe(wMap.get("x"), 0.0);
                        double rwy = parseDoubleSafe(wMap.get("y"), 64.0);
                        double rwz = parseDoubleSafe(wMap.get("z"), 0.0);
                        float ryaw = parseFloatSafe(wMap.get("yaw"), 0.0f);
                        float rpitch = parseFloatSafe(wMap.get("pitch"), 0.0f);
                        rWarp = new Location(rw, rwx, rwy, rwz, ryaw, rpitch);
                    }
                }

                ArenaGeometry rGeom = new ArenaGeometry(rWorld, rx1, ry1, rz1, rx2, ry2, rz2, rWarp);
                regionList.add(new ArenaRegion(rId, rName, rWeight, rGeom));
            }

            dynamicConfig = new DynamicLocationConfig(
                    true, switchMode, interval, minInterval, maxInterval, timestamps, switchOnCap,
                    warnings, grace, invincibility, stateTrans, playerTrans, noRepeat, fx, sound, beacon, regionList
            );
        }

        // 8. Per-Outpost Language Overrides
        Map<String, String> langOverrides = new HashMap<>();
        if (yaml.contains("lang") && yaml.isConfigurationSection("lang")) {
            ConfigurationSection langSec = yaml.getConfigurationSection("lang");
            if (langSec != null) {
                for (String key : langSec.getKeys(true)) {
                    if (!langSec.isConfigurationSection(key)) {
                        langOverrides.put(key, langSec.getString(key));
                    }
                }
            }
        }

        // 9. Capture Mode Engine
        CaptureModeEngine captureEngine = switch (mode) {
            case TUG_OF_WAR -> new TugOfWarEngine(teamProvider, mechanicsConfig);
            case TICKET_ACCUMULATION -> new TicketAccumulationEngine(teamProvider, mechanicsConfig);
            case PASSIVE_DECAY -> new PassiveDecayEngine(teamProvider, mechanicsConfig);
            default -> new StandardHillEngine(teamProvider, mechanicsConfig);
        };

        DefaultOutpostArena arena = new DefaultOutpostArena(
                id, displayName, geometry, mechanicsConfig, multipliers,
                teamProvider, antiCheeseValidator, pipeline, captureEngine, langManager, occupancyMode, rewardInterval
        );
        arena.setBoundingParticlesEnabled(boundingParticles);
        arena.setDynamicLocationConfig(dynamicConfig);
        arena.setCustomLangOverrides(langOverrides);

        // Per-outpost overrides
        if (yaml.contains("display.bossbar_range_blocks")) {
            arena.setCustomBossbarRange(yaml.getInt("display.bossbar_range_blocks"));
        } else if (yaml.contains("display.bossbar.range_blocks")) {
            arena.setCustomBossbarRange(yaml.getInt("display.bossbar.range_blocks"));
        } else if (yaml.contains("bossbar_range_blocks")) {
            arena.setCustomBossbarRange(yaml.getInt("bossbar_range_blocks"));
        }

        if (yaml.contains("geometry.boundary_render_distance")) {
            arena.setCustomBoundaryRenderDistance(yaml.getInt("geometry.boundary_render_distance"));
        } else if (yaml.contains("display.boundary_render_distance")) {
            arena.setCustomBoundaryRenderDistance(yaml.getInt("display.boundary_render_distance"));
        }

        if (yaml.contains("combat_restrictions.prevent_chorus_fruit")) {
            arena.setCustomPreventChorusFruit(yaml.getBoolean("combat_restrictions.prevent_chorus_fruit"));
        } else if (yaml.contains("protections.prevent_chorus_fruit")) {
            arena.setCustomPreventChorusFruit(yaml.getBoolean("protections.prevent_chorus_fruit"));
        }

        if (yaml.contains("combat_restrictions.prevent_elytra_flight")) {
            arena.setCustomPreventElytraFlight(yaml.getBoolean("combat_restrictions.prevent_elytra_flight"));
        } else if (yaml.contains("protections.prevent_elytra_flight")) {
            arena.setCustomPreventElytraFlight(yaml.getBoolean("protections.prevent_elytra_flight"));
        }

        if (yaml.contains("combat_restrictions.prevent_block_break")) {
            arena.setCustomPreventBlockBreak(yaml.getBoolean("combat_restrictions.prevent_block_break"));
        } else if (yaml.contains("protections.prevent_block_break")) {
            arena.setCustomPreventBlockBreak(yaml.getBoolean("protections.prevent_block_break"));
        }

        if (yaml.contains("combat_restrictions.prevent_block_place")) {
            arena.setCustomPreventBlockPlace(yaml.getBoolean("combat_restrictions.prevent_block_place"));
        } else if (yaml.contains("protections.prevent_block_place")) {
            arena.setCustomPreventBlockPlace(yaml.getBoolean("protections.prevent_block_place"));
        }

        if (yaml.contains("hooks.team_provider")) {
            arena.setCustomTeamProvider(yaml.getString("hooks.team_provider"));
        } else if (yaml.contains("team_provider")) {
            arena.setCustomTeamProvider(yaml.getString("team_provider"));
        }

        return arena;
    }

    private void loadActions(YamlConfiguration yaml, String path, ActionTrigger trigger, DefaultActionPipeline pipeline) {
        if (!yaml.contains(path)) return;

        List<Map<?, ?>> actionList = yaml.getMapList(path);
        for (Map<?, ?> map : actionList) {
            String type = String.valueOf(map.get("type")).toUpperCase();
            switch (type) {
                case "BROADCAST" -> {
                    String msg = String.valueOf(map.get("message"));
                    pipeline.addAction(trigger, new BroadcastAction(msg));
                }
                case "COMMAND_CONSOLE", "COMMAND_CONSOLE_PER_PLAYER", "COMMAND_CONSOLE_PER_TEAM" -> {
                    String cmd = String.valueOf(map.get("command"));
                    String target;
                    if ("COMMAND_CONSOLE_PER_PLAYER".equals(type)) {
                        target = "PER_PLAYER";
                    } else if ("COMMAND_CONSOLE_PER_TEAM".equals(type)) {
                        target = "PER_TEAM";
                    } else {
                        target = map.containsKey("target") ? String.valueOf(map.get("target")) : null;
                    }
                    pipeline.addAction(trigger, new ConsoleCommandAction(cmd, target, teamProvider, scheduler));
                }
                case "TEAM_BANK_DEPOSIT", "FACTION_BANK_DEPOSIT" -> {
                    double amt = parseDoubleSafe(map.get("amount"), 0.0);
                    pipeline.addAction(trigger, new TeamBankDepositAction(amt, economyProvider));
                }
                case "SOUND" -> {
                    String key = String.valueOf(map.get("sound"));
                    float vol = parseFloatSafe(map.get("volume"), 1.0f);
                    float pitch = parseFloatSafe(map.get("pitch"), 1.0f);
                    pipeline.addAction(trigger, new SoundAction(key, vol, pitch));
                }
                case "TITLE" -> {
                    String title = map.containsKey("title") ? String.valueOf(map.get("title")) : "";
                    String subtitle = map.containsKey("subtitle") ? String.valueOf(map.get("subtitle")) : "";
                    String target = map.containsKey("target") ? String.valueOf(map.get("target")) : null;
                    pipeline.addAction(trigger, new TitleAction(title, subtitle, target, teamProvider, scheduler));
                }
                default -> Bukkit.getLogger().warning(
                    "[Outposts] Unknown action type '" + type + "' in path '" + path + "'. Check your arena YAML."
                );
            }
        }
    }

    /**
     * Saves a newly created outpost arena to its respective YAML file.
     */
    public void saveToFile(@NotNull OutpostArena arena, @NotNull File file) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", arena.getId());
        yaml.set("meta.name", MiniMessage.miniMessage().serialize(arena.getDisplayName()));

        yaml.set("geometry.world", arena.getWorldName());
        yaml.set("geometry.min.x", arena.getMinX());
        yaml.set("geometry.min.y", arena.getMinY());
        yaml.set("geometry.min.z", arena.getMinZ());
        yaml.set("geometry.max.x", arena.getMaxX());
        yaml.set("geometry.max.y", arena.getMaxY());
        yaml.set("geometry.max.z", arena.getMaxZ());
        yaml.set("geometry.bounding_particles", arena.isBoundingParticlesEnabled());

        Location warp = arena.getWarpLocation();
        if (warp == null) {
            warp = arena.getCenterLocation();
        }
        if (warp != null) {
            yaml.set("geometry.warp.x", warp.getX());
            yaml.set("geometry.warp.y", warp.getY());
            yaml.set("geometry.warp.z", warp.getZ());
            yaml.set("geometry.warp.yaw", warp.getYaw());
            yaml.set("geometry.warp.pitch", warp.getPitch());
        }

        yaml.set("mechanics.enabled", true);
        yaml.set("mechanics.occupancy_mode", arena.getOccupancyMode().name());
        yaml.set("mechanics.mode", arena.getCaptureModeType().name());
        yaml.set("mechanics.speed.percent_per_second", 2.5);
        yaml.set("mechanics.speed.uncapture_percent_per_second", 2.5);
        yaml.set("mechanics.speed.scaling_per_member", 0.5);
        yaml.set("mechanics.speed.max_cappers_counted", 4);

        yaml.set("mechanics.behavior.freeze_when_contested", true);
        yaml.set("mechanics.behavior.lose_control_threshold", 100.0);
        yaml.set("mechanics.behavior.lockout_seconds", 10);
        yaml.set("mechanics.behavior.knock_delay_seconds", 5);
        yaml.set("mechanics.behavior.passive_decay.enabled", true);
        yaml.set("mechanics.behavior.passive_decay.rate_per_second", 2.5);
        yaml.set("mechanics.behavior.hysteresis_buffer_percent", 2.0);
        yaml.set("mechanics.behavior.state_change_cooldown_seconds", 1.0);

        yaml.set("mechanics.anti_cheese.line_of_sight", true);
        yaml.set("mechanics.anti_cheese.combat_damage_policy", "PAUSE_CAPTURE");
        yaml.set("mechanics.anti_cheese.combat_pause_seconds", 5);
        yaml.set("mechanics.anti_cheese.disallow_godmode", true);
        yaml.set("mechanics.anti_cheese.disallow_flying", true);
        yaml.set("mechanics.anti_cheese.disallow_elytra", true);
        yaml.set("mechanics.anti_cheese.disallow_vanished", true);
        yaml.set("mechanics.anti_cheese.disallow_allied_stall", true);

        if (arena instanceof DefaultOutpostArena def) {
            yaml.set("mechanics.mode_settings.standard_hill.min_cappers_required", def.getMechanicsConfig().getMinCappersRequired());
            yaml.set("mechanics.mode_settings.tug_of_war.neutral_anchor_percent", def.getMechanicsConfig().getNeutralAnchorPercent());
            yaml.set("mechanics.mode_settings.tug_of_war.contested_advantage_scaling", def.getMechanicsConfig().getContestedAdvantageScaling());
            yaml.set("mechanics.mode_settings.tug_of_war.neutral_drift_rate", def.getMechanicsConfig().getNeutralDriftRate());
            yaml.set("mechanics.mode_settings.tug_of_war.team_assignment", def.getMechanicsConfig().getTugOfWarTeamAssignment().name());
            yaml.set("mechanics.mode_settings.tug_of_war.deadzone_buffer_percent", def.getMechanicsConfig().getDeadzoneBufferPercent());
            yaml.set("mechanics.mode_settings.ticket_accumulation.target_tickets", def.getMechanicsConfig().getTargetTickets());
            yaml.set("mechanics.mode_settings.ticket_accumulation.tickets_per_second", def.getMechanicsConfig().getTicketsPerSecond());
        }

        yaml.set("multipliers.mob_drop_rate", arena.getMultiplier("mob_drop_rate"));
        yaml.set("multipliers.exp_drop_rate", arena.getMultiplier("exp_drop_rate"));
        yaml.set("multipliers.damage_rate", arena.getMultiplier("damage_rate"));
        yaml.set("multipliers.shopgui_sell_rate", arena.getMultiplier("shopgui_sell_rate"));

        List<Map<String, Object>> captureActions = new ArrayList<>();
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "BROADCAST");
        broadcast.put("message", "<prefix> <gold><team></gold> captured <name>!");
        captureActions.add(broadcast);
        yaml.set("actions.on_capture", captureActions);

        // Dynamic Shifting Outposts serialization
        DynamicLocationConfig dlc = arena.getDynamicLocationConfig();
        if (dlc.isEnabled() || !dlc.getRegions().isEmpty()) {
            yaml.set("dynamic_locations.enabled", dlc.isEnabled());
            yaml.set("dynamic_locations.switch_mode", dlc.getSwitchMode().name());
            yaml.set("dynamic_locations.switch_interval_seconds", dlc.getIntervalSeconds());
            yaml.set("dynamic_locations.min_interval_seconds", dlc.getMinIntervalSeconds());
            yaml.set("dynamic_locations.max_interval_seconds", dlc.getMaxIntervalSeconds());
            yaml.set("dynamic_locations.scheduled_timestamps", dlc.getScheduledTimestamps());
            yaml.set("dynamic_locations.switch_on_capture", dlc.isSwitchOnCapture());
            yaml.set("dynamic_locations.warning_seconds", dlc.getWarningSeconds());
            yaml.set("dynamic_locations.activation_grace_seconds", dlc.getActivationGraceSeconds());
            yaml.set("dynamic_locations.warp_invincibility_seconds", dlc.getWarpInvincibilitySeconds());
            yaml.set("dynamic_locations.state_transition", dlc.getStateTransition().name());
            yaml.set("dynamic_locations.player_transition", dlc.getPlayerTransition().name());
            yaml.set("dynamic_locations.no_immediate_repeat", dlc.isNoImmediateRepeat());
            yaml.set("dynamic_locations.effects.enabled", dlc.isEffectsEnabled());
            yaml.set("dynamic_locations.effects.sound", dlc.getSoundEffect());
            yaml.set("dynamic_locations.effects.beacon_beam", dlc.isBeaconBeam());

            List<Map<String, Object>> regionList = new ArrayList<>();
            for (ArenaRegion r : dlc.getRegions()) {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("id", r.getId());
                rm.put("name", r.getRawName());
                rm.put("weight", r.getWeight());
                rm.put("world", r.getGeometry().getWorldName());

                Map<String, Object> minMap = new LinkedHashMap<>();
                minMap.put("x", r.getGeometry().getMinX());
                minMap.put("y", r.getGeometry().getMinY());
                minMap.put("z", r.getGeometry().getMinZ());
                rm.put("min", minMap);

                Map<String, Object> maxMap = new LinkedHashMap<>();
                maxMap.put("x", r.getGeometry().getMaxX());
                maxMap.put("y", r.getGeometry().getMaxY());
                maxMap.put("z", r.getGeometry().getMaxZ());
                rm.put("max", maxMap);

                Location warpLoc = r.getGeometry().getWarpLocation();
                if (warpLoc != null) {
                    Map<String, Object> warpMap = new LinkedHashMap<>();
                    warpMap.put("x", warpLoc.getX());
                    warpMap.put("y", warpLoc.getY());
                    warpMap.put("z", warpLoc.getZ());
                    warpMap.put("yaw", warpLoc.getYaw());
                    warpMap.put("pitch", warpLoc.getPitch());
                    rm.put("warp", warpMap);
                }
                regionList.add(rm);
            }
            yaml.set("dynamic_locations.regions", regionList);
        }

        // Per-outpost language overrides
        if (!arena.getCustomLangOverrides().isEmpty()) {
            for (Map.Entry<String, String> entry : arena.getCustomLangOverrides().entrySet()) {
                yaml.set("lang." + entry.getKey(), entry.getValue());
            }
        }

        yaml.save(file);
    }

    /**
     * Creates a new arena file from the comprehensive default.yml template.
     * Replaces coordinate bounds, world, id, and display name while preserving every comment,
     * mechanic explanation, multiplier, dynamic region config, and sample language override.
     */
    public void createFromTemplate(
            @NotNull File templateFile,
            @NotNull File targetFile,
            @NotNull String id,
            @NotNull String worldName,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            @NotNull Location warpLoc,
            @NotNull OccupancyMode occupancyMode
    ) throws IOException {
        createFromTemplate(templateFile, targetFile, id, worldName, minX, minY, minZ, maxX, maxY, maxZ, warpLoc, occupancyMode, CaptureModeType.STANDARD_HILL);
    }

    public void createFromTemplate(
            @NotNull File templateFile,
            @NotNull File targetFile,
            @NotNull String id,
            @NotNull String worldName,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            @NotNull Location warpLoc,
            @NotNull OccupancyMode occupancyMode,
            @NotNull CaptureModeType captureMode
    ) throws IOException {
        String templateContent = null;
        if (templateFile.exists()) {
            try {
                templateContent = Files.readString(templateFile.toPath(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }
        if (templateContent == null) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("outposts/default.yml")) {
                if (in != null) {
                    templateContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        if (templateContent != null && !templateContent.isBlank()) {
            String capitalizedId = !id.isEmpty() ? (id.substring(0, 1).toUpperCase() + id.substring(1)) : id;
            double wx = Math.round(warpLoc.getX() * 10.0) / 10.0;
            double wy = Math.round(warpLoc.getY() * 10.0) / 10.0;
            double wz = Math.round(warpLoc.getZ() * 10.0) / 10.0;
            float wyaw = Math.round(warpLoc.getYaw() * 10.0) / 10.0f;
            float wpitch = Math.round(warpLoc.getPitch() * 10.0) / 10.0f;

            // Perform targeted regex replacements to preserve 100% of inline comments and documentation
            String customized = templateContent
                    .replaceAll("(?m)^id:\\s*.*$", "id: " + id)
                    .replaceAll("(?m)^(\\s*name:\\s*).*$", "$1<#F07DB5><bold>" + capitalizedId + " Outpost</bold></#F07DB5>")
                    .replaceAll("(?m)^(\\s*world:\\s*).*$", "$1" + worldName)
                    .replaceAll("(?m)^(\\s*occupancy_mode:\\s*).*$", "$1" + occupancyMode.name())
                    .replaceAll("(?m)^(\\s*mode:\\s*).*$", "$1" + captureMode.name());

            // Replace coordinates under geometry.min, geometry.max, and geometry.warp
            customized = customized.replaceFirst("(?s)(min:\\s*\\n\\s*x:\\s*)[^\\n]+(\\n\\s*y:\\s*)[^\\n]+(\\n\\s*z:\\s*)[^\\n]+",
                    "$1" + minX + "$2" + minY + "$3" + minZ);
            customized = customized.replaceFirst("(?s)(max:\\s*\\n\\s*x:\\s*)[^\\n]+(\\n\\s*y:\\s*)[^\\n]+(\\n\\s*z:\\s*)[^\\n]+",
                    "$1" + maxX + "$2" + maxY + "$3" + maxZ);
            customized = customized.replaceFirst("(?s)(warp:\\s*\\n\\s*x:\\s*)[^\\n]+(\\n\\s*y:\\s*)[^\\n]+(\\n\\s*z:\\s*)[^\\n]+(\\n\\s*yaw:\\s*)[^\\n]+(\\n\\s*pitch:\\s*)[^\\n]+",
                    "$1" + wx + "$2" + wy + "$3" + wz + "$4" + wyaw + "$5" + wpitch);

            if (targetFile.getParentFile() != null) {
                targetFile.getParentFile().mkdirs();
            }
            Files.writeString(targetFile.toPath(), customized, StandardCharsets.UTF_8);
            return;
        }

        // Fallback to YamlConfiguration if template reading failed
        YamlConfiguration yaml = new YamlConfiguration();
        if (templateFile.exists()) {
            try {
                yaml.load(templateFile);
            } catch (Exception e) {
                throw new IOException("Failed to load template file: " + e.getMessage(), e);
            }
        } else {
            // Fallback: read from bundled plugin jar resources
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("outposts/default.yml")) {
                if (in != null) {
                    try {
                        yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        throw new IOException("Failed to parse bundled template resource: " + e.getMessage(), e);
                    }
                }
            }
        }
        yaml.set("id", id);
        String capitalizedId = !id.isEmpty() ? (id.substring(0, 1).toUpperCase() + id.substring(1)) : id;
        yaml.set("meta.name", "<#F07DB5><bold>" + capitalizedId + "</bold></#F07DB5>");
        yaml.set("geometry.world", worldName);
        yaml.set("geometry.min.x", minX);
        yaml.set("geometry.min.y", minY);
        yaml.set("geometry.min.z", minZ);
        yaml.set("geometry.max.x", maxX);
        yaml.set("geometry.max.y", maxY);
        yaml.set("geometry.max.z", maxZ);

        yaml.set("geometry.warp.x", Math.round(warpLoc.getX() * 10.0) / 10.0);
        yaml.set("geometry.warp.y", Math.round(warpLoc.getY() * 10.0) / 10.0);
        yaml.set("geometry.warp.z", Math.round(warpLoc.getZ() * 10.0) / 10.0);
        yaml.set("geometry.warp.yaw", Math.round(warpLoc.getYaw() * 10.0) / 10.0);
        yaml.set("geometry.warp.pitch", Math.round(warpLoc.getPitch() * 10.0) / 10.0);

        yaml.set("mechanics.occupancy_mode", occupancyMode.name());
        yaml.set("mechanics.mode", captureMode.name());

        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }
        yaml.save(targetFile);
    }

    private static int parseIntSafe(Object val, int fallback) {
        if (val == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(val).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double parseDoubleSafe(Object val, double fallback) {
        if (val == null) return fallback;
        try {
            return Double.parseDouble(String.valueOf(val).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static float parseFloatSafe(Object val, float fallback) {
        if (val == null) return fallback;
        try {
            return Float.parseFloat(String.valueOf(val).trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
