package nesoi.network.NClaim;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nandayo.DAPI.Util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Config {

    public FileConfiguration get() {
        return config;
    }

    private static File file;
    private static FileConfiguration config;

    private final NCoreMain plugin;

    public Config(NCoreMain plugin) {
        this.plugin = plugin;
        file = new File(this.plugin.getDataFolder(), "config.yml");
        if(!file.exists()) {
            this.plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
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
        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
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
