package nesoi.aysihuniks.nclaim;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.nandayo.dapi.Util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
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

    public String defaultLanguage;
    public double claimBuyPrice;
    public double eachLandBuyPrice;
    public int claimExpiryDays;
    public int maxCoopPlayers;
    public int maxClaimCount;
    public List<String> blacklistedWorlds;
    public List<String> blacklistedRegions;
    public int autoSave;

    private boolean enableTieredPricing;

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

    public FileConfiguration get() {
        return config;
    }

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
        setMaxCoopPlayers(config.getInt("claim_settings.max_coop.default", 3));

        setClaimExpiryDays(config.getInt("claim_settings.expiry_days", 7));

        setEnableTieredPricing(config.getBoolean("claim_settings.tiered_pricing.enable", false));

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

        validateTierConfiguration();

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

            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (Exception e) {
            Util.log("&cFailed to save config.yml! " + e.getMessage());
        }
    }

    public double getTieredPrice(int chunkNumber) {
        if (chunkNumber > 41) {
            return -1;
        }

        if (!isEnableTieredPricing()) {
            return getEachLandBuyPrice();
        }

        if (config.isConfigurationSection("claim_settings.tiered_pricing.tiers")) {
            for (String tierKey : config.getConfigurationSection("claim_settings.tiered_pricing.tiers").getKeys(false)) {
                int minChunk = config.getInt("claim_settings.tiered_pricing.tiers." + tierKey + ".min", 1);
                int maxChunk = config.getInt("claim_settings.tiered_pricing.tiers." + tierKey + ".max", 1);
                double price = config.getDouble("claim_settings.tiered_pricing.tiers." + tierKey + ".price", 0);

                if (minChunk > 41 || maxChunk > 41) {
                    Util.log("&cWarning: Tier " + tierKey + " has chunk numbers above limit (41). Skipping...");
                    continue;
                }

                if (chunkNumber >= minChunk && chunkNumber <= maxChunk) {
                    return price;
                }
            }
        }

        return getEachLandBuyPrice();
    }

    public String getTierInfo() {
        if (!isEnableTieredPricing()) {
            return "§7Tiered pricing system is disabled. Fixed price: §a$" + String.format("%.2f", getEachLandBuyPrice());
        }

        StringBuilder info = new StringBuilder("§6=== Tiered Pricing System ===\n");

        if (config.isConfigurationSection("claim_settings.tiered_pricing.tiers")) {
            for (String tierKey : config.getConfigurationSection("claim_settings.tiered_pricing.tiers").getKeys(false)) {
                int minChunk = config.getInt("claim_settings.tiered_pricing.tiers." + tierKey + ".min", 1);
                int maxChunk = config.getInt("claim_settings.tiered_pricing.tiers." + tierKey + ".max", 1);
                double price = config.getDouble("claim_settings.tiered_pricing.tiers." + tierKey + ".price", 0);

                // Skip invalid tiers
                if (minChunk > 41 || maxChunk > 41) {
                    info.append("§c").append(tierKey).append(": §cInvalid (exceeds 41 chunk limit)\n");
                    continue;
                }

                String priceText = price > 0 ? "$" + String.format("%.0f", price) : "FREE";
                info.append("§e").append(minChunk).append("-").append(maxChunk)
                        .append(" chunks: §a").append(priceText).append(" per chunk\n");
            }
        }

        return info.toString();
    }

    public boolean validateTierConfiguration() {
        if (!isEnableTieredPricing()) {
            return true;
        }

        boolean isValid = true;

        if (config.isConfigurationSection("claim_settings.tiered_pricing.tiers")) {
            for (String tierKey : config.getConfigurationSection("claim_settings.tiered_pricing.tiers").getKeys(false)) {
                int minChunk = config.getInt("claim_settings.tiered_pricing.tiers." + tierKey + ".min", 1);
                int maxChunk = config.getInt("claim_settings.tiered_pricing.tiers." + tierKey + ".max", 1);

                if (minChunk > 41 || maxChunk > 41) {
                    Util.log("&cError: Tier " + tierKey + " exceeds maximum chunk limit (41)!");
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

        return isValid;
    }

    public int getMaxCoopPlayers(Player player) {
        if (player.isOp() || player.hasPermission("nclaim.bypass.*") || player.hasPermission("nclaim.bypass.max_coop_count")) {
            return Integer.MAX_VALUE;
        }

        int maxCoop = config.getInt("claim_settings.max_coop_count.default", 3);

        if (config.isConfigurationSection("claim_settings.max_coop_count")) {
            for (String key : config.getConfigurationSection("claim_settings.max_coop_count").getKeys(false)) {
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
