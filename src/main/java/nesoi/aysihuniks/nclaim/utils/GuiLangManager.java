package nesoi.aysihuniks.nclaim.utils;

import nesoi.aysihuniks.nclaim.NClaim;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class GuiLangManager {

    private final FileConfiguration guiConfig;

    public GuiLangManager() {
        File file = new File(NClaim.inst().getDataFolder(), "guis.yml");
        if (!file.exists()) {
            NClaim.inst().saveResource("guis.yml", false);
        }
        this.guiConfig = YamlConfiguration.loadConfiguration(file);
    }

    public String getString(String section, String path) {
        return guiConfig.getString("guis." + section + "." + path);
    }
    public String getString(String section, String path, String defaultValue) {
        return guiConfig.getString("guis." + section + "." + path, defaultValue);
    }

    public List<String> getStringList(String section, String path) {
        return guiConfig.getStringList("guis." + section + "." + path);
    }

    public String getString(String fullPath) {
        return guiConfig.getString("guis." + fullPath, fullPath);
    }

    public List<String> getStringList(String fullPath) {
        return guiConfig.getStringList("guis." + fullPath);
    }

    public ConfigurationSection getSection(String path) {
        return guiConfig.getConfigurationSection("guis." + path);
    }

}