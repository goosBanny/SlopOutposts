package me.goosbanny.outposts.command;

import me.goosbanny.outposts.Outposts;
import me.goosbanny.outposts.api.arena.OccupancyMode;
import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.api.event.OutpostTeleportEvent;
import me.goosbanny.outposts.api.mechanics.CaptureModeType;
import me.goosbanny.outposts.config.LangManager;
import me.goosbanny.outposts.core.anticheese.AntiCheeseConfig;
import me.goosbanny.outposts.core.anticheese.AntiCheeseValidator;
import me.goosbanny.outposts.core.arena.ArenaGeometry;
import me.goosbanny.outposts.core.arena.ArenaMechanicsConfig;
import me.goosbanny.outposts.core.arena.ArenaMultipliers;
import me.goosbanny.outposts.core.arena.ArenaRegion;
import me.goosbanny.outposts.core.arena.DefaultOutpostArena;
import me.goosbanny.outposts.core.mechanics.StandardHillEngine;
import me.goosbanny.outposts.core.pipeline.DefaultActionPipeline;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Command dispatcher and tab completer for /outpost subcommands.
 * All messages and feedback strings are resolved dynamically from lang.yml.
 */
public class OutpostCommand implements CommandExecutor, TabCompleter {

    private final Outposts plugin;
    private final SystemDoctor doctor;
    private final LangManager langManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public OutpostCommand(@NotNull Outposts plugin, @NotNull SystemDoctor doctor, @NotNull LangManager langManager) {
        this.plugin = plugin;
        this.doctor = doctor;
        this.langManager = langManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> handleList(sender);
            case "doctor" -> {
                if (!sender.hasPermission("outposts.admin")) {
                    sender.sendMessage(langManager.get("commands.no_permission"));
                    return true;
                }
                doctor.runDiagnostics(sender);
            }
            case "wand" -> handleWand(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setwarp" -> handleSetWarp(sender, args);
            case "tp" -> handleTp(sender, args);
            case "info" -> handleInfo(sender, args);
            case "compass" -> handleCompass(sender, args);
            case "region" -> handleRegion(sender, args);
            case "reload" -> handleReload(sender);
            case "forcestart" -> handleForceStart(sender, args);
            case "forcestop" -> handleForceStop(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(mm.deserialize("<#F07DB5><bold>OUTPOSTS</bold></#F07DB5> <gray>▶</gray> <#FDC05C>Outposts Commands</#FDC05C>"));
        sender.sendMessage(mm.deserialize("<gray>● <#00B8FF>/outpost list</#00B8FF> <dark_gray>-</dark_gray> <#CECECE>View all outposts and capture status</#CECECE>"));
        sender.sendMessage(mm.deserialize("<gray>● <#00B8FF>/outpost info <id></#00B8FF> <dark_gray>-</dark_gray> <#CECECE>View detailed status of an outpost</#CECECE>"));
        sender.sendMessage(mm.deserialize("<gray>● <#00B8FF>/outpost tp <id></#00B8FF> <dark_gray>-</dark_gray> <#CECECE>Teleport to outpost warp</#CECECE>"));
        sender.sendMessage(mm.deserialize("<gray>● <#00B8FF>/outpost compass <id></#00B8FF> <dark_gray>-</dark_gray> <#CECECE>Point compass needle to active outpost pad</#CECECE>"));
        if (sender.hasPermission("outposts.admin")) {
            sender.sendMessage(mm.deserialize("<gray>● <#FDC05C>/outpost wand</#FDC05C> <dark_gray>-</dark_gray> <#CECECE>Get visual setup wand</#CECECE>"));
            sender.sendMessage(mm.deserialize("<gray>● <#FDC05C>/outpost create <id> <TEAM|SOLO> [mode]</#FDC05C> <dark_gray>-</dark_gray> <#CECECE>Create new outpost from wand selection</#CECECE>"));
            sender.sendMessage(mm.deserialize("<gray>● <#FDC05C>/outpost delete <id></#FDC05C> <dark_gray>-</dark_gray> <#CECECE>Delete an outpost arena</#CECECE>"));
            sender.sendMessage(mm.deserialize("<gray>● <#FDC05C>/outpost setwarp <id></#FDC05C> <dark_gray>-</dark_gray> <#CECECE>Set warp position to current location</#CECECE>"));
            sender.sendMessage(mm.deserialize("<gray>● <#FDC05C>/outpost region <add|remove|list|setweight|shift></#FDC05C> <dark_gray>-</dark_gray> <#CECECE>Manage dynamic regions</#CECECE>"));
            sender.sendMessage(mm.deserialize("<gray>● <#FDC05C>/outpost reload</#FDC05C> <dark_gray>-</dark_gray> <#CECECE>Non-destructive reload of configs</#CECECE>"));
            sender.sendMessage(mm.deserialize("<gray>● <#FDC05C>/outpost doctor</#FDC05C> <dark_gray>-</dark_gray> <#CECECE>Audit system & bounding boxes</#CECECE>"));
            sender.sendMessage(mm.deserialize("<gray>● <#FDC05C>/outpost forcestart <id></#FDC05C> <dark_gray>-</dark_gray> <#CECECE>Force unlock arena</#CECECE>"));
            sender.sendMessage(mm.deserialize("<gray>● <#FDC05C>/outpost forcestop <id></#FDC05C> <dark_gray>-</dark_gray> <#CECECE>Force reset arena to neutral</#CECECE>"));
        }
    }

    private void handleList(CommandSender sender) {
        Collection<OutpostArena> arenas = plugin.getArenaManager().getArenas();
        if (arenas.isEmpty()) {
            sender.sendMessage(langManager.get("commands.list_empty"));
            return;
        }

        sender.sendMessage(langManager.get("commands.list_header", Map.of("count", String.valueOf(arenas.size()))));
        for (OutpostArena arena : arenas) {
            String stateStr = langManager.getPlaceholderState(arena.getState().name(), arena.getState().name());
            String team = arena.getControllerTeamName() != null ? arena.getControllerTeamName() : langManager.getPlaceholder("none", "None");

            Map<String, String> tokens = new HashMap<>();
            tokens.put("id", arena.getId());
            tokens.put("state", "[" + stateStr + "]");
            tokens.put("progress", String.format("%.1f", arena.getProgress()));
            tokens.put("team", team);

            sender.sendMessage(langManager.get("commands.list_entry", tokens));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<#E13148>Usage: /outpost info <id></#E13148>"));
            return;
        }
        OutpostArena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", args[1])));
            return;
        }

        sender.sendMessage(mm.deserialize("<#F07DB5><bold>OUTPOSTS</bold></#F07DB5> <gray>▶</gray> <#FDC05C>Outpost Details: <white>" + mm.escapeTags(arena.getId()) + "</white></#FDC05C>"));
        sender.sendMessage(mm.deserialize("<gray>● <#CECECE>Display Name: </#CECECE>").append(arena.getDisplayName()));
        sender.sendMessage(mm.deserialize("<gray>● <#CECECE>World: <white>" + mm.escapeTags(arena.getWorldName()) + "</white></#CECECE>"));
        sender.sendMessage(mm.deserialize("<gray>● <#CECECE>Bounds: <#00B8FF>" + arena.getMinX() + "," + arena.getMinY() + "," + arena.getMinZ()
                + " <dark_gray>to</dark_gray> " + arena.getMaxX() + "," + arena.getMaxY() + "," + arena.getMaxZ() + "</#00B8FF></#CECECE>"));
        sender.sendMessage(mm.deserialize("<gray>● <#CECECE>State: <white>" + mm.escapeTags(langManager.getPlaceholderState(arena.getState().name(), arena.getState().name())) + "</white></#CECECE>"));
        sender.sendMessage(mm.deserialize("<gray>● <#CECECE>Progress: <#FDC05C>" + String.format("%.1f%%", arena.getProgress()) + "</#FDC05C></#CECECE>"));
        String controllerName = arena.getControllerTeamName() != null ? arena.getControllerTeamName() : langManager.getPlaceholder("none", "None");
        sender.sendMessage(mm.deserialize("<gray>● <#CECECE>Controller: <#98fc98>" + mm.escapeTags(controllerName) + "</#98fc98></#CECECE>"));
        sender.sendMessage(mm.deserialize("<gray>● <#CECECE>Cappers Inside: <#FF6C00>" + arena.getCapperCount() + "</#FF6C00></#CECECE>"));
        sender.sendMessage(mm.deserialize("<gray>● <#CECECE>Occupancy Mode: <white>" + arena.getOccupancyMode().name() + "</white></#CECECE>"));
        sender.sendMessage(mm.deserialize("<gray>● <#CECECE>Capture Mode: <white>" + arena.getCaptureModeType().name() + "</white></#CECECE>"));
    }

    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(langManager.get("commands.player_only"));
            return;
        }
        if (!player.hasPermission("outposts.admin")) {
            player.sendMessage(langManager.get("commands.no_permission"));
            return;
        }

