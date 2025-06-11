package nesoi.network.NClaim;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.nandayo.DAPI.Util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@Setter
public class Config {

    public FileConfiguration get() {
        return config;
    }

    private final NCoreMain plugin;

    public Config(NCoreMain plugin) {
        this.plugin = plugin;
    }

    public double claimBuyPrice;
    public double eachLandBuyPrice;
    public int claimExpiryDays;
    public int maxCoopPlayers;
    public int maxClaimCount;

    public synchronized Config load() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        setMaxClaimCount(config.getInt("max_claim_count", 3));
        setClaimBuyPrice(config.getDouble("claim_buy_price", 1500));
        setClaimExpiryDays(config.getInt("claim_expiry_days", 7));
        setEachLandBuyPrice(config.getDouble("each_land_buy_price", 2000));
        setMaxCoopPlayers(config.getInt("max_coop_players", 4));

        return this;
    }

    public int getMaxClaimCount(Player player) {
        if (player.isOp() || player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.max-claim-count")) {
            return Integer.MAX_VALUE;
        }

        int maxClaims = config.getInt("max_claim_count", 3);

        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String perm = permInfo.getPermission();
            if (perm.startsWith("nclaim.maxclaim.")) {
                try {
                    int value = Integer.parseInt(perm.substring("nclaim.maxclaim.".length()));
                    if (value > maxClaims) {
                        maxClaims = value;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return maxClaims;
    }


    private static FileConfiguration config;

    public synchronized void save() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (Exception e) {
            Util.log("&cFailed to save config.yml! " + e.getMessage());
        }
    }

    /*
     * Update config
     */
    public Config updateConfig() {
        String version = plugin.getDescription().getVersion();
        String configVersion = config.getString("config_version", "0");

        if(version.equals(configVersion)) return this;

        InputStream defStream = plugin.getResource("config.yml");
        if(defStream == null) {
            Util.log("&cDefault config.yml not found in plugin resources.");
            return this;
        }

        // Backup old config
        saveBackupConfig();

        // Value pasting from old config
        FileConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
        for(String key : defConfig.getKeys(true)) {
            if (defConfig.isConfigurationSection(key)) {
                continue; // Skip parent keys
            }
            if(config.contains(key)) {
                defConfig.set(key, config.get(key));
            }
        }
        File file = new File(this.plugin.getDataFolder(), "config.yml");

        try {
            defConfig.set("config_version", version);
            defConfig.save(file);
            config = defConfig;
            Util.log("&aUpdated config file.");
        }catch (Exception e) {
            Util.log("&cFailed to save updated config file.");
            e.printStackTrace();
        }
        return this;
    }

    private void saveBackupConfig() {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        String date = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss").format(new Date());
        File backupFile = new File(backupDir, "config_" + date + ".yml");
        try {
            config.save(backupFile);
            Util.log("&aBacked up old config file.");
        } catch (Exception e) {
            Util.log("&cFailed to save backup file.");
            e.printStackTrace();
        }
    }

}
