package nesoi.aysihuniks.nclaim.service;

import lombok.RequiredArgsConstructor;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.HashSet;

@RequiredArgsConstructor
public class ClaimVisualizerService {
    private final NClaim plugin;
    private final Map<UUID, BukkitTask> activeVisualizations = new HashMap<>();
    private final Set<UUID> playersInPreviewMode = new HashSet<>(); 

    public void showClaimBorders(Player player, Chunk chunk) {
        cancelVisualization(player);
        playersInPreviewMode.add(player.getUniqueId());

        AtomicInteger counter = new AtomicInteger(0);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !player.getWorld().equals(chunk.getWorld()) || counter.get() >= 10) {
                cancelVisualization(player);
                return;
            }
            visualizeChunkBorders(player, chunk);
            counter.incrementAndGet();
        }, 0L, 20L);

        activeVisualizations.put(player.getUniqueId(), task);
    }

    public void showClaimBorders(Player player) {
        cancelVisualization(player);
        playersInPreviewMode.add(player.getUniqueId());

        AtomicInteger counter = new AtomicInteger(0);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || counter.get() >= 10) {
                cancelVisualization(player);
                return;
            }
            Chunk currentChunk = player.getLocation().getChunk();
            visualizeChunkBorders(player, currentChunk);
            counter.incrementAndGet();
        }, 0L, 20L);

        activeVisualizations.put(player.getUniqueId(), task);
    }

    public void cancelVisualization(Player player) {
        BukkitTask task = activeVisualizations.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        playersInPreviewMode.remove(player.getUniqueId());
    }

    private void visualizeChunkBorders(Player player, Chunk chunk) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;
        double playerY = player.getLocation().getY();

        int maxHeight = 32;
        int stepHeight = 4;

        List<Location> corners = new ArrayList<>();
        corners.add(new Location(world, chunkX, playerY, chunkZ));
        corners.add(new Location(world, chunkX + 15, playerY, chunkZ));
        corners.add(new Location(world, chunkX + 15, playerY, chunkZ + 15));
        corners.add(new Location(world, chunkX, playerY, chunkZ + 15));

        for (int i = 0; i < corners.size(); i++) {
            Location start = corners.get(i);
            Location end = corners.get((i + 1) % corners.size());

            double distance = start.distance(end);
            Vector direction = end.toVector().subtract(start.toVector()).normalize();

            for (double d = 0; d <= distance; d += 0.5) {
                Location particleLoc = start.clone().add(direction.clone().multiply(d));
                player.spawnParticle(Particle.END_ROD, particleLoc, 0, 0, 0, 0, 0);
            }
        }

        for (Location corner : corners) {
            for (double y = playerY; y < playerY + maxHeight; y += 0.5) {
                Location pillarLoc = corner.clone();
                pillarLoc.setY(y);
                player.spawnParticle(Particle.END_ROD, pillarLoc, 0, 0, 0, 0, 0);
            }
            for (double y = playerY; y > playerY - maxHeight; y -= 0.5) {
                Location pillarLoc = corner.clone();
                pillarLoc.setY(y);
                player.spawnParticle(Particle.END_ROD, pillarLoc, 0, 0, 0, 0, 0);
            }
        }

        for (double y = playerY + stepHeight; y < playerY + maxHeight; y += stepHeight) {
            for (int i = 0; i < corners.size(); i++) {
                Location start = corners.get(i).clone().add(0, y - playerY, 0);
                Location end = corners.get((i + 1) % corners.size()).clone().add(0, y - playerY, 0);

                double distance = start.distance(end);
                Vector direction = end.toVector().subtract(start.toVector()).normalize();

                for (double d = 0; d <= distance; d += 0.5) {
                    Location particleLoc = start.clone().add(direction.clone().multiply(d));
                    player.spawnParticle(Particle.END_ROD, particleLoc, 0, 0, 0, 0, 0);
                }
            }
        }
    }
}