package nesoi.network.NClaim.model;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.Setter;
import nesoi.network.NClaim.Config;
import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.enums.Balance;
import nesoi.network.NClaim.utils.HoloManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.DAPI.Util;
import org.nandayo.DAPI.object.DParticle;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.nandayo.DAPI.HexUtil.parse;

@Getter
@Setter
public class Claim {

    private NCoreMain plugin;

    public Claim(
            @NotNull String claimId,
            @NotNull Chunk chunk,
            @NotNull Date createdAt,
            @NotNull Date expiredAt,
            @NotNull UUID owner,
            @NotNull Location bedrockLocation,
            Collection<String> lands,
            Collection<UUID> coopPlayers,
            HashMap<UUID, Date> coopPlayerJoinDate,
            HashMap<UUID, CoopPermission> coopPermissions,
            ClaimSetting settings
            ) {
        this.plugin = NCoreMain.inst();
        this.claimId = claimId;
        this.chunk = chunk;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        this.owner = owner;
        this.bedrockLocation = bedrockLocation;
        this.lands = lands;
        this.coopPlayers = coopPlayers;
        this.coopPlayerJoinDate = coopPlayerJoinDate;
        this.coopPermissions = coopPermissions;
        this.settings = settings;

        claims.add(this);
    }

    private final @NotNull String claimId;
    private final @NotNull Chunk chunk;
    private final @NotNull Date createdAt;
    private @NotNull Date expiredAt;
    private final @NotNull UUID owner;
    private final @NotNull Location bedrockLocation;
    private final Collection<String> lands;
    private final Collection<UUID> coopPlayers;
    private final HashMap<UUID, Date> coopPlayerJoinDate;

    private final HashMap<UUID, CoopPermission> coopPermissions;
    private final ClaimSetting settings;

    static public void buy(Player player) {
        Chunk chunk = player.getLocation().getChunk();

        User user = User.getUser(player.getUniqueId());

        if (user.getPlayerClaims().size() >= NCoreMain.inst().config.getMaxClaimCount(player)) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.reached-max-claim-count"));
            return;
        }

