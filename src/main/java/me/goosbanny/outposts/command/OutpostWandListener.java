package me.goosbanny.outposts.command;

import me.goosbanny.outposts.config.LangManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visual setup wand listener and particle box visualizer for outpost creation.
 * Automatically clears particle visualizer upon outpost creation or after a 30-second inactivity timeout.
 */
public class OutpostWandListener implements Listener {

    public static final String WAND_TAG = "outpost_wand";
    public static final long SELECTION_TIMEOUT_MILLIS = 30_000L; // 30-second timeout

    private final NamespacedKey wandKey;
    private final LangManager langManager;
    private final Map<UUID, Selection> playerSelections = new ConcurrentHashMap<>();

    public OutpostWandListener(@NotNull Plugin plugin, @NotNull LangManager langManager) {
        this.wandKey = new NamespacedKey(plugin, WAND_TAG);
        this.langManager = langManager;
    }

    public ItemStack createWand() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gradient:#FF416C:#8A2387><bold>Outpost Selection Wand</bold></gradient>"));
            List<Component> lore = List.of(
                    Component.text("Left-click: Set Position 1", NamedTextColor.GRAY),
                    Component.text("Right-click: Set Position 2", NamedTextColor.GRAY),
                    Component.text("Use /outpost create <id> when ready", NamedTextColor.YELLOW)
            );
            meta.lore(lore);
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isWand(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    @Nullable
    public Selection getSelection(@NotNull UUID uuid) {
        Selection sel = playerSelections.get(uuid);
        if (sel != null && sel.isExpired()) {
            playerSelections.remove(uuid);
            return null;
        }
        return sel;
    }

    public void clearSelection(@NotNull UUID uuid) {
        playerSelections.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!isWand(item)) return;

        if (!player.hasPermission("outposts.admin")) {
            return;
        }

        event.setCancelled(true);
        Block block = event.getClickedBlock();
        if (block == null) return;

        Selection selection = playerSelections.computeIfAbsent(player.getUniqueId(), k -> new Selection());

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selection.setPos1(block.getLocation());
            Map<String, String> tokens = Map.of("x", String.valueOf(block.getX()), "y", String.valueOf(block.getY()), "z", String.valueOf(block.getZ()));
            player.sendMessage(langManager.get("commands.wand_pos1", tokens));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selection.setPos2(block.getLocation());
            Map<String, String> tokens = Map.of("x", String.valueOf(block.getX()), "y", String.valueOf(block.getY()), "z", String.valueOf(block.getZ()));
            player.sendMessage(langManager.get("commands.wand_pos2", tokens));
        }
    }

    /**
     * Renders live particle outline for active selections.
     * Enforces a 30-second inactivity timeout.
     */
    public void tickVisuals() {
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 105, 180), 1.0f);
        Iterator<Map.Entry<UUID, Selection>> it = playerSelections.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Selection> entry = it.next();
            Selection sel = entry.getValue();

            // Check 30-second timeout
            if (sel.isExpired()) {
                it.remove();
                continue;
            }

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }
            if (!sel.isComplete()) continue;

            Location p1 = sel.getPos1();
            Location p2 = sel.getPos2();
            if (p1.getWorld() == null || !p1.getWorld().equals(p2.getWorld())) continue;

            double minX = Math.min(p1.getBlockX(), p2.getBlockX());
            double minY = Math.min(p1.getBlockY(), p2.getBlockY());
            double minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
            double maxX = Math.max(p1.getBlockX(), p2.getBlockX()) + 1.0;
            double maxY = Math.max(p1.getBlockY(), p2.getBlockY()) + 1.0;
            double maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ()) + 1.0;

            World world = p1.getWorld();
            double step = 1.5;

            // Draw selection box
            drawEdge(world, dust, minX, minY, minZ, maxX, minY, minZ, step);
            drawEdge(world, dust, minX, minY, maxZ, maxX, minY, maxZ, step);
            drawEdge(world, dust, minX, minY, minZ, minX, minY, maxZ, step);
            drawEdge(world, dust, maxX, minY, minZ, maxX, minY, maxZ, step);

            drawEdge(world, dust, minX, maxY, minZ, maxX, maxY, minZ, step);
            drawEdge(world, dust, minX, maxY, maxZ, maxX, maxY, maxZ, step);
            drawEdge(world, dust, minX, maxY, minZ, minX, maxY, maxZ, step);
            drawEdge(world, dust, maxX, maxY, minZ, maxX, maxY, maxZ, step);

            drawEdge(world, dust, minX, minY, minZ, minX, maxY, minZ, step);
            drawEdge(world, dust, maxX, minY, minZ, maxX, maxY, minZ, step);
            drawEdge(world, dust, minX, minY, maxZ, minX, maxY, maxZ, step);
            drawEdge(world, dust, maxX, minY, maxZ, maxX, maxY, maxZ, step);
        }
    }

    private void drawEdge(World world, Particle.DustOptions dust, double x1, double y1, double z1, double x2, double y2, double z2, double step) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int points = Math.max(1, (int) (length / step));
        for (int i = 0; i <= points; i++) {
            double ratio = (double) i / points;
            world.spawnParticle(Particle.REDSTONE, x1 + dx * ratio, y1 + dy * ratio, z1 + dz * ratio, 1, 0, 0, 0, 0, dust);
        }
    }

    public static class Selection {
        private Location pos1;
        private Location pos2;
        private long lastInteractionTime = System.currentTimeMillis();

        public Location getPos1() { return pos1; }
        public void setPos1(Location pos1) { 
            this.pos1 = pos1; 
            this.lastInteractionTime = System.currentTimeMillis();
        }

        public Location getPos2() { return pos2; }
        public void setPos2(Location pos2) { 
            this.pos2 = pos2; 
            this.lastInteractionTime = System.currentTimeMillis();
        }

        public boolean isComplete() { return pos1 != null && pos2 != null; }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastInteractionTime > SELECTION_TIMEOUT_MILLIS;
        }

        public long getLastInteractionTime() {
            return lastInteractionTime;
        }
    }
}
