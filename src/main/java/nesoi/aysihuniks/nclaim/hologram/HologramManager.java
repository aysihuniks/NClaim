package nesoi.aysihuniks.nclaim.hologram;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.HoloEnum;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.nandayo.dapi.HexUtil;
import org.nandayo.dapi.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HologramManager {
    private final NClaim plugin;
    private static final double DECENT_HOLO_OFFSET = 3;
    private static final double FANCY_HOLO_OFFSET = 1.5;
    private HologramHandler hologramHandler;

    private static final Pattern HOLOGRAM_ID_PATTERN = Pattern.compile("claim_(.+)_(-?\\d+)_(-?\\d+)");

    public HologramManager(NClaim plugin) {
        this.plugin = plugin;
        initializeHologramHandler();

        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOrphanedHolograms();
            }
        }.runTaskLater(plugin, 20L * 3);
    }

    private void initializeHologramHandler() {
        if (HoloEnum.getActiveHologram() == HoloEnum.DECENT_HOLOGRAM) {
            hologramHandler = new DecentHologramHandler();
        } else if (HoloEnum.getActiveHologram() == HoloEnum.FANCY_HOLOGRAM) {
            hologramHandler = new FancyHologramHandler();
        } else {
            throw new IllegalStateException("No supported hologram plugin found!");
        }
    }

    public void cleanupOrphanedHolograms() {
        Util.log("&eStarting orphaned hologram cleanup... ");

        List<String> allHologramIds = hologramHandler.getHologramIds();
        int removedCount = 0;

        for (String hologramId : allHologramIds) {
            if (!hologramId.startsWith("claim_")) {
                continue;
            }

            ChunkInfo chunkInfo = parseHologramId(hologramId);
            if (chunkInfo == null) {
                continue;
            }

            World world = Bukkit.getWorld(chunkInfo.worldName);
            if (world == null) {
                Util.log("&cWorld not found, removing hologram: &f" + hologramId);
                hologramHandler.deleteHologram(hologramId);
                removedCount++;
                continue;
            }

            Chunk chunk = world.getChunkAt(chunkInfo.x, chunkInfo.z);
            Claim claim = Claim.getClaim(chunk);

            if (claim == null) {
                Util.log("&aClaim not found, removing hologram: &f" + hologramId);
                hologramHandler.deleteHologram(hologramId);
                removedCount++;
            }
        }

        Util.log("&aOrphaned hologram cleanup completed. Removed &f" + removedCount + " &aholograms.");
    }

    private ChunkInfo parseHologramId(String hologramId) {
        Matcher matcher = HOLOGRAM_ID_PATTERN.matcher(hologramId);
        if (!matcher.matches()) {
            return null;
        }

        try {
            String worldName = matcher.group(1);
            int x = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            return new ChunkInfo(worldName, x, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void forceCleanup() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOrphanedHolograms();
            }
        }.runTaskAsynchronously(plugin);
    }

    public void createHologram(Location location) {
        Chunk chunk = location.getChunk();
        Claim claim = Claim.getClaim(chunk);
        if (claim == null) return;

        String hologramId = getHologramId(chunk);
        List<String> lines = generateHologramLines(claim);
        Location adjustedLocation = getAdjustedLocation(location.clone());

        deleteHologram(chunk);
        hologramHandler.createHologram(hologramId, adjustedLocation, lines);
    }

    public void deleteHologram(Chunk chunk) {
        String hologramId = getHologramId(chunk);
        hologramHandler.deleteHologram(hologramId);
    }

    private Location getAdjustedLocation(Location location) {
        double offset = HoloEnum.getActiveHologram() == HoloEnum.DECENT_HOLOGRAM ?
                DECENT_HOLO_OFFSET : FANCY_HOLO_OFFSET;
        return location.add(0.5, offset, 0.5);
    }

    private String getHologramId(Chunk chunk) {
        return "claim_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
    }

    private List<String> generateHologramLines(Claim claim) {
        List<String> lines = new ArrayList<>();
        Chunk chunk = claim.getChunk();

        lines.add(plugin.getLangManager().getString("hologram.title"));
        lines.add(plugin.getLangManager().getString("hologram.owner")
                .replace("{owner}", "%nclaim_owner_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));
        lines.add(plugin.getLangManager().getString("hologram.time_left.text")
                .replace("{time_left}", "%nclaim_expiration_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));

        int coopCount = claim.getCoopPlayers().size();
        if (coopCount > 0) {
            lines.add(plugin.getLangManager().getString("hologram.coop_count")
                    .replace("{coop_count}", "%nclaim_coop_count_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));
        }

        lines.add(plugin.getLangManager().getString("hologram.total_size")
                .replace("{total_size}", "%nclaim_total_size_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));
        lines.add("");
        lines.add(plugin.getLangManager().getString("hologram.edit"));

        return lines.stream()
                .map(HexUtil::parse)
                .collect(java.util.stream.Collectors.toList());
    }

    private static class ChunkInfo {
        final String worldName;
        final int x;
        final int z;

        ChunkInfo(String worldName, int x, int z) {
            this.worldName = worldName;
            this.x = x;
            this.z = z;
        }
    }
}