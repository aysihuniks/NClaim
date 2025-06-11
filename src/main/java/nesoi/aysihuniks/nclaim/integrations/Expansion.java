package nesoi.network.NClaim.integrations;


import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.model.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class Expension extends PlaceholderExpansion {

    private final NCoreMain plugin;

    public Expension(NCoreMain plugin) {
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
            return "";
        }

        if (params.startsWith("get_")) {
            String[] parts = params.split("_");
            if (parts.length < 3) return null;

            String data = parts[1];
            String path = parts[2];

            switch (data) {
                case "string":
                    return NCoreMain.inst().configManager.getString(path, "Null");
                case "int":
                    return String.valueOf(NCoreMain.inst().configManager.getInt(path, 0));
                case "boolean":
                    return String.valueOf(NCoreMain.inst().configManager.getBoolean(path, false));
                case "list":
                    if (parts.length < 4) return null;
                    try {
                        int index = Integer.parseInt(parts[3]);
                        List<String> list = NCoreMain.inst().configManager.getStringList(path);
                        if (list == null || list.isEmpty()) return "List Not Found";
                        if (index < 0 || index >= list.size()) return "Invalid Index";
                        return list.get(index);
                    } catch (NumberFormatException e) {
                        return "Invalid Index Format";
                    }
                default:
                    return null;
            }

        }

        if (params.startsWith("expiration_") || params.startsWith("owner_")) {
            String[] parts = params.split("_");
            if (parts.length < 4) return null;

            String worldName = parts[1];
            int chunkX, chunkZ;

            try {
                chunkX = Integer.parseInt(parts[2]);
                chunkZ = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                return "Error occurred while trying to parsing coordinates";
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) return "World is not found";

            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            Claim claim = Claim.getClaim(chunk);

            if (claim == null) return "Claim not found";

            if (params.startsWith("expiration_")) {
                return claim.getFancyExpireDate();
            }

            if (params.startsWith("owner_")) {
                UUID ownerUUID = claim.getOwner();

                OfflinePlayer claimOwner = Bukkit.getOfflinePlayer(ownerUUID);
                return claimOwner.getName() != null ? claimOwner.getName() : "Owner is not found";
            }
        }

        return null;
    }
}
