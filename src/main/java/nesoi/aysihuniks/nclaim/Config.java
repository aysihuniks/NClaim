package nesoi.aysihuniks.nclaim;

import lombok.Getter;
import lombok.Setter;
import nesoi.aysihuniks.nclaim.model.TimeLeftThreshold;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.nandayo.dapi.util.Util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class Config {

    private final NClaim plugin;
    private static FileConfiguration config;

    public Config(NClaim plugin) {
        this.plugin = plugin;
    }

    private String defaultLanguage;
    private double claimBuyPrice;
    private double eachLandBuyPrice;
    private boolean enableTieredPricing;
    private int claimExpiryDays;
    private int maxCoopPlayers;
    private int maxClaimCount;
    private List<String> blacklistedWorlds;
    private List<String> blacklistedRegions;
    private int autoSave;

    private boolean showHologramTitle;
    private boolean showHologramOwner;
    private boolean showHologramTimeLeft;
    private boolean showHologramCoopCount;
    private boolean showHologramTotalSize;
    private boolean showHologramEdit;

    private boolean databaseEnabled;
    private String databaseType;
    private String sqliteFile;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUser;
    private String mysqlPassword;
    private int maximumPoolSize;
    private int minimumIdle;
    private long idleTimeout;
    private long maxLifetime;
    private long connectionTimeout;

    private double timeExtensionPricePerMinute;
    private double timeExtensionPricePerHour;
    private double timeExtensionPricePerDay;
    private double timeExtensionTaxRate;

    private Material defaultClaimBlockType = Material.OBSIDIAN;

    public FileConfiguration get() {
        return config;
    }
    private List<TimeLeftThreshold> timeLeftThresholds = new java.util.ArrayList<>();

    public Config load() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        setDefaultLanguage(config.getString("lang_file", "en-US"));
        setBlacklistedWorlds(config.getStringList("blacklisted_worlds"));
        setBlacklistedRegions(config.getStringList("blacklisted_regions"));

        setMaxClaimCount(config.getInt("claim_settings.max_count", 3));
        setClaimBuyPrice(config.getDouble("claim_settings.buy_price", 1500));
        setEachLandBuyPrice(config.getDouble("claim_settings.expand_price", 2000));
        setEnableTieredPricing(config.getBoolean("claim_settings.tiered_pricing.enable", false));
        setMaxCoopPlayers(config.getInt("claim_settings.max_coop.default", 3));
        setClaimExpiryDays(config.getInt("claim_settings.expiry_days", 7));

        setShowHologramTitle(config.getBoolean("hologram_settings.show_title", true));
        setShowHologramOwner(config.getBoolean("hologram_settings.show_owner", true));
        setShowHologramTimeLeft(config.getBoolean("hologram_settings.show_time_left", true));
        setShowHologramCoopCount(config.getBoolean("hologram_settings.show_coop_count", true));
        setShowHologramTotalSize(config.getBoolean("hologram_settings.show_total_size", true));
        setShowHologramEdit(config.getBoolean("hologram_settings.show_edit", true));

        setTimeExtensionPricePerMinute(config.getDouble("time_extension.price_per_minute", 10));
        setTimeExtensionPricePerHour(config.getDouble("time_extension.price_per_hour", 500));
        setTimeExtensionPricePerDay(config.getDouble("time_extension.price_per_day", 10000));
        setTimeExtensionTaxRate(config.getDouble("time_extension.tax_rate", 0.1));

        setAutoSave(config.getInt("auto_save", 30));

        setDatabaseEnabled(config.getBoolean("database.enable", false));
        setDatabaseType(config.getString("database.type", "sqlite"));
        setSqliteFile(config.getString("database.sqlite.file", "database.db"));
        setMysqlHost(config.getString("database.mysql.host", "localhost"));
        setMysqlPort(config.getInt("database.mysql.port", 3306));
        setMysqlDatabase(config.getString("database.mysql.database", "nclaim"));
        setMysqlUser(config.getString("database.mysql.user", "root"));
        setMysqlPassword(config.getString("database.mysql.password", ""));
        setMaximumPoolSize(config.getInt("database.mysql.maximum_pool_size", 10));
        setMinimumIdle(config.getInt("database.mysql.minimum_idle", 5));
        setIdleTimeout(config.getLong("database.mysql.idle_timeout", 300000));
        setMaxLifetime(config.getLong("database.mysql.max_lifetime", 1800000));
        setConnectionTimeout(config.getLong("database.mysql.connection_timeout", 30000));

        String defClaimBlockStr = config.getString("claim_settings.default_claim_block_type", "OBSIDIAN");
        try {
            setDefaultClaimBlockType(Material.valueOf(defClaimBlockStr.toUpperCase()));
        } catch (Exception e) {
            setDefaultClaimBlockType(Material.OBSIDIAN);
            Util.log("&cThe default_claim_block_type in Config is invalid! It has been set to OBSIDIAN.");
        }

        validateTierConfiguration();
        loadTimeLeftThresholds();

        return this;
    }

    public synchronized void save() {
        try {
            config.set("lang_file", getDefaultLanguage());
            config.set("blacklisted_worlds", getBlacklistedWorlds());
            config.set("blacklisted_regions", getBlacklistedRegions());

            config.set("claim_settings.max_count", getMaxClaimCount());
            config.set("claim_settings.buy_price", getClaimBuyPrice());
            config.set("claim_settings.expand_price", getEachLandBuyPrice());
            config.set("claim_settings.max_coop.default", getMaxCoopPlayers());
            config.set("claim_settings.expiry_days", getClaimExpiryDays());

            config.set("hologram_settings.show_title", isShowHologramTitle());
            config.set("hologram_settings.show_owner", isShowHologramOwner());
            config.set("hologram_settings.show_time_left", isShowHologramTimeLeft());
            config.set("hologram_settings.show_coop_count", isShowHologramCoopCount());
            config.set("hologram_settings.show_total_size", isShowHologramTotalSize());
            config.set("hologram_settings.show_edit", isShowHologramEdit());

            config.set("claim_settings.tiered_pricing.enable", isEnableTieredPricing());

            config.set("time_extension.price_per_minute", getTimeExtensionPricePerMinute());
            config.set("time_extension.price_per_hour", getTimeExtensionPricePerHour());
            config.set("time_extension.price_per_day", getTimeExtensionPricePerDay());
            config.set("time_extension.tax_rate", getTimeExtensionTaxRate());

            config.set("auto_save", getAutoSave());

            config.set("database.enable", isDatabaseEnabled());
            config.set("database.type", getDatabaseType());
            config.set("database.sqlite.file", getSqliteFile());
            config.set("database.mysql.host", getMysqlHost());
            config.set("database.mysql.port", getMysqlPort());
            config.set("database.mysql.database", getMysqlDatabase());
            config.set("database.mysql.user", getMysqlUser());
            config.set("database.mysql.password", getMysqlPassword());
            config.set("database.mysql.maximum_pool_size", getMaximumPoolSize());
            config.set("database.mysql.minimum_idle", getMinimumIdle());
            config.set("database.mysql.idle_timeout", getIdleTimeout());
            config.set("database.mysql.max_lifetime", getMaxLifetime());
            config.set("database.mysql.connection_timeout", getConnectionTimeout());

            config.set("claim_settings.default_claim_block_type", getDefaultClaimBlockType().name());

            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (Exception e) {
            Util.log("&cFailed to save config.yml! " + e.getMessage());
        }
    }

    public void loadTimeLeftThresholds() {
        timeLeftThresholds.clear();
        if (config.isList("hologram_settings.time_left_thresholds")) {
            List<?> rawList = config.getList("hologram_settings.time_left_thresholds", new ArrayList<>());
            for (Object obj : rawList) {
                if (!(obj instanceof java.util.Map)) continue;
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                String thresholdStr = String.valueOf(map.get("threshold"));
                String color = String.valueOf(map.get("color"));
                String[] parts = thresholdStr.split(" ");
                if (parts.length != 3) continue;
                TimeLeftThreshold t = new TimeLeftThreshold();
                t.unit = parts[0].trim();
                t.operator = parts[1].trim();
                t.value = Integer.parseInt(parts[2].trim());
                t.color = color;
                timeLeftThresholds.add(t);
            }
        }
    }

    public static long getTimeUnitValue(long seconds, String unit) {
        switch (unit) {
            case "day": return seconds / 86400;
            case "hour": return seconds / 3600;
            case "minute": return seconds / 60;
            default: return seconds;
        }
    }

    public double getTieredPrice(int chunkNumber) {
        if (chunkNumber > 35) {
            return -1;
        }

        if (!isEnableTieredPricing()) {
            return getEachLandBuyPrice();
        }

        ConfigurationSection tiersSection = config.getConfigurationSection("claim_settings.tiered_pricing.tiers");
        if (tiersSection != null) {
            for (String tierKey : tiersSection.getKeys(false)) {
                int minChunk = config.getInt("claim_settings.tiered_pricing.tiers." + tierKey + ".min", 1);
                int maxChunk = config.getInt("claim_settings.tiered_pricing.tiers." + tierKey + ".max", 1);
                double price = config.getDouble("claim_settings.tiered_pricing.tiers." + tierKey + ".price", 0);

                if (minChunk > 35 || maxChunk > 35) {
                    Util.log("&cWarning: Tier " + tierKey + " has chunk numbers above limit (35). Skipping...");
                    continue;
                }

                if (chunkNumber >= minChunk && chunkNumber <= maxChunk) {
                    return price;
                }
            }
        }

        return getEachLandBuyPrice();
    }

    public void validateTierConfiguration() {
        if (!isEnableTieredPricing()) {
            return;
        }

        boolean isValid = true;

        ConfigurationSection tiersSection = config.getConfigurationSection("claim_settings.tiered_pricing.tiers");
        if (tiersSection != null) {
            for (String tierKey : tiersSection.getKeys(false)) {
                int minChunk = config.getInt("claim_settings.tiered_pricing.tiers." + tierKey + ".min", 1);
                int maxChunk = config.getInt("claim_settings.tiered_pricing.tiers." + tierKey + ".max", 1);

                if (minChunk > 35 || maxChunk > 35) {
                    Util.log("&cError: Tier " + tierKey + " exceeds maximum chunk limit (35)!");
                    isValid = false;
                }

                if (minChunk > maxChunk) {
                    Util.log("&cError: Tier " + tierKey + " has min (" + minChunk + ") greater than max (" + maxChunk + ")!");
                    isValid = false;
                }

                if (minChunk < 1) {
                    Util.log("&cError: Tier " + tierKey + " has min chunk less than 1!");
                    isValid = false;
                }
            }
        }

        if (!isValid) {
            Util.log("&cTiered pricing system has configuration errors. Please fix your config.yml!");
        }

    }

    public int getMaxCoopPlayers(Player player) {
        if (player.isOp() || player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.max_coop_count")) {
            return Integer.MAX_VALUE;
        }

        int maxCoop = config.getInt("claim_settings.max_coop_count.default", 3);

        ConfigurationSection maxCoopSection = config.getConfigurationSection("claim_settings.max_coop_count");
        if (maxCoopSection != null) {
            for (String key : maxCoopSection.getKeys(false)) {
                if (!key.equals("default") && player.hasPermission("nclaim.maxcoop." + key)) {
                    int value = config.getInt("claim_settings.max_coop_count." + key);
                    if (value > maxCoop) {
                        maxCoop = value;
                    }
                }
            }
        }

        return maxCoop;
    }


    public int getMaxClaimCount(Player player) {
        if (player.isOp() || player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.max_claim_count")) {
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

    public boolean isValidLanguage(String lang) {
        return lang.equals("en-US") || lang.equals("tr-TR");
    }

    public Config updateConfig() {
        String version = plugin.getDescription().getVersion();
        String configVersion = config.getString("config_version", "0");

        if(version.equals(configVersion)) return this;

        InputStream defStream = plugin.getResource("config.yml");
        if(defStream == null) {
            Util.log("&cDefault config.yml not found in plugin resources.");
            return this;
        }

        saveBackupConfig();

        FileConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
        for(String key : defConfig.getKeys(true)) {
            if (defConfig.isConfigurationSection(key)) {
                continue;
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
        if (!backupDir.exists()) { //noinspection ResultOfMethodCallIgnored
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
