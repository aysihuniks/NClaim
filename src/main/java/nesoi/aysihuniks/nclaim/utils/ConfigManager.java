package nesoi.aysihuniks.nclaim.utils;

import nesoi.aysihuniks.nclaim.NClaim;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nandayo.dapi.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class ConfigManager {

    private final YamlConfiguration config;

    public ConfigManager(FileConfiguration config) {
        this.config = YamlConfiguration.loadConfiguration(new StringReader(config.saveToString()));
    }

    /*
     * SETTER
     */
    public void set(String key, Object value) {
        config.set(key, value);
    }

    /*
     * GETTERS
     */
    public Object get(String key, Object defaultValue) {
        return config.get(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        Object value = config.get(key);
        return (value != null) ? value.toString() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        return config.getInt(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        return config.getDouble(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    public ConfigurationSection getConfigurationSection(String key) {
        return config.getConfigurationSection(key);
    }

    public List<String> getStringList(String key) {
        return config.getStringList(key);
    }

    public String getSound(String key, String defaultValue) {
        return getString("sounds." + key, defaultValue);
    }

    /*
     * SAVE
     */


    public void saveConfig() {
        try {
            config.save(new File(NClaim.inst().getDataFolder(), "config.yml"));
        } catch (IOException e) {
            Util.log("Failed to loading save config: " + e.getMessage());
        }
    }
}
