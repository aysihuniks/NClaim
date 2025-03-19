package nesoi.network.NClaim.utils;

import nesoi.network.NClaim.NCoreMain;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class ChunkBorderManager {

    private final HashMap<UUID, BukkitRunnable> activeBorders = new HashMap<>();

    public void showChunkBorder(Player player, Chunk chunk) {
        UUID playerId = player.getUniqueId();

        closeChunkBorder(player);

        BukkitRunnable task = new BukkitRunnable() {
            int counter = 30;

            @Override
            public void run() {
                if (counter <= 0) {
                    closeChunkBorder(player);

                }

                counter--;
                drawChunkBorder(player, chunk);
            }
        };

        task.runTaskTimer(NCoreMain.inst(), 0L, 20L);
        activeBorders.put(playerId, task);
    }

    public void closeChunkBorder(Player player) {
        UUID playerId = player.getUniqueId();
        if (activeBorders.containsKey(playerId)) {
            BukkitRunnable task = activeBorders.get(playerId);
            task.cancel();
            activeBorders.remove(playerId);

        }

    }

    private void drawChunkBorder(Player player, Chunk chunk) {
        World world = chunk.getWorld();
        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        int y = player.getLocation().getBlockY();


        for (int x = minX; x <= maxX; x += 2) {
            spawnParticle(world, x, y, minZ);
            spawnParticle(world, x, y, maxZ);
        }

        for (int z = minZ; z <= maxZ; z += 2) {
            spawnParticle(world, minX, y, z);
            spawnParticle(world, maxX, y, z);
        }
    }

    private void spawnParticle(World world, int x, int y, int z) {
        world.spawnParticle(Particle.COMPOSTER, new Location(world, x + 0.5, y + 0.1, z + 0.5), 1, 0, 0, 0, 0);
    }
}
//pup