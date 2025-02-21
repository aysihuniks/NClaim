package nesoi.network.NClaim.models;


import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import nesoi.network.NClaim.Config;
import nesoi.network.NClaim.NCoreMain;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

import static nesoi.network.NClaim.NCoreMain.economy;

public class PlaceholderManager extends PlaceholderExpansion {

    private final NCoreMain plugin;

    public PlaceholderManager(NCoreMain plugin) {
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
    public String onPlaceholderRequest(Player player, String params) {

        if (params.equals("player_balance")) {
            String moneyData = NCoreMain.inst().configManager.getString("money-data", "PlayerData");
            if (moneyData.equals("Vault")) {
                return String.valueOf(economy.getBalance(player));
            } else if (moneyData.equals("PlayerData")) {
                PlayerDataManager playerDataManager = NCoreMain.pdCache.get(player);
                return String.valueOf(playerDataManager.getBalance());
            } else {
                return NCoreMain.inst().langManager.getMsg("messages.setup-your-config-file");
            }

        }

        if (params.startsWith("get_")) {
            String[] parts = params.split("_");
            if (parts.length < 3) return null;

            String data = parts[1];
            String path = parts[2];

            return switch (data) {
                case "string" -> NCoreMain.inst().configManager.getString(path, "Null");
                case "int" -> String.valueOf(NCoreMain.inst().configManager.getInt(path, 0));
                case "boolean" -> String.valueOf(NCoreMain.inst().configManager.getBoolean(path, false));
                case "list" -> {
                    if (parts.length < 4) yield null;
                    try {
                        int index = Integer.parseInt(parts[3]);
                        List<String> list = NCoreMain.inst().configManager.getStringList(path);
                        if (list == null || list.isEmpty()) yield "List Not Found";
                        if (index < 0 || index >= list.size()) yield "Invalid Index";
                        yield list.get(index);
                    } catch (NumberFormatException e) {
                        yield "Invalid Index Format";
                    }
                }
                default -> null;
            };
        }

        if (params.startsWith("expiration_") || params.startsWith("owner_")) {
            String[] parts = params.split("_");
            if (parts.length < 3) return null;

            int chunkX = Integer.parseInt(parts[1]);
            int chunkZ = Integer.parseInt(parts[2]);

            String worldName = plugin.claimDataManager.getClaimWorld(chunkX, chunkZ);
            if (worldName == null) return "Unknown World";

            World world = Bukkit.getWorld(worldName);
            if (world == null) return "Unknown World";

            Chunk chunk = world.getChunkAt(chunkX, chunkZ);

            if (params.startsWith("expiration_")) {
                return plugin.claimDataManager.getExpiredDate(chunk);
            }
            if (params.startsWith("owner_")) {
                String ownerUUID = plugin.claimDataManager.getClaimOwner(chunk);
                if (ownerUUID == null) {
                    return "Owner is undefined";
                }

                OfflinePlayer claimOwner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
                return claimOwner.getName() != null ? claimOwner.getName() : "Unknown Owner";

            }
        }

        return null;
    }
}
