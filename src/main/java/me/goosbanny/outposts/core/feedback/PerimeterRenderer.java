package me.goosbanny.outposts.core.feedback;

import me.goosbanny.outposts.api.arena.OutpostArena;
import me.goosbanny.outposts.core.arena.DefaultOutpostArena;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders live RGB dust perimeter particle outlines connecting the cuboid corners.
 * Performance-optimized with distance-culling and targeted client packets.
 */
public class PerimeterRenderer {

    private static final Particle.DustOptions DUST_CONTESTED = new Particle.DustOptions(Color.fromRGB(255, 30, 30), 1.0f);
    private static final Particle.DustOptions DUST_CONTROLLED = new Particle.DustOptions(Color.fromRGB(30, 220, 60), 1.0f);
    private static final Particle.DustOptions DUST_CAPTURING = new Particle.DustOptions(Color.fromRGB(255, 200, 30), 1.0f);
    private static final Particle.DustOptions DUST_NEUTRAL = new Particle.DustOptions(Color.fromRGB(200, 200, 200), 1.0f);

    private static final Particle DUST_PARTICLE;

    static {
        Particle p;
        try {
            p = Particle.valueOf("DUST");
        } catch (IllegalArgumentException | NoSuchFieldError e) {
            try {
                p = Particle.valueOf("REDSTONE");
            } catch (Exception ex) {
                p = Particle.REDSTONE;
            }
        }
        DUST_PARTICLE = p;
    }

    private final int maxRenderDistanceBlocks;

    public PerimeterRenderer(int maxRenderDistanceBlocks) {
        this.maxRenderDistanceBlocks = Math.max(16, maxRenderDistanceBlocks);
    }

    /**
     * Renders boundary particles for an arena along the 12 edges of its AABB.
     */
    public void renderPerimeter(@NotNull OutpostArena arena) {
        if (!arena.isBoundingParticlesEnabled()) return;
        Location center = arena.getCenterLocation();
        if (center == null || center.getWorld() == null) return;

        int renderDist = arena instanceof DefaultOutpostArena def
                ? def.getBoundaryRenderDistance()
                : this.maxRenderDistanceBlocks;
        double rSquared = (double) renderDist * renderDist;

        World world = center.getWorld();
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.isOnline() && !p.isDead()) {
                Location loc = p.getLocation();
                if (loc.getWorld() != null && loc.getWorld().equals(world) && loc.distanceSquared(center) <= rSquared) {
                    nearbyPlayers.add(p);
                }
            }
        }
        if (nearbyPlayers.isEmpty()) return;

        Particle.DustOptions dustOptions;
        if (arena.isContested()) {
            dustOptions = DUST_CONTESTED;
        } else if (arena.getControllerTeamId() != null) {
            dustOptions = DUST_CONTROLLED;
        } else if (arena.getCappingTeamId() != null) {
            dustOptions = DUST_CAPTURING;
        } else {
            dustOptions = DUST_NEUTRAL;
        }

        double minX = arena.getMinX();
        double minY = arena.getMinY();
        double minZ = arena.getMinZ();
        double maxX = arena.getMaxX() + 1.0;
        double maxY = arena.getMaxY() + 1.0;
        double maxZ = arena.getMaxZ() + 1.0;

        double step = 2.0;

        // 4 Bottom edges
        drawEdge(world, nearbyPlayers, dustOptions, minX, minY, minZ, maxX, minY, minZ, step);
        drawEdge(world, nearbyPlayers, dustOptions, minX, minY, maxZ, maxX, minY, maxZ, step);
        drawEdge(world, nearbyPlayers, dustOptions, minX, minY, minZ, minX, minY, maxZ, step);
        drawEdge(world, nearbyPlayers, dustOptions, maxX, minY, minZ, maxX, minY, maxZ, step);

        // 4 Top edges
        drawEdge(world, nearbyPlayers, dustOptions, minX, maxY, minZ, maxX, maxY, minZ, step);
        drawEdge(world, nearbyPlayers, dustOptions, minX, maxY, maxZ, maxX, maxY, maxZ, step);
        drawEdge(world, nearbyPlayers, dustOptions, minX, maxY, minZ, minX, maxY, maxZ, step);
        drawEdge(world, nearbyPlayers, dustOptions, maxX, maxY, minZ, maxX, maxY, maxZ, step);

        // 4 Vertical edges
        drawEdge(world, nearbyPlayers, dustOptions, minX, minY, minZ, minX, maxY, minZ, step);
        drawEdge(world, nearbyPlayers, dustOptions, maxX, minY, minZ, maxX, maxY, minZ, step);
        drawEdge(world, nearbyPlayers, dustOptions, minX, minY, maxZ, minX, maxY, maxZ, step);
        drawEdge(world, nearbyPlayers, dustOptions, maxX, minY, maxZ, maxX, maxY, maxZ, step);
    }

    private void drawEdge(World world, List<Player> players, Particle.DustOptions dust, double x1, double y1, double z1, double x2, double y2, double z2, double step) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int points = Math.max(1, (int) (length / step));

        for (int i = 0; i <= points; i++) {
            double ratio = (double) i / points;
            double px = x1 + dx * ratio;
            double py = y1 + dy * ratio;
            double pz = z1 + dz * ratio;
            for (Player p : players) {
                p.spawnParticle(DUST_PARTICLE, px, py, pz, 1, 0, 0, 0, 0, dust);
            }
        }
    }
}
