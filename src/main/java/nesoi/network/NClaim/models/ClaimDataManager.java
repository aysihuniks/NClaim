package nesoi.network.NClaim.models;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.hologram.Hologram;
import eu.decentsoftware.holograms.api.DHAPI;
import nesoi.network.NClaim.Config;
import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.enums.Balance;
import nesoi.network.NClaim.enums.Holo;
import nesoi.network.NClaim.menus.ConfirmMenu;
import nesoi.network.NClaim.utils.HoloManager;
import nesoi.network.NClaim.utils.LangManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static nesoi.network.NClaim.NCoreMain.economy;
import static org.nandayo.DAPI.HexUtil.parse;

public class ClaimDataManager {

    private static final Logger LOGGER = Logger.getLogger(ClaimDataManager.class.getName());

    private final File claimsFile;
    private final FileConfiguration claimsConfig;

    public ClaimDataManager() {
        this.claimsFile = new File(NCoreMain.inst().getDataFolder(), "claims.yml");
        this.claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);

        if (!claimsFile.exists()) {
            saveClaimsData();
        }
    }

    public void checkClaim(Player p) {

        PlayerDataManager playerDataManager = NCoreMain.pdCache.get(p);
        int chunkX = p.getLocation().getChunk().getX();
        int chunkZ = p.getLocation().getChunk().getZ();
        World world = p.getLocation().getWorld();

        if (getPlayerClaimCount(playerDataManager) >= NCoreMain.inst().configManager.getInt("max-claim-count", 3)) {
            p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.reached-max-claim-count"));
            return;
        }

        if (getClaimOwner(p.getLocation().getChunk()) != null) {
            p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.chunk-already-claimed"));
            return;
        }

        if (world == null) {
            return;
        }

        if (NCoreMain.inst().configManager.getStringList("blacklisted-worlds").contains(world.getName())) {
            p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.you-are-in-blacklisted-world"));
            return;
        }

        Map<String, Object> claimData = new HashMap<>();
        claimData.put("chunkX", chunkX);
        claimData.put("chunkZ", chunkZ);

        Consumer<String> onFinish = (result) -> {
            if ("confirmed".equals(result)) {
                addClaim(playerDataManager, claimData);
                p.closeInventory();
            } else if ("declined".equals(result)) {
                p.closeInventory();
            }
        };

        new ConfirmMenu(p, "Buy a Claim", Arrays.asList("", "{GRAY}If you approve this action,", "{WHITE}" + NCoreMain.inst().configManager.getInt("claim-buy-price", 1500) + "$ will be {WHITE}removed {GRAY}from", "{GRAY}your {WHITE}balance {GRAY}and you will", "{GRAY}buy a {WHITE}new claim{GRAY}."), onFinish);
    }

    private int getPlayerClaimCount(PlayerDataManager playerDataManager) {
        UUID playerUUID = playerDataManager.getPlayer().getUniqueId();
        ConfigurationSection claimedChunksSection = claimsConfig.getConfigurationSection("chunks_claimed");

        if (claimedChunksSection == null) {
            return 0;
        }

        return (int) claimedChunksSection
                .getKeys(false).stream()
                .filter(key -> {
                    String ownerUUID = claimsConfig.getString("chunks_claimed." + key + ".owner");
                    return ownerUUID != null && ownerUUID.equals(playerUUID.toString());
                })
                .count();
    }

    public void addClaim(PlayerDataManager playerDataManager, Map<String, Object> claimData) {
        try {
            if (playerDataManager == null) {
                NCoreMain.inst().getLogger().warning("(Method: addClaim - PlayerDataManager) Some error occurred! Contact with us: @aysihuniks");
                return;
            }
            if (claimData == null) {
                NCoreMain.inst().getLogger().warning("(Method: addClaim - ClaimData) Some error occurred! Contact with us: @aysihuniks");
                return;
            }


            Integer chunkX = (Integer) claimData.get("chunkX");
            Integer chunkZ = (Integer) claimData.get("chunkZ");
            if (chunkX == null || chunkZ == null) {
                return;
            }

            Player player = playerDataManager.getPlayer();
            int claimBuyPrice = NCoreMain.inst().configManager.getInt("claim-buy-price", 1500);

            double playerBalance;

            if (NCoreMain.inst().balanceSystem == Balance.VAULT) {
                playerBalance = economy.getBalance(player);
            } else {
                playerBalance = playerDataManager.getBalance();
            }

            if (playerBalance < claimBuyPrice) {
                player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.claim-insufficient-balance", playerBalance, claimBuyPrice));
                return;
            }

            if (NCoreMain.inst().balanceSystem == Balance.VAULT) {
                economy.withdrawPlayer(player, claimBuyPrice);
            } else {
                playerDataManager.setBalance(playerBalance - claimBuyPrice);
            }



            Location location = player.getLocation();
            location.getBlock().setType(Material.BEDROCK);

            World world = location.getWorld();
            if (world == null) {
                return;
            }

            int x = chunkX;
            int z = chunkZ;
            UUID uuid = playerDataManager.getPlayer().getUniqueId();

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String formattedDate = dateFormat.format(new Date());

            String claimPath = "chunks_claimed." + x + "_" + z;
            claimsConfig.set(claimPath + ".created-at", formattedDate);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, NCoreMain.inst().configManager.getInt("claim-end-day", 7));
            String expirationDate = dateFormat.format(calendar.getTime());
            claimsConfig.set(claimPath + ".expired-at", expirationDate);

            claimsConfig.set(claimPath + ".owner", uuid.toString());

            claimsConfig.set(claimPath + "." + "settings.claim-pvp", false);
            claimsConfig.set(claimPath + "." + "settings.tnt-damage", true);
            claimsConfig.set(claimPath + "." + "settings.creeper-pvp", true);
            claimsConfig.set(claimPath + "." + "settings.mob-attacking", false);
            claimsConfig.set(claimPath + "." + "settings.monsters-spawning", true);
            claimsConfig.set(claimPath + "." + "settings.animals-spawning", true);
            claimsConfig.set(claimPath + "." + "settings.villager-interacting", false);
            claimsConfig.set(claimPath + ".coops", null);

            saveClaimsData();

            claimsConfig.set(claimPath + ".bedrock_location.world", world.getName());
            claimsConfig.set(claimPath + ".bedrock_location.x", location.getBlockX());
            claimsConfig.set(claimPath + ".bedrock_location.y", location.getBlockY());
            claimsConfig.set(claimPath + ".bedrock_location.z", location.getBlockZ());
            saveClaimsData();

            double centerX = location.getBlockX() + 0.5;
            double centerZ = location.getBlockZ() + 0.5;
            double bedrockY;
            if (NCoreMain.inst().hologramSystem == Holo.FANCY_HOLOGRAM) {
                bedrockY = location.getBlockY() + 1.3;
            } else {
                bedrockY = location.getBlockY() + 2.7;
            }

            Location hologramLocation = new Location(location.getWorld(), centerX, bedrockY, centerZ);

            HoloManager hologramManager = new HoloManager();
            hologramManager.createClaimHologram(player, hologramLocation);

            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.claim-successfully-bought"));
        } catch (Exception e) {
            NCoreMain.inst().getLogger().severe("(Method: addClaim - Catch) Some error occurred! Contact with us: @aysihuniks " + e.getMessage());
        }
    }

    public boolean isPlayerUnAllowed(Player player, Chunk chunk, String permissionType) {
        String chunkKey = chunk.getX() + "_" + chunk.getZ();
        String basePath = "chunks_claimed." + chunkKey;

        if (!claimsConfig.contains(basePath)) {
            for (String key : claimsConfig.getConfigurationSection("chunks_claimed").getKeys(false)) {
                List<String> lands = claimsConfig.getStringList("chunks_claimed." + key + ".lands");
                if (lands.contains(chunkKey)) {
                    basePath = "chunks_claimed." + key;
                    break;
                }
            }
        }

        String ownerID = claimsConfig.getString(basePath + ".owner", null);
        if (ownerID == null || ownerID.isEmpty()) {
            return false;
        }

        UUID ownerUUID = UUID.fromString(ownerID);
        if (player.getUniqueId().equals(ownerUUID)) {
            return false;
        }

        String coopPath = basePath + ".coops." + player.getUniqueId();
        return !claimsConfig.getBoolean(coopPath + ".permissions." + permissionType, false);
    }

    private Location getBedrockLocation(String claimKey) {
        String path = "chunks_claimed." + claimKey + ".bedrock_location";

        if (!claimsConfig.contains(path)) return null;

        String worldName = claimsConfig.getString(path + ".world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = claimsConfig.getDouble(path + ".x");
        double y = claimsConfig.getDouble(path + ".y");
        double z = claimsConfig.getDouble(path + ".z");

        return new Location(world, x, y, z);
    }


    public void removeClaim(String claimKey) {
        Location bedrockLocation = getBedrockLocation(claimKey);
        if (bedrockLocation != null) {
            bedrockLocation.getBlock().setType(Material.AIR);
        }

        claimsConfig.set("chunks_claimed." + claimKey, null);
        saveClaimsData();

        String[] parts = claimKey.split("_");
        if (parts.length != 2) return;

        int chunkX = Integer.parseInt(parts[0]);
        int chunkZ = Integer.parseInt(parts[1]);

        String fixedClaimKey = "claim_" + chunkX + "_" + chunkZ;

        if (NCoreMain.inst().hologramSystem == Holo.FANCY_HOLOGRAM) {
            HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
            Hologram hologram = manager.getHologram(fixedClaimKey).orElse(null);
            if (hologram != null)
                manager.removeHologram(hologram);
        } else if (NCoreMain.inst().hologramSystem == Holo.DECENT_HOLOGRAM) {
            eu.decentsoftware.holograms.api.holograms.Hologram hologram = DHAPI.getHologram(fixedClaimKey);
            if (hologram != null)
                hologram.delete();
        }


        int centerX = chunkX * 16 + 8;
        int centerZ = chunkZ * 16 + 8;

        Bukkit.getOnlinePlayers().forEach(player ->
                player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.claim-expired", centerX, centerZ))
        );
    }


    public void removeClaim(Player p) {
        Chunk chunk = p.getLocation().getChunk();
        if (isUnClaimed(chunk)) {
            p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.claim-not-found"));
            return;
        }

        int chunkX = p.getLocation().getChunk().getX();
        int chunkZ = p.getLocation().getChunk().getZ();
        String claimKey = chunkX + "_" + chunkZ;
        String fixedClaimKey = "claim_" + chunkX + "_" + chunkZ;

        if (NCoreMain.inst().hologramSystem == Holo.FANCY_HOLOGRAM) {
            HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
            Hologram hologram = manager.getHologram(fixedClaimKey).orElse(null);
            if (hologram != null)
                manager.removeHologram(hologram);
        } else if (NCoreMain.inst().hologramSystem == Holo.DECENT_HOLOGRAM) {
            eu.decentsoftware.holograms.api.holograms.Hologram hologram = DHAPI.getHologram(fixedClaimKey);
            if (hologram != null)
                hologram.delete();
        }

        Location bedrockLocation = getBedrockLocation(claimKey);
        if (bedrockLocation != null) {
            bedrockLocation.getBlock().setType(Material.AIR);
        }

        p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.claim-removed"));

        claimsConfig.set("chunks_claimed." + claimKey, null);
        saveClaimsData();
    }

    public String getClaimCoords(String claimKey) {
        String[] parts = claimKey.split("_");

        int chunkX = Integer.parseInt(parts[0]);
        int chunkZ = Integer.parseInt(parts[1]);

        int centerX = chunkX * 16 + 8;
        int centerZ = chunkZ * 16 + 8;

        return centerX + ", " + centerZ;
    }

    // Date

    public void extendExpirationDate(Player p,Chunk chunk, int days, int hours, int minutes) {
        try {
            String claimKey = chunk.getX() + "_" + chunk.getZ();
            String expiredAtString = claimsConfig.getString("chunks_claimed." + claimKey + ".expired-at");

            if (expiredAtString == null) {
                Bukkit.getLogger().warning(NCoreMain.inst().langManager.getMsg("messages.claim-not-found"));
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date expirationDate = dateFormat.parse(expiredAtString);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(expirationDate);

            calendar.add(Calendar.DAY_OF_MONTH, days);
            calendar.add(Calendar.HOUR_OF_DAY, hours);
            calendar.add(Calendar.MINUTE, minutes);

            Date newExpirationDate = calendar.getTime();

            String newExpirationDateString = dateFormat.format(newExpirationDate);
            claimsConfig.set("chunks_claimed." + claimKey + ".expired-at", newExpirationDateString);
            p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.expiration-date-successfully-extended", List.of(days, hours, minutes)));

            saveClaimsData();

        } catch (ParseException e) {
            Bukkit.getLogger().warning("(Method: addExpirationDate) Some error occurred! Contact with us: @aysihuniks " + e.getMessage());
        }
    }

    public void subtractExpirationDate(Player p, int days, int hours, int minutes) {
        try {
            Chunk chunk = p.getLocation().getChunk();
            String claimKey = chunk.getX() + "_" + chunk.getZ();
            String expiredAtString = claimsConfig.getString("chunks_claimed." + claimKey + ".expired-at");

            if (isUnClaimed(chunk)) {
                p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.claim-not-found"));
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date expirationDate = dateFormat.parse(expiredAtString);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(expirationDate);

            calendar.add(Calendar.DAY_OF_MONTH, -days);
            calendar.add(Calendar.HOUR_OF_DAY, -hours);
            calendar.add(Calendar.MINUTE, -minutes);

            Date newExpirationDate = calendar.getTime();
            String newExpirationDateString = dateFormat.format(newExpirationDate);
            claimsConfig.set("chunks_claimed." + claimKey + ".expired-at", newExpirationDateString);

            if (newExpirationDate.getTime() < System.currentTimeMillis()) {
                removeClaim(claimKey);
            }

            saveClaimsData();
        } catch (ParseException e) {
            Bukkit.getLogger().warning("(Method: subtractExpirationDate) Some error occurred! Contact with us: @aysihuniks " + e.getMessage());
        }
    }

    public void checkExpiredClaims() {
        ConfigurationSection claimedChunksSection = claimsConfig.getConfigurationSection("chunks_claimed");

        if (claimedChunksSection == null){
            return;
        }

        for (String claimKey : claimedChunksSection.getKeys(false)) {
            String expiredAt = claimsConfig.getString("chunks_claimed." + claimKey + ".expired-at");

            if (expiredAt != null) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    Date expirationDate = dateFormat.parse(expiredAt);
                    Date currentDate = new Date();

                    if (expirationDate.before(currentDate)) {
                        removeClaim(claimKey);
                    }
                } catch (ParseException e) {
                    LOGGER.severe("A error occurred! " + e.getMessage());
                }
            }
        }
    }

    // Getters


    public List<String> getPlayerClaims(UUID playerUUID) {
        List<String> playerClaims = new ArrayList<>();
        ConfigurationSection chunksClaimedSection = claimsConfig.getConfigurationSection("chunks_claimed");

        if (chunksClaimedSection != null) {
            for (String claimKey : chunksClaimedSection.getKeys(false)) {
                String ownerID = claimsConfig.getString("chunks_claimed." + claimKey + ".owner");
                if (ownerID != null && ownerID.equals(playerUUID.toString())) {
                    playerClaims.add(claimKey);
                }
            }
        }
        return playerClaims;
    }

    public List<String> getCoopClaims(UUID playerUUID) {
        List<String> coopClaims = new ArrayList<>();
        ConfigurationSection chunksClaimedSection = claimsConfig.getConfigurationSection("chunks_claimed");

        if (chunksClaimedSection != null) {
            for (String claimKey : chunksClaimedSection.getKeys(false)) {
                ConfigurationSection coopsSection = chunksClaimedSection.getConfigurationSection(claimKey + ".coops");
                if (coopsSection == null) continue;
                if(coopsSection.getKeys(false).contains(playerUUID.toString())) {
                    coopClaims.add(claimKey);
                }
            }
        }

        return coopClaims;
    }

    public boolean isCoopMember(Chunk chunk, UUID playerUUID) {
        String chunkKey = chunk.getX() + "_" + chunk.getZ();
        String basePath = "chunks_claimed." + chunkKey;

        if (!claimsConfig.contains(basePath)) {
            for (String key : claimsConfig.getConfigurationSection("chunks_claimed").getKeys(false)) {
                List<String> lands = claimsConfig.getStringList("chunks_claimed." + key + ".lands");

                if (lands.contains(chunkKey)) {
                    basePath = "chunks_claimed." + key;
                    break;
                }
            }
        }

        String coopPath = basePath + ".coops";
        if (!claimsConfig.contains(coopPath)) {
            return false;
        }

        return claimsConfig.getConfigurationSection(coopPath).getKeys(false).contains(playerUUID.toString());
    }

    public String getClaimWorld(int chunkX, int chunkZ) {
        String chunkKey = chunkX + "_" + chunkZ;
        if (!claimsConfig.contains("chunks_claimed." + chunkKey + ".bedrock_location.world")) {
            return null;
        }
        return claimsConfig.getString("chunks_claimed." + chunkKey + ".bedrock_location.world");
    }

    public String getExpiredDate(Chunk chunk) {
        String claimPath = "chunks_claimed." + chunk.getX() + "_" + chunk.getZ();
        String expiredAt = claimsConfig.getString(claimPath + ".expired-at", null);

        if (expiredAt == null) {
            return "No expiration date set.";
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date expirationDate = dateFormat.parse(expiredAt);
            Date currentDate = new Date();

            long diffInMollies = expirationDate.getTime() - currentDate.getTime();
            long diffDays = TimeUnit.DAYS.convert(diffInMollies, TimeUnit.MILLISECONDS);
            long diffHours = TimeUnit.HOURS.convert(diffInMollies, TimeUnit.MILLISECONDS) % 24;
            long diffMinutes = TimeUnit.MINUTES.convert(diffInMollies, TimeUnit.MILLISECONDS) % 60;
            long diffSeconds = TimeUnit.SECONDS.convert(diffInMollies, TimeUnit.MILLISECONDS) % 60;

            String timeLeft;
            if (diffDays > 0) {
                timeLeft = String.format("%dd, %dh", diffDays, diffHours);
            } else if (diffHours > 0) {
                timeLeft = String.format("%dh, %dm", diffHours, diffMinutes);
            } else if (diffMinutes > 0) {
                timeLeft = String.format("%dm, %ds", diffMinutes, diffSeconds);
            } else {
                timeLeft = String.format("%ds", diffSeconds);
            }

            String color;
            if (diffDays >= 2) {
                color = "{GREEN}";
            } else if (diffDays >= 1) {
                color = "{YELLOW}";
            } else {
                color = "{RED}";
            }

            return parse(color + timeLeft);
        } catch (ParseException e) {
            LOGGER.severe("A error occurred! " + e.getMessage());
            return "Error parsing expiration date.";
        }
    }

    public String getClaimOwner(Chunk chunk) {
        String chunkKey = chunk.getX() + "_" + chunk.getZ();
        String ownerID = claimsConfig.getString("chunks_claimed." + chunkKey + ".owner");

        if (ownerID == null) {
            ConfigurationSection chunksClaimedSection = claimsConfig.getConfigurationSection("chunks_claimed");
            if (chunksClaimedSection != null) {
                for (String key : chunksClaimedSection.getKeys(false)) {
                    List<String> lands = claimsConfig.getStringList("chunks_claimed." + key + ".lands");
                    if (lands.contains(chunkKey)) {
                        ownerID = claimsConfig.getString("chunks_claimed." + key + ".owner");
                        break;
                    }
                }
            }
        }

        return ownerID;
    }

    public void addLandToClaim(Chunk mainChunk, Chunk newLand, PlayerDataManager playerDataManager) {
        Player player = playerDataManager.getPlayer();
        int newLandBalance = NCoreMain.inst().configManager.getInt("claim-each-land-price", 2000);

        if (!isUnClaimed(newLand)) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.chunk-already-claimed"));
            return;
        }

        int requiredBalance = NCoreMain.inst().configManager.getInt("claim-each-land-price", 2000);


        if (NCoreMain.inst().balanceSystem == Balance.VAULT) {
            double playerBalance = economy.getBalance(player);
            if (playerBalance < newLandBalance) {
                player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.land-insufficient-balance", playerBalance, requiredBalance));
                return;
            } else {
                economy.withdrawPlayer(player, newLandBalance);
            }
        } else {
            double playerBalance = playerDataManager.getBalance();
            if (playerBalance < newLandBalance) {
                player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.land-insufficient-balance", playerBalance, requiredBalance));
                return;
            } else {
                playerDataManager.setBalance(playerBalance - newLandBalance);
            }

        }

        String mainChunkKey = mainChunk.getX() + "_" + mainChunk.getZ();
        String newLandKey = newLand.getX() + "_" + newLand.getZ();

        List<String> lands = claimsConfig.getStringList("chunks_claimed." + mainChunkKey + ".lands");
        if (!lands.contains(newLandKey)) {
            lands.add(newLandKey);
            claimsConfig.set("chunks_claimed." + mainChunkKey + ".lands", lands);
            saveClaimsData();
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.claim-expanded"));
        }
    }

    public boolean isUnClaimed(Chunk chunk) {
        String ownerID = getClaimOwner(chunk);
        return ownerID == null;
    }

    public boolean isChunkClaimedByAnotherPlayer(Chunk chunk, String playerUUID) {
        String ownerID = getClaimOwner(chunk);
        return ownerID != null && !ownerID.equals(playerUUID);
    }

    public List<String> getLands(Chunk chunk) {
        String chunkKey = chunk.getX() + "_" + chunk.getZ();
        return claimsConfig.getStringList("chunks_claimed." + chunkKey + ".lands");
    }

    public String getCenterChunk(Chunk newChunk) {
        String chunkKey = newChunk.getX() + "_" + newChunk.getZ();
        ConfigurationSection section = claimsConfig.getConfigurationSection("chunks_claimed");
        if(section != null) {
            if(section.getKeys(false).contains(chunkKey)) return chunkKey;
            return section.getKeys(false).stream()
                    .filter(key -> section.getStringList(key + ".lands").contains(chunkKey))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public String getCoopJoinedDate(String claimKey, UUID playerUUID) {
        String path = "chunks_claimed." + claimKey + ".coops." + playerUUID + ".joined-at";

        String joinedDate = claimsConfig.getString(path);

        if (joinedDate == null) {
            return "No date available";
        }

        return joinedDate;
    }

    public Location getClaimBedrockLocation(String claimKey) {
        int chunkX = claimsConfig.getInt("chunks_claimed." + claimKey + ".bedrock_location.x");
        int chunkY = claimsConfig.getInt("chunks_claimed." + claimKey + ".bedrock_location.y");
        int chunkZ = claimsConfig.getInt("chunks_claimed." + claimKey + ".bedrock_location.z");
        String worldName = claimsConfig.getString("chunks_claimed." + claimKey + ".bedrock_location.world");

        assert worldName != null;
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return new Location(world, chunkX, chunkY, chunkZ);
        }
        return null;
    }

    public boolean isBedrockLocation(Location location) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        World world = location.getWorld();

        if (world == null) {
            return false;
        }
        String worldName = world.getName();
        ConfigurationSection chunkSection = claimsConfig.getConfigurationSection("chunks_claimed");
        if (chunkSection == null) {
            return false;
        }

        for (String key : chunkSection.getKeys(false)) {
            String path = "chunks_claimed." + key + ".bedrock_location";
            String bedrockWorld = claimsConfig.getString(path + ".world");
            int bedrockX = claimsConfig.getInt(path + ".x");
            int bedrockY = claimsConfig.getInt(path + ".y");
            int bedrockZ = claimsConfig.getInt(path + ".z");

            if (worldName.equals(bedrockWorld) && x == bedrockX && y == bedrockY && z == bedrockZ) {
                return true;
            }
        }
        return false;
    }

    /* Coop Actions */

    public void addCoopPlayer(Player owner, Player coopPlayer, Chunk chunk) {
        String ownerID = getClaimOwner(chunk);
        Config config = NCoreMain.inst().config;
        LangManager langManager = NCoreMain.inst().langManager;

        if (ownerID == null) {
            owner.sendMessage(langManager.getMsg("messages.claim-not-found"));
            return;
        }

        if (coopPlayer == null) {
            owner.sendMessage(langManager.getMsg("messages.player-not-found"));
            return;
        }

        if (!owner.getUniqueId().toString().equals(ownerID)) {
            owner.sendMessage(langManager.getMsg("messages.claim-is-not-yours"));
            return;
        }

        if (owner.getUniqueId().equals(coopPlayer.getUniqueId())) {
            owner.sendMessage(langManager.getMsg("messages.cannot-add-yourself"));
            return;
        }

        String claimPath = "chunks_claimed." + chunk.getX() + "_" + chunk.getZ();
        String coopPath = claimPath + ".coops";

        if (claimsConfig.contains(coopPath + "." + coopPlayer.getUniqueId())) {
            owner.sendMessage(langManager.getMsg("messages.coop-player-already-added", coopPlayer.getName()));
            return;
        }

        ConfigurationSection coopSection = claimsConfig.getConfigurationSection(coopPath);
        int currentCoopCount = (coopSection != null) ? coopSection.getKeys(false).size() : 0;

        int maxCoopPlayers = NCoreMain.inst().configManager.getInt("max-coop-limit", 7);
        if (currentCoopCount >= maxCoopPlayers) {
            owner.sendMessage(langManager.getMsg("messages.coop-limit-reached", String.valueOf(maxCoopPlayers)));
            return;
        }


        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String formattedDate = dateFormat.format(new Date());

        claimsConfig.set(coopPath + "." + coopPlayer.getUniqueId() + ".joined-at", formattedDate);
        claimsConfig.set(coopPath + "." + coopPlayer.getUniqueId() + ".permissions.can-break-spawners", false);
        claimsConfig.set(coopPath + "." + coopPlayer.getUniqueId() + ".permissions.can-place-spawners", false);
        claimsConfig.set(coopPath + "." + coopPlayer.getUniqueId() + ".permissions.can-cast-water-or-lava", false);
        claimsConfig.set(coopPath + "." + coopPlayer.getUniqueId() + ".permissions.can-interact-with-claim-bedrock", false);
        claimsConfig.set(coopPath + "." + coopPlayer.getUniqueId() + ".permissions.can-break-blocks", false);
        claimsConfig.set(coopPath + "." + coopPlayer.getUniqueId() + ".permissions.can-place-blocks", false);
        claimsConfig.set(coopPath + "." + coopPlayer.getUniqueId() + ".permissions.can-interact-with-chests", false);
        claimsConfig.set(coopPath + "." + coopPlayer.getUniqueId() + ".permissions.can-interact-with-buttons-doors-pressure-plates", false);
        saveClaimsData();

        owner.sendMessage(langManager.getMsg("messages.coop-player-successfully-added", coopPlayer.getName()));
        coopPlayer.sendMessage(langManager.getMsg("messages.you-been-added-to-claim", owner.getName()));
    }

    public void kickCoopPlayer(Player owner, Player coopPlayer, Chunk chunk) {
        String ownerID = getClaimOwner(chunk);
        Config config = NCoreMain.inst().config;
        LangManager langManager = NCoreMain.inst().langManager;

        if (ownerID == null) {
            owner.sendMessage(langManager.getMsg("messages.claim-not-found"));
            return;
        }

        if (!owner.getUniqueId().toString().equals(ownerID)) {
            owner.sendMessage(langManager.getMsg("messages.claim-is-not-yourself"));
            return;
        }

        String claimPath = "chunks_claimed." + chunk.getX() + "_" + chunk.getZ();
        String coopPath = claimPath + ".coops." + coopPlayer.getUniqueId();

        if (claimsConfig.contains(coopPath)) {
            claimsConfig.set(coopPath, null);
            saveClaimsData();
            owner.sendMessage(langManager.getMsg("messages.coop-player-successfully-kicked", coopPlayer.getName()));
        } else {
            owner.sendMessage(langManager.getMsg("messages.coop-player-already-not-in-coop", coopPlayer.getName()));
        }
    }

    public List<OfflinePlayer> getCoopPlayers(Chunk mainChunk) {
        List<OfflinePlayer> coopPlayers = new ArrayList<>();

        String mainClaimPath = "chunks_claimed." + mainChunk.getX() + "_" + mainChunk.getZ() + ".coops";
        ConfigurationSection coopSection = claimsConfig.getConfigurationSection(mainClaimPath);
        if (coopSection == null) {
            return List.of();
        }
        if (claimsConfig.contains(mainClaimPath)) {
            for (String key : coopSection.getKeys(false)) {
                OfflinePlayer coopPlayer = Bukkit.getOfflinePlayer(UUID.fromString(key));
                coopPlayers.add(coopPlayer);
            }
        }

        return coopPlayers;
    }

    public void toggleCoopPlayerPermission(Chunk chunk, String playerUUID, String perm) {
        String claimPath = "chunks_claimed." + chunk.getX() + "_" + chunk.getZ();
        String coopPath = claimPath + ".coops." + playerUUID + ".permissions." + perm;
        boolean currentStatus = claimsConfig.getBoolean(coopPath, false);
        claimsConfig.set(coopPath, !currentStatus);

        saveClaimsData();
    }

    public boolean getCoopPlayerPermission(Chunk chunk, String playerUUID, String perm) {
        String claimPath = "chunks_claimed." + chunk.getX() + "_" + chunk.getZ();
        String coopPath = claimPath + ".coops." + playerUUID + ".permissions." + perm;
        return claimsConfig.getBoolean(coopPath, false);
    }

    /* Settings */
    public Boolean isClaimSettingEnabled(Chunk chunk, String setting, boolean defaultValue) {
        String chunkKey = chunk.getX() + "_" + chunk.getZ();

        if (claimsConfig.contains("chunks_claimed." + chunkKey + ".settings." + setting)) {
            return claimsConfig.getBoolean("chunks_claimed." + chunkKey + ".settings." + setting);
        }

        for (String mainClaimKey : claimsConfig.getConfigurationSection("chunks_claimed").getKeys(false)) {
            List<String> lands = claimsConfig.getStringList("chunks_claimed." + mainClaimKey + ".lands");
            if (lands.contains(chunkKey) && claimsConfig.contains("chunks_claimed." + mainClaimKey + ".settings." + setting)) {
                return claimsConfig.getBoolean("chunks_claimed." + mainClaimKey + ".settings." + setting);
            }
        }

        return defaultValue;
    }

    public void toggleClaimSetting(Chunk chunk, String setting) {
        String chunkKey = chunk.getX() + "_" + chunk.getZ();

        boolean currentValue = claimsConfig.getBoolean("chunks_claimed." + chunkKey + ".settings." + setting, false);
        claimsConfig.set("chunks_claimed." + chunkKey + ".settings." + setting, !currentValue);

        saveClaimsData();
    }

    /* Default */

    public void saveAllChanges() {
        saveClaimsData();
    }

    private void saveClaimsData() {
        try {
            claimsConfig.save(claimsFile);
        } catch (IOException e) {
            NCoreMain.inst().getLogger().warning("Could not save claims data.");
        }
    }
}
