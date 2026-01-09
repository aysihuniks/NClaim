package nesoi.aysihuniks.nclaim.utils;

import nesoi.aysihuniks.nclaim.NClaim;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.util.HexUtil;
import org.nandayo.dapi.util.Util;
import org.nandayo.dapi.message.ChannelType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LangManager {

    private final @NotNull NClaim plugin;
    private final File folder;
    public final List<String> REGISTERED_LANGUAGES = new ArrayList<>();
    private final List<String> DEFAULT_LANGUAGES = Arrays.asList("en-US", "tr-TR", "fr-FR");
    private final String DEFAULT_LANGUAGE = "en-US";
    private FileConfiguration DEFAULT_LANGUAGE_CONFIG;
    private FileConfiguration SELECTED_LANGUAGE_CONFIG;

    public LangManager(@NotNull NClaim plugin, @NotNull String fileName) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "lang");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.loadDefaultFiles();
        this.loadFiles(fileName);
    }

    private void loadFiles(@NotNull String searchingFor) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName().substring(0, file.getName().length() - 4);
                REGISTERED_LANGUAGES.add(fileName);
                if (fileName.equals(searchingFor)) {
                    this.SELECTED_LANGUAGE_CONFIG = (DEFAULT_LANGUAGES.contains(fileName)) ? updateLanguage(fileName) : YamlConfiguration.loadConfiguration(file);
                }
                if (fileName.equals(DEFAULT_LANGUAGE)) {
                    this.DEFAULT_LANGUAGE_CONFIG = updateLanguage(fileName);
                }
            }
        }
        if (this.SELECTED_LANGUAGE_CONFIG == null) {
            this.SELECTED_LANGUAGE_CONFIG = this.DEFAULT_LANGUAGE_CONFIG;
            Util.log("&cLanguage " + searchingFor + " was not found. Using default language.");
        }
    }

    private void loadDefaultFiles() {
        for (String fileName : DEFAULT_LANGUAGES) {
            File file = new File(folder, fileName + ".yml");
            if (file.exists() || plugin.getResource("lang/" + fileName + ".yml") == null) continue;
            plugin.saveResource("lang/" + fileName + ".yml", false);
        }
    }

    public FileConfiguration updateLanguage(@NotNull String languageName) {
        File file = new File(folder, languageName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String version = plugin.getDescription().getVersion();
        String configVersion = config.getString("lang_version", "0");

        if (version.equals(configVersion)) return config;

        Util.log("&eNew language version detected for " + languageName + " (" + version + "), updating...");

        FileConfiguration defConfig = getSourceConfiguration(languageName);
        if (defConfig == null) return config;

        saveBackupConfig(languageName, config);

        for (String key : defConfig.getKeys(true)) {
            if (defConfig.isConfigurationSection(key)) continue;
            if (config.contains(key)) {
                defConfig.set(key, config.get(key));
            }
        }

        defConfig.set("lang_version", version);
        try {
            defConfig.save(file);
            Util.log("&aLanguage file '" + languageName + "' successfully updated to version " + version + "!");
        } catch (Exception e) {
            Util.log("&cAn error occurred while saving the updated language file: " + languageName);
        }
        return defConfig;
    }

    private FileConfiguration getSourceConfiguration(@NotNull String languageName) {
        InputStream defStream = plugin.getResource("lang/" + languageName + ".yml");
        if (defStream == null) {
            Util.log("&cDefault '" + languageName + ".yml' was not found in plugin resources.");
            return null;
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
    }

    private void saveBackupConfig(@NotNull String languageName, @NotNull FileConfiguration config) {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(backupDir, "lang_" + languageName + "_" + date + ".yml");
        try {
            config.save(backupFile);
            Util.log("&aBacked up old language file: " + languageName);
        } catch (Exception e) {
            Util.log("&cFailed to save backup file for: " + languageName);
        }
    }

    static public void sendSortedMessage(@NotNull Player player, String msg) {
        if (msg == null || msg.isEmpty()) return;
        String[] parts = msg.split("=");
        if (parts.length < 2) {
            ChannelType.CHAT.send(player, msg);
            return;
        }
        switch (parts[0]) {
            case "CHAT": ChannelType.CHAT.send(player, parts[1]); break;
            case "ACTION_BAR": ChannelType.ACTION_BAR.send(player, parts[1]); break;
            case "TITLE": ChannelType.TITLE.send(player, parts[1]); break;
        }
    }

    @Nullable
    public ConfigurationSection getSection(@NotNull String path) {
        ConfigurationSection section = SELECTED_LANGUAGE_CONFIG.getConfigurationSection(path);
        if (section != null) return section;
        return DEFAULT_LANGUAGE_CONFIG.getConfigurationSection(path);
    }

    @NotNull
    public String getString(@NotNull String path) {
        String str = SELECTED_LANGUAGE_CONFIG.contains(path)
                ? SELECTED_LANGUAGE_CONFIG.getString(path)
                : DEFAULT_LANGUAGE_CONFIG.getString(path);
        if (str == null) {
            Util.log("&cNull message at path '" + path + "'");
            return "";
        }
        String prefix = NClaim.inst().getConfigManager().getString("prefix", "&8[<#fa8443>NClaim&8]&r");
        return HexUtil.parse(str.replace("{prefix}", prefix));
    }

    @NotNull
    public List<String> getStringList(@NotNull String path) {
        List<String> list = SELECTED_LANGUAGE_CONFIG.contains(path)
                ? SELECTED_LANGUAGE_CONFIG.getStringList(path)
                : DEFAULT_LANGUAGE_CONFIG.getStringList(path);
        String prefix = NClaim.inst().getConfigManager().getString("prefix", "&8[<#fa8443>NClaim&8]&r");
        return list.stream()
                .map(str -> HexUtil.parse(str.replace("{prefix}", prefix)))
                .collect(Collectors.toList());
    }

    @NotNull
    public Boolean getBoolean(@NotNull String path) {
        return SELECTED_LANGUAGE_CONFIG.contains(path)
                ? SELECTED_LANGUAGE_CONFIG.getBoolean(path)
                : DEFAULT_LANGUAGE_CONFIG.getBoolean(path);
    }

    @NotNull
    public String getString(@Nullable ConfigurationSection section, @NotNull String subPath) {
        final String currentPath = section == null ? "" : section.getCurrentPath();
        return getString(currentPath + "." + subPath);
    }

    @NotNull
    public List<String> getStringList(@Nullable ConfigurationSection section, @NotNull String subPath) {
        final String currentPath = section == null ? "" : section.getCurrentPath();
        return getStringList(currentPath + "." + subPath);
    }

    @NotNull
    public Boolean getBoolean(@Nullable ConfigurationSection section, @NotNull String subPath) {
        final String currentPath = section == null ? "" : section.getCurrentPath();
        return getBoolean(currentPath + "." + subPath);
    }
}