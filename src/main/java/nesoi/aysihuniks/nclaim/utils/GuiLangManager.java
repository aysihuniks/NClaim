package nesoi.aysihuniks.nclaim.utils;

import lombok.Getter;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Setting;
import nesoi.aysihuniks.nclaim.model.SettingCfg;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.nandayo.dapi.util.Util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiLangManager {

    @Getter
    private final FileConfiguration guiConfig;

    private final HeadManager headManager;
    private Map<Setting, SettingCfg> settingConfigs;

    public GuiLangManager() {
        File file = new File(NClaim.inst().getDataFolder(), "guis.yml");
        if (!file.exists()) {
            NClaim.inst().saveResource("guis.yml", false);
        }
        this.guiConfig = YamlConfiguration.loadConfiguration(file);
        this.headManager = NClaim.inst().getHeadManager();
        this.settingConfigs = new HashMap<>();
        loadSettingConfigs();
    }

    private void loadSettingConfigs() {
        ConfigurationSection settingsSection = guiConfig.getConfigurationSection("guis.claim_settings_menu.settings");
        if (settingsSection == null) return;

        for (String key : settingsSection.getKeys(false)) {
            ConfigurationSection settingSection = settingsSection.getConfigurationSection(key);
            if (settingSection == null) continue;

            boolean defaultValue = settingSection.getBoolean("default_value", false);
            boolean changeable = settingSection.getBoolean("changeable", true);
            String permission = settingSection.getString("permission", null);
            String displayName = settingSection.getString("display_name", key);
            String material = settingSection.getString("material", "DIRT");
            List<String> lore = settingSection.getStringList("lore");

            Setting settingEnum = getSettingByKey(key);
            if (settingEnum != null) {
                settingConfigs.put(settingEnum, new SettingCfg(defaultValue, changeable, permission, displayName, material, lore));
            }
        }
    }

    private Setting getSettingByKey(String key) {
        switch (key) {
            case "pvp": return Setting.CLAIM_PVP;
            case "tnt_explosions": return Setting.TNT_DAMAGE;
            case "creeper_explosions": return Setting.CREEPER_DAMAGE;
            case "mob_attacks": return Setting.MOB_ATTACKING;
            case "monster_spawning": return Setting.MONSTER_SPAWNING;
            case "animal_spawning": return Setting.ANIMAL_SPAWNING;
            case "villager_interactions": return Setting.VILLAGER_INTERACTION;
            default: return null;
        }
    }

    public SettingCfg getSettingConfig(Setting setting) {
        return settingConfigs.get(setting);
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

    public Boolean getBoolean(String path) {
        return guiConfig.getBoolean("guis." + path);
    }

    public ItemStack getMaterial(String section, String path) {
        String materialName = guiConfig.getString("guis." + section + "." + path + ".material");
        return resolveItemStack(materialName, "guis." + section + "." + path);
    }

    public ItemStack getMaterial(String fullPath) {
        String materialName = guiConfig.getString("guis." + fullPath + ".material");
        return resolveItemStack(materialName, "guis." + fullPath);
    }

    private ItemStack resolveItemStack(String materialName, String logPath) {
        if (materialName == null) {
            Util.log("Material not found for path: " + logPath + ". Using DIRT as default.");
            return new ItemStack(Material.DIRT);
        }

        String matName = materialName;
        Integer customModelData = null;

        if (materialName.contains(":")) {
            String[] split = materialName.split(":");
            matName = split[0];
            try {
                customModelData = Integer.parseInt(split[1]);
            } catch (NumberFormatException e) {
                Util.log("Invalid custom model data '" + split[1] + "' for path: " + logPath + ". Ignoring custom model data.");
            }
        }

        ItemStack item;
        if (matName.toUpperCase().startsWith("HEAD")) {
            String texture = matName.length() > 4 ? matName.substring(5) : "";
            item = headManager.createHeadWithTexture(texture);
        } else {
            try {
                Material material = Material.valueOf(matName.toUpperCase());
                item = new ItemStack(material);
            } catch (IllegalArgumentException e) {
                Util.log("Invalid material name '" + matName + "' for path: " + logPath + ". Using DIRT as default. Error: " + e.getMessage());
                item = new ItemStack(Material.DIRT);
            }
        }

        if (customModelData != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                NamespacedKey customModelKey = new NamespacedKey(NClaim.inst(), "custom_model_data");

                meta.getPersistentDataContainer().set(customModelKey, PersistentDataType.INTEGER, customModelData);
                item.setItemMeta(meta);
            }
        }

        return item;
    }
}