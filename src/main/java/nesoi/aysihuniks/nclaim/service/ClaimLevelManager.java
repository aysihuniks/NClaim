package nesoi.aysihuniks.nclaim.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.api.events.ClaimCalculateLevelEvent;
import nesoi.aysihuniks.nclaim.model.ClaimLevel;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.nandayo.dapi.util.Util;
import org.nandayo.dapi.message.ChannelType;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public class ClaimLevelManager {
    private final NClaim plugin;

    @Getter
    private final Map<Material, ClaimLevel> blockValues = new HashMap<>();

    private final List<ClaimCalculationRequest> calculationQueue = new ArrayList<>();
    private boolean isProcessing = false;
    private String currentlyCalculating = null;

    private File configFile;
    private FileConfiguration config;

    private static class ClaimCalculationRequest {
        @Getter
        private final UUID playerId;
        @Getter
        private final String playerName;
        @Getter
        private final Claim claim;

        public ClaimCalculationRequest(UUID playerId, String playerName, Claim claim) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.claim = claim;
        }
    }

    public void loadBlockValues() {
        configFile = new File(plugin.getDataFolder(), "block_levels.yml");
        if (!configFile.exists()) {
            Util.log("&eblock_levels.yml does not exist, creating default...");
            plugin.saveResource("block_levels.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        blockValues.clear();

        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection == null) {
            Util.log("&cNo 'blocks' section found in block_levels.yml!");
            return;
        }

        int loadedCount = 0;
        for (String key : blocksSection.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                int value = blocksSection.getInt(key, 0);

                if (value > 0) {
                    blockValues.put(material, new ClaimLevel(material, value));
                    loadedCount++;
                }
            } catch (IllegalArgumentException ignored) {}
        }

        Util.log("&aLoaded " + loadedCount + " block values.");
    }

    public void saveBlockValues() {
        if (config == null) {
            return;
        }

        config.set("blocks", null);
        ConfigurationSection blocksSection = config.createSection("blocks");

        int savedCount = 0;
        for (Map.Entry<Material, ClaimLevel> entry : blockValues.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                blocksSection.set(entry.getKey().name().toLowerCase(), entry.getValue().getValue());
                savedCount++;
            }
        }

        try {
            config.save(configFile);
            Util.log("&aSaved " + savedCount + " block values to block_levels.yml.");
        } catch (IOException e) {
            Util.log("&cCould not save block_levels.yml: " + e.getMessage());
        }
    }

    public void reloadBlockValues() {
        saveBlockValues();
        loadBlockValues();
    }

    public int getBlockValue(Material material) {
        if (material == null) {
            return 0;
        }
        ClaimLevel blockValue = blockValues.get(material);
        return blockValue != null ? blockValue.getValue() : 0;
    }

    public void setBlockValue(Material material, int value) {
        if (material == null || value <= 0) {
            return;
        }
        blockValues.put(material, new ClaimLevel(material, value));
        saveBlockValues();
    }

    public boolean isValidBlock(Material material) {
        return material != null && blockValues.containsKey(material);
    }

    public void removeBlockValue(Material material) {
        if (material != null && blockValues.containsKey(material)) {
            blockValues.remove(material);
            saveBlockValues();
        }
    }

    public long calculateClaimValue(Claim claim) {
        if (claim == null) return 0;

        long oldValue = claim.getClaimValue();

        long totalValue = 0;
        Chunk mainChunk = claim.getChunk();
        totalValue += calculateChunkValue(mainChunk);

        for (String landStr : claim.getLands()) {
            String[] parts = landStr.split(",");
            if (parts.length == 3) {
                try {
                    World world = Bukkit.getWorld(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    if (world != null) {
                        Chunk landChunk = world.getChunkAt(x, z);
                        totalValue += calculateChunkValue(landChunk);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        ClaimCalculateLevelEvent event = new ClaimCalculateLevelEvent(claim, oldValue, totalValue);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return event.getOldLevel();
        }
        return event.getNewLevel();
    }

    public void requestClaimCalculation(UUID playerId, String playerName, Claim claim) {
        boolean alreadyInQueue = calculationQueue.stream()
                .anyMatch(req -> req.getPlayerId().equals(playerId));

        if (alreadyInQueue) {
            sendMessageToPlayer(playerId, NClaim.inst().getLangManager().getString("claim.level.your_already_in_queue"));
            return;
        }

        ClaimCalculationRequest request = new ClaimCalculationRequest(playerId, playerName, claim);
        calculationQueue.add(request);

        int position = calculationQueue.size();
        sendMessageToPlayer(playerId, NClaim.inst().getLangManager().getString("claim.level.calculating_in_queue_position").replace("{position}", String.valueOf(position)));

        if (!isProcessing) {
            processNextCalculation();
        }
    }

    private void processNextCalculation() {
        if (calculationQueue.isEmpty()) {
            isProcessing = false;
            currentlyCalculating = null;
            return;
        }

        isProcessing = true;
        ClaimCalculationRequest request = calculationQueue.remove(0);
        currentlyCalculating = request.getPlayerName();

        sendMessageToPlayer(request.getPlayerId(), NClaim.inst().getLangManager().getString("claim.level.calculating_started"));

        long startTime = System.currentTimeMillis();
        long claimValue = calculateClaimValue(request.getClaim());
        long duration = System.currentTimeMillis() - startTime;

        String message = NClaim.inst().getLangManager().getString("command.level.info")
                .replace("{value}", String.format("%,d", claimValue))
                .replace("{chunks}", String.valueOf(request.getClaim().getLands().size() + 1))
                .replace("{time}", duration + "ms");

        sendMessageToPlayer(request.getPlayerId(), message);

        isProcessing = false;
        processNextCalculation();
    }

    private void sendMessageToPlayer(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            ChannelType.CHAT.send(player, message);
        }
    }

    public void checkQueueStatus(UUID playerId) {
        if (currentlyCalculating != null) {
            sendMessageToPlayer(playerId, NClaim.inst().getLangManager().getString("claim.level.calculating_in_progress"));
        }

        if (!calculationQueue.isEmpty()) {
            sendMessageToPlayer(playerId, NClaim.inst().getLangManager().getString("claim.level.calculating_in_queue").replace("{count}", String.valueOf(calculationQueue.size())));

            for (int i = 0; i < calculationQueue.size(); i++) {
                if (calculationQueue.get(i).getPlayerId().equals(playerId)) {
                    sendMessageToPlayer(playerId, NClaim.inst().getLangManager().getString("claim.level.calculating_in_queue_position").replace("{position}", String.valueOf(i + 1)));
                    break;
                }
            }
        } else {
            sendMessageToPlayer(playerId, NClaim.inst().getLangManager().getString("claim.level.queue_is_empty"));
        }
    }

    public long calculateChunkValue(Chunk chunk) {
        if (chunk == null) {
            return 0;
        }

        return performChunkCalculation(chunk);
    }

    private long performChunkCalculation(Chunk chunk) {
        if (chunk == null) {
            return 0;
        }

        long chunkValue = 0;
        World world = chunk.getWorld();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getX() * 16 + x;
                int worldZ = chunk.getZ() * 16 + z;
                int highestY = world.getHighestBlockYAt(worldX, worldZ);

                int SURFACE_DEPTH = 15;
                for (int y = Math.max(world.getMinHeight(), highestY - SURFACE_DEPTH); y <= highestY; y++) {
                    Material blockType = chunk.getBlock(x, y, z).getType();
                    if (isValidMaterial(blockType)) {
                        int blockValue = getBlockValue(blockType);
                        if (blockValue > 0) {
                            chunkValue += blockValue;
                        }
                    }
                }

                int UNDERGROUND_SKIP = 2;
                for (int y = world.getMinHeight(); y < Math.max(world.getMinHeight(), highestY - SURFACE_DEPTH); y += UNDERGROUND_SKIP) {
                    Material blockType = chunk.getBlock(x, y, z).getType();
                    int blockValue = getBlockValue(blockType);
                    if (blockValue > 10) {
                        chunkValue += blockValue;
                    }
                }
            }
        }

        return chunkValue;
    }

    private boolean isValidMaterial(Material material) {
        return material != null &&
                material != Material.AIR &&
                material != Material.CAVE_AIR &&
                material != Material.VOID_AIR;
    }

    private String getChunkCacheKey(Chunk chunk) {
        return chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
    }
}