        player.getInventory().addItem(plugin.getWandListener().createWand());
        plugin.getWandListener().addWandUser(player.getUniqueId());
        player.sendMessage(langManager.get("commands.wand_given"));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(langManager.get("commands.player_only"));
            return;
        }
        if (!player.hasPermission("outposts.admin")) {
            player.sendMessage(langManager.get("commands.no_permission"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(mm.deserialize("<red>Usage: /outpost create <id> <TEAM|SOLO> [mode]</red>"));
            return;
        }

        String id = args[1].toLowerCase();
        if (plugin.getArenaManager().getArena(id) != null) {
            player.sendMessage(langManager.get("commands.already_exists", Map.of("id", id)));
            return;
        }

        String occStr = args[2].toUpperCase();
        if (!occStr.equals("TEAM") && !occStr.equals("SOLO")) {
            player.sendMessage(mm.deserialize("<red>Invalid occupancy mode '" + args[2] + "'. Must be TEAM or SOLO.</red>"));
            return;
        }
        OccupancyMode occupancyMode = OccupancyMode.valueOf(occStr);

        CaptureModeType captureMode = CaptureModeType.STANDARD_HILL;
        if (args.length >= 4) {
            try {
                captureMode = CaptureModeType.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(mm.deserialize("<red>Invalid capture mode '" + args[3] + "'. Valid modes: STANDARD_HILL, TUG_OF_WAR, TICKET_ACCUMULATION</red>"));
                return;
            }
        }

        OutpostWandListener.Selection sel = plugin.getWandListener().getSelection(player.getUniqueId());
        if (sel == null || !sel.isComplete()) {
            player.sendMessage(langManager.get("commands.wand_incomplete"));
            return;
        }

        Location p1 = sel.getPos1();
        Location p2 = sel.getPos2();
        ArenaGeometry geometry = new ArenaGeometry(
                p1.getWorld().getName(),
                p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(),
                player.getLocation()
        );

        plugin.getWandListener().removeWandUser(player.getUniqueId());
        File file = new File(plugin.getDataFolder(), "outposts/" + id + ".yml");
        File templateFile = new File(plugin.getDataFolder(), "outposts/default.yml");

        try {
            plugin.getArenaSerializer().createFromTemplate(
                    templateFile, file, id,
                    geometry.getWorldName(),
                    geometry.getMinX(), geometry.getMinY(), geometry.getMinZ(),
                    geometry.getMaxX(), geometry.getMaxY(), geometry.getMaxZ(),
                    player.getLocation(),
                    occupancyMode,
                    captureMode
            );

            OutpostArena arena = plugin.getArenaSerializer().loadFromFile(file);
            if (arena != null) {
                arena.setActive(false);
                plugin.getArenaManager().registerArena(arena);
                plugin.getScheduleManager().syncArenaState(arena);
            }

            player.sendMessage(langManager.get("commands.created", Map.of("id", id)));
        } catch (Exception e) {
            player.sendMessage(mm.deserialize("<red>Failed to create outpost from template: " + e.getMessage() + "</red>"));
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("outposts.admin")) {
            sender.sendMessage(langManager.get("commands.no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /outpost delete <id></red>"));
            return;
        }

        String id = args[1].toLowerCase();
        OutpostArena arena = plugin.getArenaManager().unregisterArena(id);
        if (arena == null) {
            sender.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", id)));
            return;
        }

        File file = new File(plugin.getDataFolder(), "outposts/" + id + ".yml");
        if (file.exists()) {
            file.delete();
        }
        sender.sendMessage(langManager.get("commands.deleted", Map.of("id", id)));
    }

    private void handleSetWarp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(langManager.get("commands.player_only"));
            return;
        }
        if (!player.hasPermission("outposts.admin")) {
            player.sendMessage(langManager.get("commands.no_permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize("<red>Usage: /outpost setwarp <id></red>"));
            return;
        }

        OutpostArena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            player.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", args[1])));
            return;
        }

        File file = new File(plugin.getDataFolder(), "outposts/" + arena.getId() + ".yml");
        try {
            arena.setWarpLocation(player.getLocation());
            plugin.getArenaSerializer().saveToFile(arena, file);
            player.sendMessage(langManager.get("commands.warp_set", Map.of("id", arena.getId())));
        } catch (Exception e) {
            player.sendMessage(mm.deserialize("<red>Failed to update warp: " + e.getMessage() + "</red>"));
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(langManager.get("commands.player_only"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize("<red>Usage: /outpost tp <id></red>"));
            return;
        }

        OutpostArena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            player.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", args[1])));
            return;
        }

        Location warp = arena.getWarpLocation();
        if (warp == null) {
            warp = arena.getCenterLocation();
        }
        if (warp != null) {
            OutpostTeleportEvent event =
                    new OutpostTeleportEvent(arena, player, warp);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            player.teleportAsync(event.getTargetLocation());
            player.sendMessage(langManager.get("commands.teleported", Map.of("id", arena.getId())));
        } else {
            player.sendMessage(langManager.get("commands.no_warp", Map.of("id", arena.getId())));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("outposts.admin")) {
            sender.sendMessage(langManager.get("commands.no_permission"));
            return;
        }

        plugin.reload();
        sender.sendMessage(langManager.get("commands.reloaded"));
    }

    private void handleForceStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("outposts.admin")) {
            sender.sendMessage(langManager.get("commands.no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /outpost forcestart <id></red>"));
            return;
        }
        OutpostArena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", args[1])));
            return;
        }
        arena.setActive(true);
        arena.setProgress(0.0);
        if (arena instanceof DefaultOutpostArena def) {
            def.setLockoutRemainingSeconds(0);
        }
        sender.sendMessage(langManager.get("commands.forcestarted", Map.of("id", arena.getId())));
    }

    private void handleForceStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("outposts.admin")) {
            sender.sendMessage(langManager.get("commands.no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /outpost forcestop <id></red>"));
            return;
        }
        OutpostArena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", args[1])));
            return;
        }
        arena.setActive(false);
        arena.resetToNeutral();
        sender.sendMessage(langManager.get("commands.forcestopped", Map.of("id", arena.getId())));
    }

    private void handleCompass(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(langManager.get("commands.player_only"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize("<red>Usage: /outpost compass <id></red>"));
            return;
        }
        OutpostArena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            player.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", args[1])));
            return;
        }
        Location target = arena.getWarpLocation();
        if (target == null) target = arena.getCenterLocation();
        if (target == null) {
            player.sendMessage(langManager.get("commands.no_warp", Map.of("id", arena.getId())));
            return;
        }
        player.setCompassTarget(target);
        String coords = target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ();
        player.sendMessage(langManager.get("commands.compass_pointed", arena, Map.of("id", arena.getId(), "coords", coords)));
    }

    private void handleRegion(CommandSender sender, String[] args) {
        if (!sender.hasPermission("outposts.admin")) {
            sender.sendMessage(langManager.get("commands.no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /outpost region <add|remove|list|setweight|shift> ...</red>"));
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(langManager.get("commands.player_only"));
                    return;
                }
                if (args.length < 4) {
                    player.sendMessage(mm.deserialize("<red>Usage: /outpost region add <arena> <regionId> [weight] [name]</red>"));
                    return;
                }
                String arenaId = args[2].toLowerCase();
                String regionId = args[3].toLowerCase();
                int weight = args.length >= 5 ? Integer.parseInt(args[4]) : 50;
                String name = args.length >= 6 ? String.join(" ", Arrays.copyOfRange(args, 5, args.length)) : null;

                OutpostArena arena = plugin.getArenaManager().getArena(arenaId);
                if (arena == null) {
                    player.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", arenaId)));
                    return;
                }

                OutpostWandListener.Selection sel = plugin.getWandListener().getSelection(player.getUniqueId());
                if (sel == null || !sel.isComplete()) {
                    player.sendMessage(langManager.get("commands.wand_incomplete"));
                    return;
                }

                Location pos1 = sel.getPos1();
                Location pos2 = sel.getPos2();

                ArenaGeometry geom = new ArenaGeometry(
                        pos1.getWorld().getName(),
                        pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                        pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ(),
                        player.getLocation()
                );
                ArenaRegion region =
                        new ArenaRegion(regionId, name, weight, geom);
                arena.getDynamicLocationConfig().setEnabled(true);
                arena.getDynamicLocationConfig().addRegion(region);
                plugin.getWandListener().removeWandUser(player.getUniqueId());

                File file = new File(plugin.getDataFolder(), "outposts/" + arena.getId() + ".yml");
                try {
                    plugin.getArenaSerializer().saveToFile(arena, file);
                    player.sendMessage(langManager.get("commands.region_added", Map.of("region", regionId, "arena", arena.getId())));
                } catch (Exception e) {
                    player.sendMessage(mm.deserialize("<red>Failed to save region: " + e.getMessage() + "</red>"));
                }
            }
            case "remove" -> {
                if (args.length < 4) {
                    sender.sendMessage(mm.deserialize("<red>Usage: /outpost region remove <arena> <regionId></red>"));
                    return;
                }
                String arenaId = args[2].toLowerCase();
                String regionId = args[3].toLowerCase();
                OutpostArena arena = plugin.getArenaManager().getArena(arenaId);
                if (arena == null) {
                    sender.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", arenaId)));
                    return;
                }
                boolean removed = arena.getDynamicLocationConfig().removeRegion(regionId);
                if (!removed) {
                    sender.sendMessage(mm.deserialize("<red>Region '" + regionId + "' not found on outpost '" + arenaId + "'.</red>"));
                    return;
                }
                File file = new File(plugin.getDataFolder(), "outposts/" + arena.getId() + ".yml");
                try {
                    plugin.getArenaSerializer().saveToFile(arena, file);
                    sender.sendMessage(langManager.get("commands.region_removed", Map.of("region", regionId, "arena", arena.getId())));
                } catch (Exception e) {
                    sender.sendMessage(mm.deserialize("<red>Failed to save region: " + e.getMessage() + "</red>"));
                }
            }
            case "list" -> {
                if (args.length < 3) {
                    sender.sendMessage(mm.deserialize("<red>Usage: /outpost region list <arena></red>"));
                    return;
                }
                String arenaId = args[2].toLowerCase();
                OutpostArena arena = plugin.getArenaManager().getArena(arenaId);
                if (arena == null) {
                    sender.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", arenaId)));
                    return;
                }
                List<ArenaRegion> regs = arena.getDynamicLocationConfig().getRegions();
                if (regs.isEmpty()) {
                    sender.sendMessage(mm.deserialize("<yellow>No dynamic regions configured for '" + arenaId + "'.</yellow>"));
                    return;
                }
                sender.sendMessage(mm.deserialize("<gradient:#FF416C:#8A2387><bold>Regions for '" + arenaId + "' (" + regs.size() + "):</bold></gradient>"));
                int totalWeight = 0;
                for (ArenaRegion r : regs) totalWeight += r.getWeight();
                for (ArenaRegion r : regs) {
                    double chance = totalWeight > 0 ? (r.getWeight() * 100.0 / totalWeight) : 0;
                    boolean isActive = arena.getCurrentRegion() != null && arena.getCurrentRegion().getId().equalsIgnoreCase(r.getId());
                    String activeBadge = isActive ? " <green><bold>[ACTIVE]</bold></green>" : "";
                    Location c = r.getGeometry().getCenterLocation();
                    String coords = c != null ? "(" + c.getBlockX() + ", " + c.getBlockY() + ", " + c.getBlockZ() + ")" : "";
                    sender.sendMessage(mm.deserialize("<gray>• <white>" + r.getId() + "</white> " + mm.serialize(r.getDisplayName())
                            + " <dark_gray>| <gray>Chance: <gold>" + String.format("%.1f", chance) + "%</gold> (" + r.getWeight() + "w) <dark_gray>| <gray>" + coords + activeBadge));
                }
            }
            case "setweight" -> {
                if (args.length < 5) {
                    sender.sendMessage(mm.deserialize("<red>Usage: /outpost region setweight <arena> <regionId> <weight></red>"));
                    return;
                }
                String arenaId = args[2].toLowerCase();
                int weight;
                try {
                    weight = Integer.parseInt(args[4]);
                    if (weight < 0) {
                        sender.sendMessage(mm.deserialize("<red>Weight must be a non-negative integer (>= 0).</red>"));
                        return;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(mm.deserialize("<red>Invalid weight number '" + args[4] + "'. Must be an integer.</red>"));
                    return;
                }
                String regionId = args[3].toLowerCase();
                OutpostArena arena = plugin.getArenaManager().getArena(arenaId);
                if (arena == null) {
                    sender.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", arenaId)));
                    return;
                }
                ArenaRegion existing = arena.getDynamicLocationConfig().getRegion(regionId);
                if (existing == null) {
                    sender.sendMessage(mm.deserialize("<red>Region '" + regionId + "' not found on outpost '" + arenaId + "'.</red>"));
                    return;
                }
                ArenaRegion updated =
                        new ArenaRegion(existing.getId(), existing.getRawName(), weight, existing.getGeometry());
                arena.getDynamicLocationConfig().addRegion(updated);
                File file = new File(plugin.getDataFolder(), "outposts/" + arena.getId() + ".yml");
                try {
                    plugin.getArenaSerializer().saveToFile(arena, file);
                    sender.sendMessage(langManager.get("commands.region_weight_set", Map.of("region", regionId, "arena", arena.getId(), "weight", String.valueOf(weight))));
                } catch (Exception e) {
                    sender.sendMessage(mm.deserialize("<red>Failed to save weight: " + e.getMessage() + "</red>"));
                }
            }
            case "shift" -> {
                if (args.length < 3) {
                    sender.sendMessage(mm.deserialize("<red>Usage: /outpost region shift <arena> [regionId]</red>"));
                    return;
                }
                String arenaId = args[2].toLowerCase();
                OutpostArena arena = plugin.getArenaManager().getArena(arenaId);
                if (arena == null) {
                    sender.sendMessage(langManager.get("commands.outpost_not_found", Map.of("id", arenaId)));
                    return;
                }
                ArenaRegion target = null;
                if (args.length >= 4) {
                    target = arena.getDynamicLocationConfig().getRegion(args[3]);
                    if (target == null) {
                        sender.sendMessage(mm.deserialize("<red>Region '" + args[3] + "' not found on outpost '" + arenaId + "'.</red>"));
                        return;
                    }
                }
                arena.shiftToRegion(target);
                sender.sendMessage(langManager.get("commands.region_shifted", Map.of("arena", arena.getId())));
            }
            default -> sender.sendMessage(mm.deserialize("<red>Usage: /outpost region <add|remove|list|setweight|shift> ...</red>"));
        }
    }

    private void playerOnlyError(CommandSender sender) {
        sender.sendMessage(langManager.get("commands.unknown_subcommand"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("list", "info", "tp", "compass"));
            if (sender.hasPermission("outposts.admin")) {
                subs.addAll(List.of("wand", "create", "delete", "setwarp", "region", "reload", "doctor", "forcestart", "forcestop"));
            }
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("info", "tp", "compass", "delete", "setwarp", "forcestart", "forcestop").contains(sub)) {
                List<String> arenaIds = new ArrayList<>();
                for (OutpostArena a : plugin.getArenaManager().getArenas()) {
                    arenaIds.add(a.getId());
                }
                return filter(arenaIds, args[1]);
            }
            if ("region".equals(sub) && sender.hasPermission("outposts.admin")) {
                return filter(List.of("add", "remove", "list", "setweight", "shift"), args[1]);
            }
        }
        if (args.length == 3) {
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("outposts.admin")) {
                return filter(List.of("TEAM", "SOLO"), args[2]);
            }
            if ("region".equalsIgnoreCase(args[0]) && sender.hasPermission("outposts.admin")) {
                List<String> arenaIds = new ArrayList<>();
                for (OutpostArena a : plugin.getArenaManager().getArenas()) {
                    arenaIds.add(a.getId());
                }
                return filter(arenaIds, args[2]);
            }
        }
        if (args.length == 4 && sender.hasPermission("outposts.admin")) {
            if ("create".equalsIgnoreCase(args[0])) {
                return filter(List.of("STANDARD_HILL", "TUG_OF_WAR", "TICKET_ACCUMULATION"), args[3]);
            }
            if ("region".equalsIgnoreCase(args[0])) {
                String regSub = args[1].toLowerCase();
                if (List.of("remove", "setweight", "shift").contains(regSub)) {
                    OutpostArena arena = plugin.getArenaManager().getArena(args[2]);
                    if (arena != null) {
                        List<String> rIds = new ArrayList<>();
                        for (ArenaRegion r : arena.getDynamicLocationConfig().getRegions()) {
                            rIds.add(r.getId());
                        }
                        return filter(rIds, args[3]);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String query) {
        List<String> res = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(query.toLowerCase())) {
                res.add(s);
            }
        }
        return res;
    }
}