        if (getClaim(chunk) != null) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.already-claimed"));
            return;
        }

        double claimPrice = NCoreMain.inst().config.getClaimBuyPrice();

        if (!user.getPlayer().hasPermission("nclaim.bypass.*") && !user.getPlayer().hasPermission("nclaim.bypass.claim-buy-price")) {
            if (NCoreMain.inst().balanceSystem == Balance.PLAYERDATA) {
                if (user.getBalance() >= claimPrice) {
                    user.setBalance(user.getBalance() - claimPrice);
                } else {
                    player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.not-enough-money"));
                    return;
                }
            } else if (NCoreMain.inst().balanceSystem == Balance.VAULT) {
                Economy econ = NCoreMain.inst().getEconomy();
                if (econ != null && econ.has(player, claimPrice)) {
                    econ.withdrawPlayer(player, claimPrice);
                } else {
                    player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.not-enough-money"));
                    return;
                }
            }
        }

        String claimId = chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date createdAt = null;
        try {
            createdAt = dateFormat.parse(dateFormat.format(new Date()));
        } catch (ParseException e) {
            Util.log("Error occurred while parsing created-at: " + e.getMessage());
        }

        if (createdAt == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, NCoreMain.inst().configManager.getInt("claim-end-day", 7));
        Date expirationDate = calendar.getTime();

        UUID owner = player.getUniqueId();

        Location bedrockLocation = player.getLocation().getBlock().getLocation();
        bedrockLocation.getBlock().setType(Material.BEDROCK);

        Collection<String> lands = new ArrayList<>();
        Collection<UUID> coopPlayers = new ArrayList<>();
        HashMap<UUID, Date> coopPlayerJoinDate = new HashMap<>();
        HashMap<UUID, CoopPermission> coopPermissions = new HashMap<>();
        ClaimSetting setting = new ClaimSetting();

        Claim claim = new Claim(claimId, chunk, createdAt, expirationDate, owner, bedrockLocation, lands, coopPlayers, coopPlayerJoinDate, coopPermissions, setting);

        double x = bedrockLocation.getBlockX() + 0.5;
        double z = bedrockLocation.getBlockZ() + 0.5;
        double y;

        y = bedrockLocation.getBlockY() + 2.7;


        Location hologramLocation = new Location(bedrockLocation.getWorld(), x, y, z);

        HoloManager hologramManager = new HoloManager();
        hologramManager.createClaimHologram(player, hologramLocation);

        User.getUser(player.getUniqueId()).getPlayerClaims().add(claim);

        player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.success.claim-received"));
    }

    public Collection<Chunk> getAllChunks() {
        List<Chunk> chunks = new ArrayList<>();
        chunks.add(chunk);
        getLands().forEach(l -> chunks.add(NCoreMain.deserializeChunk(l)));
        return chunks;
    }


    public void addCoop(@NotNull Player coop) {
        Player owner = Bukkit.getPlayer(getOwner());
        Config config = NCoreMain.inst().config;
        if (owner == null) return;
        UUID coopUUID = coop.getUniqueId();

        if (getOwner().equals(coopUUID)) {
            owner.sendMessage(plugin.langManager.getMsg("messages.error.cant-add-yourself"));
            return;
        }

        if (!getOwner().equals(owner.getUniqueId())) {
            owner.sendMessage(plugin.langManager.getMsg("messages.error.claim-not-yours"));
            return;
        }

        if (getCoopPlayers().size() >= config.getMaxCoopPlayers()) {
            owner.sendMessage(plugin.langManager.getMsg("messages.error.coop-limit-reached", config.getMaxCoopPlayers()));
            return;
        }

        if (getCoopPlayers().contains(coopUUID)) {
            owner.sendMessage(plugin.langManager.getMsg("messages.error.player-already-coop"));
            return;
        }

        getCoopPlayers().add(coopUUID);
        getCoopPlayerJoinDate().put(coopUUID, new Date());
        getCoopPermissions().put(coopUUID, new CoopPermission());
        User.getUser(coopUUID).getCoopClaims().add(this);

        owner.sendMessage(plugin.langManager.getMsg("messages.success.coop-added", coop.getName()));
        coop.sendMessage(plugin.langManager.getMsg("messages.success.player-added-to-claim", owner.getName()));

    }

    public void kickCoop(@NotNull UUID coop) {
        Player owner = Bukkit.getPlayer(getOwner());
        OfflinePlayer coopPlayer = Bukkit.getOfflinePlayer(coop);
        if (owner == null) return;

        if (!getOwner().equals(owner.getUniqueId())) {
            owner.sendMessage(plugin.langManager.getMsg("messages.error.claim-not-yours"));
            return;
        }

        getCoopPlayers().remove(coop);
        getCoopPlayerJoinDate().remove(coop);
        getCoopPermissions().remove(coop);
        User.getUser(coop).getCoopClaims().remove(this);

        owner.sendMessage(plugin.langManager.getMsg("messages.success.coop-kicked", coopPlayer.getName()));
    }

    public void toggleCoopPermission(@NotNull UUID player, @NotNull CoopPermission.Permission coopPermission) {
        getCoopPermissions().get(player).toggle(coopPermission);
    }

    public boolean getCoopPermissionState(@NotNull UUID player, @NotNull CoopPermission.Permission permission) {
        return getCoopPermissions().get(player).isEnabled(permission);
    }

    public void toggleClaimSetting(@NotNull ClaimSetting.Setting setting) {
        getSettings().toggle(setting);
    }

    public boolean getSettingState(@NotNull ClaimSetting.Setting setting) {
        return getSettings().isEnabled(setting);
    }

    public void buyLand(Player player, @NotNull Chunk chunk) {
        if (getChunk().equals(chunk)) return;

        User user = User.getUser(player.getUniqueId());
        double landPrice = NCoreMain.inst().config.getEachLandBuyPrice();

        if (!user.getPlayer().hasPermission("nclaim.bypass.*") || !user.getPlayer().hasPermission("nclaim.bypass.land-buy-price")) {
            if (NCoreMain.inst().balanceSystem == Balance.PLAYERDATA) {
                if (user.getBalance() >= landPrice) {
                    user.setBalance(user.getBalance() - landPrice);
                } else {
                    player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.not-enough-money"));
                    return;
                }
            }else if (NCoreMain.inst().balanceSystem == Balance.VAULT) {
                Economy econ = NCoreMain.inst().getEconomy();
                if (econ != null && econ.has(player, landPrice)) {
                    econ.withdrawPlayer(player, landPrice);
                } else {
                    player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.not-enough-money"));
                    return;
                }
            }
        }

        String chunkKey = chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();

        if (lands.contains(chunkKey)) return;

        lands.add(chunkKey);

        player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.success.claim-expanded"));
    }

    public void remove() {
        getBedrockLocation().getBlock().setType(Material.AIR);
        Chunk chunk = getChunk();

        String fixedClaimKey = "claim_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();

        Hologram hologram = DHAPI.getHologram(fixedClaimKey);
        if (hologram != null)
            hologram.delete();

        int centerX = chunk.getX() * 16 + 8;
        int centerZ = chunk.getZ() * 16 + 8;

        Location bedrockLocation = getBedrockLocation();
        World world = getChunk().getWorld();

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.reminder.claim-expired", centerX, centerZ));

            double distance = player.getLocation().distance(bedrockLocation);
            float volume = (float) Math.max(0.2, 1 - (distance / 16.0));

            world.playSound(bedrockLocation, Sound.ENTITY_GENERIC_EXPLODE, volume, 1);
        });

        if (Bukkit.getPlayer(getOwner()) != null) {
            User.getUser(getOwner()).getPlayerClaims().remove(this);
        }

        for (UUID coop : getCoopPlayers()) {
            if (Bukkit.getPlayer(coop) != null) {
                User.getUser(coop).getCoopClaims().remove(this);
            }
        }

        getChunk().getWorld().spawnParticle(plugin.getParticle(DParticle.LARGE_SMOKE, DParticle.SMOKE_LARGE), getBedrockLocation(), 1);
        getChunk().getWorld().playSound(getBedrockLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

        claims.remove(this);
    }

    public void addExpirationDate(int days, int hours, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(expiredAt);

        calendar.add(Calendar.DAY_OF_MONTH, days);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        calendar.add(Calendar.MINUTE, minutes);

        expiredAt = calendar.getTime();

        if (expiredAt.before(new Date())) {
            remove();
        }
    }

    public String getFancyExpireDate() {
        long diffInMollies = expiredAt.getTime() - new Date().getTime();
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
    }

    @Getter
    static public Collection<Claim> claims = new ArrayList<>();

    static public Claim getClaim(@NotNull Chunk chunk) {
        return claims.stream()
                .filter(c -> c.getChunk().equals(chunk) || c.getLands().contains(chunk.getWorld().getName() +  "," + chunk.getX() + "," + chunk.getZ()))
                .findFirst().orElse(null);
    }

    static public void checkExpiredClaims() {
        for (Claim claim : new ArrayList<>(claims)) {
            Date currentDate = new Date();
            if (claim.getExpiredAt().before(currentDate)) {
                claim.remove();
            }
        }
    }

    static public synchronized void loadData() {
        claims.clear();
        File file = new File(NCoreMain.inst().getDataFolder(), "claims.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section =  config.getConfigurationSection("chunks_claimed");
        if (section == null) return;

        for (String claimId : section.getKeys(false)) {
            String[] chunkParts = claimId.split("_");
            if (chunkParts.length != 3) continue;

            World world = Bukkit.getWorld(chunkParts[0]);
            int x = Integer.parseInt(chunkParts[1]);
            int z = Integer.parseInt(chunkParts[2]);

            if (world == null) continue;

            Chunk chunk = world.getChunkAt(x,z);

            Date createdAt = NCoreMain.deserializeDate(section.getString(claimId + ".created_at"));
            if (createdAt == null) continue;
            Date expiredAt = NCoreMain.deserializeDate(section.getString(claimId + ".expired_at"));
            if (expiredAt == null) continue;
            UUID owner = UUID.fromString(section.getString(claimId + ".owner",""));
            Location bedrockLocation = NCoreMain.deserializeLocation(section.getString(claimId + ".bedrock_location"));
            if (bedrockLocation == null) continue;
            Collection<String> lands = section.getStringList(claimId + ".lands");

            Collection<UUID> coopPlayers = new ArrayList<>();
            HashMap<UUID, Date> coopPlayerJoinDates = new HashMap<>();
            HashMap<UUID, CoopPermission> coopPermissions = new HashMap<>();
            ConfigurationSection coopSection = section.getConfigurationSection(claimId + ".coops");
            if (coopSection != null) {
                for (String coopPlayer : coopSection.getKeys(false)) {
                    UUID coopPlayerUUID = UUID.fromString(coopPlayer);
                    coopPlayers.add(coopPlayerUUID);
                    Date coopPlayerJoinDate = NCoreMain.deserializeDate(coopSection.getString( coopPlayerUUID + ".joined_at"));
                    coopPlayerJoinDates.put(coopPlayerUUID, coopPlayerJoinDate);
                    CoopPermission permission = new CoopPermission();
                    ConfigurationSection permissionSection = coopSection.getConfigurationSection(coopPlayerUUID + ".permissions");
                    if (permissionSection != null) {
                        for (String permissionName : permissionSection.getKeys(false)) {
                            CoopPermission.Permission perm = CoopPermission.Permission.valueOf(permissionName);
                            permission.set(perm, permissionSection.getBoolean(permissionName));
                        }
                    }
                    coopPermissions.put(coopPlayerUUID, permission);

                    Util.log("UUID: " + coopPlayerUUID + ", joindate: " + coopPlayerJoinDate + ", permission: " + permission); //dene
                }
            }
            ClaimSetting settings = new ClaimSetting();
            ConfigurationSection claimSettingSection = section.getConfigurationSection(claimId + ".settings");
            if (claimSettingSection != null) {
                for (String settingName : claimSettingSection.getKeys(false)) {
                    ClaimSetting.Setting setting = ClaimSetting.Setting.valueOf(settingName.toUpperCase(Locale.ENGLISH).replace("-","_"));
                    settings.set(setting, claimSettingSection.getBoolean(settingName));
                }
            }

            new Claim(claimId, chunk, createdAt, expiredAt, owner, bedrockLocation, lands, coopPlayers, coopPlayerJoinDates, coopPermissions, settings);
        }
        Util.log("&eLoaded " + claims.size() +  " claims.");
    }

    static public synchronized void saveData() {
        FileConfiguration config = new YamlConfiguration();

        for (Claim claim : claims) {
            String ns = "chunks_claimed." + claim.claimId;
            config.set(ns + ".created_at", NCoreMain.serializeDate(claim.createdAt));
            config.set(ns + ".expired_at", NCoreMain.serializeDate(claim.expiredAt));
            config.set(ns + ".owner", claim.owner.toString());
            config.set(ns + ".bedrock_location", NCoreMain.serializeLocation(claim.bedrockLocation));
            config.set(ns + ".lands", claim.lands);
            for (UUID coopPlayerUUID : claim.coopPlayers) {
                config.set(ns + ".coops." + coopPlayerUUID.toString() + ".joined_at", NCoreMain.serializeDate(claim.coopPlayerJoinDate.get(coopPlayerUUID)));
                CoopPermission coopPermission = claim.coopPermissions.getOrDefault(coopPlayerUUID, new CoopPermission());
                for (CoopPermission.Permission permission : CoopPermission.Permission.values()) {
                    config.set(ns + ".coops." + coopPlayerUUID.toString() + ".permissions." + permission.toString(), coopPermission.isEnabled(permission));
                }
            }
            ClaimSetting settings = claim.settings;
            for (ClaimSetting.Setting setting : ClaimSetting.Setting.values()) {
                config.set(ns + ".settings." + setting, settings.isEnabled(setting));
            }
        }

        try {
            File file = new File(NCoreMain.inst().getDataFolder(), "claims.yml");
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
