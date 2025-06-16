package nesoi.aysihuniks.nclaim.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Balance;
import nesoi.aysihuniks.nclaim.model.ChunkAndClaim;
import nesoi.aysihuniks.nclaim.model.ChunkValueResult;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.User;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class Expansion extends PlaceholderExpansion {
    private final NClaim plugin;

    public Expansion(NClaim plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "aysihuniks";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nclaim";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.isEmpty()) {
            return null;
        }

        if (params.equals("player_balance")) {
            return handlePlayerBalance(player);
        }

        if (params.startsWith("get_")) {
            return handleConfigPlaceholder(params);
        }

        if (params.startsWith("claim_main_value_")) {
            return handleClaimMainValue(params);
        }

        if (params.startsWith("claim_total_value_")) {
            return handleClaimTotalValue(params);
        }

        if (params.startsWith("block_value_")) {
            return handleBlockValue(params);
        }

        if (params.startsWith("expiration_") || params.startsWith("owner_") ||
                params.startsWith("coop_count_") || params.startsWith("total_size_")) {
            return handleClaimInfo(params);
        }

        return null;
    }

    private @Nullable String handlePlayerBalance(Player player) {
        if (player == null) return null;
        if (NClaim.inst().getBalanceSystem() == Balance.VAULT) {
            return String.valueOf(NClaim.inst().getEconomy().getBalance(player));
        } else {
            return String.valueOf(User.getUser(player.getUniqueId()).getBalance());
        }
    }

    private @Nullable String handleConfigPlaceholder(String params) {
        String[] parts = params.split("_");
        if (parts.length < 3) {
            return "Invalid config placeholder format";
        }

        String dataType = parts[1];
        String path = parts[2];

        if ("string".equals(dataType)) {
            return plugin.getConfigManager().getString(path, "Null");
        } else if ("int".equals(dataType)) {
            return String.valueOf(plugin.getConfigManager().getInt(path, 0));
        } else if ("boolean".equals(dataType)) {
            return String.valueOf(plugin.getConfigManager().getBoolean(path, false));
        } else if ("list".equals(dataType)) {
            return handleListConfig(parts, path);
        } else {
            return "Unknown config data type: " + dataType;
        }
    }

    private String handleListConfig(String[] parts, String path) {
        if (parts.length < 4) {
            return "Invalid list placeholder: Index required";
        }
        try {
            int index = Integer.parseInt(parts[3]);
            List<String> list = plugin.getConfigManager().getStringList(path);
            if (list == null || list.isEmpty()) {
                return "List Not Found";
            }
            if (index < 0 || index >= list.size()) {
                return "Invalid Index";
            }
            return list.get(index);
        } catch (NumberFormatException e) {
            return "Invalid Index Format";
        }
    }

    private ChunkValueResult getChunkValue(String params, boolean includeAllChunks) {
        String[] parts = params.split("_");
        if (parts.length < 5) {
            return new ChunkValueResult(0, "Invalid format: Expected at least 5 parts");
        }

        ChunkAndClaim result = parseChunkAndClaim(parts[3], parts[4], parts[5]);
        if (result.getError() != null) {
            return new ChunkValueResult(0, result.getError());
        }

        Chunk mainChunk = result.getChunk();
        Claim claim = result.getClaim();

        if (mainChunk == null || (!includeAllChunks && claim == null)) {
            return new ChunkValueResult(0, "Chunk not found or not claimed");
        }

        if (includeAllChunks && claim != null) {
            return new ChunkValueResult(plugin.getBlockValueManager().calculateClaimValue(claim), null);
        } else {
            return new ChunkValueResult(plugin.getBlockValueManager().calculateChunkValue(mainChunk), null);
        }
    }

    private @NotNull String handleClaimMainValue(String params) {
        ChunkValueResult result = getChunkValue(params, false);
        return result.getError() != null ? result.getError() : String.valueOf(result.getValue());
    }

    private @NotNull String handleClaimTotalValue(String params) {
        ChunkValueResult result = getChunkValue(params, true);
        return result.getError() != null ? result.getError() : String.valueOf(result.getValue());
    }

    private @NotNull String handleBlockValue(String params) {
        String materialName = params.substring("block_value_".length()).toUpperCase();
        try {
            Material material = Material.valueOf(materialName);
            return String.valueOf(plugin.getBlockValueManager().getBlockValue(material));
        } catch (IllegalArgumentException e) {
            return "Invalid material: " + materialName;
        }
    }

    private @Nullable String handleClaimInfo(String params) {
        String[] parts = params.split("_");
        if (parts.length < 4) {
            return "Invalid placeholder format";
        }

        String prefix = parts[0];
        if (prefix.equals("total") || prefix.equals("coop_count")) {
            if (parts.length < 5) {
                return "Invalid format for " + prefix;
            }
            prefix = prefix + "_" + parts[1];
        }

        int worldIndex = parts.length - 3;
        ChunkAndClaim result = parseChunkAndClaim(parts[worldIndex], parts[worldIndex + 1], parts[worldIndex + 2]);
        if (result.getError() != null) {
            return result.getError();
        }

        Claim claim = result.getClaim();
        if (claim == null) {
            return "Claim not found";
        }

        if ("expiration".equals(prefix)) {
            return plugin.getClaimExpirationManager().getFormattedTimeLeft(claim);
        } else if ("owner".equals(prefix)) {
            OfflinePlayer claimOwner = Bukkit.getOfflinePlayer(claim.getOwner());
            return claimOwner.getName() != null ? claimOwner.getName() : "Owner not found";
        } else if ("coop_count".equals(prefix)) {
            return String.valueOf(claim.getCoopPlayers().size());
        } else if ("total_size".equals(prefix)) {
            return String.valueOf(1 + claim.getLands().size());
        } else {
            return "Unknown placeholder prefix: " + prefix;
        }
    }

    private ChunkAndClaim parseChunkAndClaim(String worldName, String chunkXStr, String chunkZStr) {
        try {
            int chunkX = Integer.parseInt(chunkXStr);
            int chunkZ = Integer.parseInt(chunkZStr);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return new ChunkAndClaim(null, null, "World not found: " + worldName);
            }

            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            return new ChunkAndClaim(chunk, Claim.getClaim(chunk), null);
        } catch (NumberFormatException e) {
            return new ChunkAndClaim(null, null, "Invalid coordinates");
        }
    }

}