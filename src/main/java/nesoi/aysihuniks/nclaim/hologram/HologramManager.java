package nesoi.aysihuniks.nclaim.hologram;

import nesoi.aysihuniks.nclaim.Config;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.HoloEnum;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.nandayo.dapi.util.HexUtil;
import org.nandayo.dapi.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HologramManager {
    private final NClaim plugin;
    private static final double DECENT_HOLO_OFFSET = 3.5;
    private static final double FANCY_HOLO_OFFSET = 1.5;
    private HologramHandler hologramHandler;
    private final Set<String> pendingWorlds = new HashSet<>();
    private boolean initialCleanupDone = false;

    private static final Pattern HOLOGRAM_ID_PATTERN = Pattern.compile("claim_(.+)_(-?\\d+)_(-?\\d+)");

    public HologramManager(NClaim plugin) {
        this.plugin = plugin;
        initializeHologramHandler();
        identifyPendingWorlds();
        scheduleInitialCleanup();
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

    private void identifyPendingWorlds() {
        if (plugin.getNconfig().isDatabaseEnabled()) return;

        File file = new File(plugin.getDataFolder(), "claims.yml");
        if (!file.exists()) return;

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = config.getConfigurationSection("chunks_claimed");
            if (section == null) return;

            for (String claimId : section.getKeys(false)) {
                String[] parts = claimId.split("_");
                if (parts.length == 3) {
                    String worldName = parts[0];
                    if (Bukkit.getWorld(worldName) == null) {
                        pendingWorlds.add(worldName);
                    }
                }
            }

            if (!pendingWorlds.isEmpty()) {
                Util.log("&eWaiting for " + pendingWorlds.size() + " worlds to load: " + pendingWorlds);
            }

        } catch (Exception e) {
            Util.log("&cError identifying pending worlds: " + e.getMessage());
        }
    }

    private void scheduleInitialCleanup() {
        new BukkitRunnable() {
            @Override
            public void run() {
                smartCleanupOrphanedHolograms();
                initialCleanupDone = true;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        validateAndRecreateHolograms();
                    }
                }.runTaskLater(plugin, 60L);
            }
        }.runTaskLater(plugin, 20L * 8);
    }


    public void smartCleanupOrphanedHolograms() {
        List<String> allHologramIds = hologramHandler.getHologramIds();
        int removedCount = 0;
        int protectedCount = 0;

        for (String hologramId : allHologramIds) {
            if (!hologramId.startsWith("claim_")) continue;

            ChunkInfo chunkInfo = parseHologramId(hologramId);
            if (chunkInfo == null) continue;

            if (pendingWorlds.contains(chunkInfo.worldName)) {
                protectedCount++;
                continue;
            }

            World world = Bukkit.getWorld(chunkInfo.worldName);
            if (world == null) {
                hologramHandler.deleteHologram(hologramId);
                removedCount++;
                continue;
            }

            Chunk chunk = world.getChunkAt(chunkInfo.x, chunkInfo.z);
            Claim claim = Claim.getClaim(chunk);

            if (claim == null) {
                hologramHandler.deleteHologram(hologramId);
                removedCount++;
            }
        }

        if (removedCount > 0 || protectedCount > 0) {
            Util.log("&aHologram cleanup completed. Removed: " + removedCount + " | Protected: " + protectedCount);
        }
    }

    public void onWorldLoaded(String worldName) {
        if (!pendingWorlds.contains(worldName)) return;

        Util.log("&aPending world '" + worldName + "' loaded! Checking holograms...");
        pendingWorlds.remove(worldName);

        new BukkitRunnable() {
            @Override
            public void run() {
                createHologramsForWorld(worldName);
            }
        }.runTaskLater(plugin, 10L);
    }

    public void createHologramsForWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        int created = 0;
        for (Claim claim : Claim.claims) {
            if (claim.getChunk().getWorld().getName().equals(worldName)) {
                String hologramId = getHologramId(claim.getChunk());

                if (hologramExists(hologramId)) {
                    createHologramForClaim(claim);
                    created++;
                }
            }
        }

        if (created > 0) {
            Util.log("&aCreated " + created + " holograms for world '" + worldName + "'");
        }
    }

    public void validateAndRecreateHolograms() {
        if (!initialCleanupDone) return;

        int created = 0;
        for (Claim claim : Claim.claims) {
            String hologramId = getHologramId(claim.getChunk());

            if (hologramExists(hologramId)) {
                createHologramForClaim(claim);
                created++;
            }
        }

        if (created > 0) {
            Util.log("&aRecreated " + created + " missing holograms.");
        }
    }

    public boolean hologramExists(String hologramId) {
        return !hologramHandler.getHologramIds().contains(hologramId);
    }

    public void createHologramForClaim(Claim claim) {
        if (claim == null) return;

        Location location = claim.getClaimBlockLocation();
        if (location.getWorld() == null) return;

        String hologramId = getHologramId(claim.getChunk());
        hologramHandler.deleteHologram(hologramId);

        List<String> lines = generateHologramLines(claim);
        Location adjustedLocation = getCenteredLocation(location.clone(), lines.size());

        hologramHandler.createHologram(hologramId, adjustedLocation, lines);
    }

    public void cleanupOrphanedHolograms() {
        smartCleanupOrphanedHolograms();
    }

    public void forceCleanup() {
        new BukkitRunnable() {
            @Override
            public void run() {
                smartCleanupOrphanedHolograms();
            }
        }.runTaskAsynchronously(plugin);
    }

    public void createHologram(Location location) {
        Chunk chunk = location.getChunk();
        Claim claim = Claim.getClaim(chunk);
        if (claim == null) return;

        String hologramId = getHologramId(chunk);
        List<String> lines = generateHologramLines(claim);
        Location adjustedLocation = getCenteredLocation(location.clone(), lines.size());

        deleteHologram(chunk);
        hologramHandler.createHologram(hologramId, adjustedLocation, lines);
    }

    public void deleteHologram(Chunk chunk) {
        String hologramId = getHologramId(chunk);
        hologramHandler.deleteHologram(hologramId);
    }

    private Location getCenteredLocation(Location location, int lineCount) {
        double baseOffset = HoloEnum.getActiveHologram() == HoloEnum.DECENT_HOLOGRAM ?
                DECENT_HOLO_OFFSET : FANCY_HOLO_OFFSET;

        return location.clone().add(0.5, baseOffset, 0.5);
    }

    private double getLineSpacing() {
        if (HoloEnum.getActiveHologram() == HoloEnum.DECENT_HOLOGRAM) {
            return 0.3;
        } else {
            return 0.25;
        }
    }

    private String getHologramId(Chunk chunk) {
        return "claim_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
    }

    private List<String> generateHologramLines(Claim claim) {
        List<String> lines = new ArrayList<>();
        Chunk chunk = claim.getChunk();

        Config config = NClaim.inst().getNconfig();

        if (config.isShowHologramTitle()) {
            lines.add(plugin.getLangManager().getString("hologram.title").replace("{display_name}", claim.getDisplayName()));
        }

        if (config.isShowHologramOwner()) {
            lines.add(plugin.getLangManager().getString("hologram.owner")
                    .replace("{owner}", "%nclaim_owner_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));

        }

        if (config.isShowHologramTimeLeft()) {
            lines.add(plugin.getLangManager().getString("hologram.time_left.text")
                    .replace("{time_left}", "%nclaim_expiration_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));

        }

        if (config.isShowHologramCoopCount()) {
            lines.add(plugin.getLangManager().getString("hologram.coop_count")
                    .replace("{coop_count}", "%nclaim_coop_count_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));

        }

        if (config.isShowHologramTotalSize()) {
            lines.add(plugin.getLangManager().getString("hologram.total_size")
                    .replace("{total_size}", "%nclaim_total_size_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));

        }

        long shownLineCount = lines.stream().filter(s -> !s.isEmpty()).count();
        if (shownLineCount > 1) {
            lines.add("");
        }

        if (config.isShowHologramEdit()) {
            lines.add(plugin.getLangManager().getString("hologram.edit"));
        }


        return lines.stream()
                .map(HexUtil::parse)
                .collect(java.util.stream.Collectors.toList());
    }

    private ChunkInfo parseHologramId(String hologramId) {
        Matcher matcher = HOLOGRAM_ID_PATTERN.matcher(hologramId);
        if (!matcher.matches()) return null;

        try {
            String worldName = matcher.group(1);
            int x = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            return new ChunkInfo(worldName, x, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Set<String> getPendingWorlds() {
        return new HashSet<>(pendingWorlds);
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