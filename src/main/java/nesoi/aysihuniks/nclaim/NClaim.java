package nesoi.aysihuniks.nclaim;

import de.tr7zw.changeme.nbtapi.NBT;
import lombok.Getter;
import lombok.Setter;
import nesoi.aysihuniks.nclaim.commands.AllCommandExecutor;
import nesoi.aysihuniks.nclaim.database.DatabaseManager;
import nesoi.aysihuniks.nclaim.database.MySQLManager;
import nesoi.aysihuniks.nclaim.database.SQLiteManager;
import nesoi.aysihuniks.nclaim.enums.Balance;
import nesoi.aysihuniks.nclaim.enums.HoloEnum;
import nesoi.aysihuniks.nclaim.hologram.HologramManager;
import nesoi.aysihuniks.nclaim.integrations.Expansion;
import nesoi.aysihuniks.nclaim.integrations.GeikFarmer;
import nesoi.aysihuniks.nclaim.integrations.Metrics;
import nesoi.aysihuniks.nclaim.integrations.AxSellWand;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.model.User;
import nesoi.aysihuniks.nclaim.model.UserManager;
import nesoi.aysihuniks.nclaim.service.*;
import nesoi.aysihuniks.nclaim.utils.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.DAPI;
import org.nandayo.dapi.object.DEntityType;
import org.nandayo.dapi.object.DMaterial;
import org.nandayo.dapi.object.DParticle;
import org.nandayo.dapi.object.DSound;
import org.nandayo.dapi.util.HexUtil;
import org.nandayo.dapi.util.Util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@Getter
@Setter
public final class NClaim extends JavaPlugin {
    private static NClaim instance;

    private Wrapper wrapper;
    private ClaimService claimService;
    private ClaimStorageManager claimStorageManager;
    private ClaimExpirationManager claimExpirationManager;
    private ClaimCoopManager claimCoopManager;
    private ClaimBlockManager claimBlockManager;
    private GuiLangManager guiLangManager;
    private HologramManager hologramManager;
    private ClaimVisualizerService claimVisualizerService;
    private ClaimSettingsManager claimSettingsManager;
    private ClaimLevelManager blockValueManager;
    private HeadManager headManager;
    private MySQLManager mySQLManager;
    private SQLiteManager sqLiteManager;
    private DatabaseManager databaseManager;
    @Getter
    private static Economy econ = null;

    private LangManager langManager;
    private ConfigManager configManager;

    private Config nconfig;
    private Balance balanceSystem;
    private static Economy economy;

    private BukkitTask autoSaveTask;

    public static NClaim inst() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        initializeDAPI();

        if (!getDataFolder().exists()) { //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }
        nconfig = new Config(this).load().updateConfig();
        configManager = new ConfigManager(nconfig.get());

        initializeManagers();

        setupDatabase();

        loadConfigurations();

        registerEventHandlers();
        registerCommands();

        setupIntegrations();

        if (!NBT.preloadApi()) {
            Util.log("&cNBT-API wasn't initialized properly, disabling the plugin");
            getPluginLoader().disablePlugin(this);
            return;
        }

        initializeHologramManager();

        setupHeadManager();

        startTasks();

        loadData();

        setupMetrics();
    }

    private void initializeDAPI() {
        DAPI.registerMenuListener();
        setupHexColors();
    }

    private void loadConfigurations() {
        File blocksFile = new File(getDataFolder(), "block_levels.yml");
        if (!blocksFile.exists()) {
            saveResource("block_levels.yml", false);
        }
        blockValueManager.loadBlockValues();
    }

    public void reloadPlugin() {
        stopTasks();

        String oldDatabaseType = nconfig.isDatabaseEnabled() ? nconfig.getDatabaseType().toLowerCase() : "yaml";

        if (nconfig.isDatabaseEnabled() && databaseManager != null) {
            for (Claim claim : Claim.claims) {
                try {
                    databaseManager.saveClaim(claim);
                } catch (Exception e) {
                    Util.log("&cError saving claim during reload: " + e.getMessage());
                }
            }
        } else if (claimStorageManager != null) {
            claimStorageManager.saveClaims();
        }

        nconfig = new Config(this).load().updateConfig();
        configManager = new ConfigManager(nconfig.get());
        langManager = new LangManager(this, configManager.getString("lang_file", "en-US"));
        guiLangManager = new GuiLangManager();

        if (nconfig.isDatabaseEnabled()) {
            if (mySQLManager != null) {
                mySQLManager.close();
            }
            if (sqLiteManager != null) {
                sqLiteManager.close();
            }

            String dbType = nconfig.getDatabaseType().toLowerCase();
            try {
                if ("mysql".equals(dbType)) {
                    mySQLManager = new MySQLManager(nconfig);
                    databaseManager = mySQLManager;
                    Util.log("&aMySQL connection reestablished.");
                } else if ("sqlite".equals(dbType)) {
                    sqLiteManager = new SQLiteManager(nconfig);
                    databaseManager = sqLiteManager;
                    Util.log("&aSQLite connection reestablished.");
                }
            } catch (Exception e) {
                Util.log("&cDatabase reconnection failed: " + e.getMessage());
                databaseManager = null;
            }
        }

        claimSettingsManager = new ClaimSettingsManager(this);

        if (nconfig.isDatabaseEnabled() && databaseManager != null) {
            try {
                List<Claim> claims = databaseManager.loadAllClaims();

                String newDatabaseType = nconfig.getDatabaseType().toLowerCase();
                if (claims.isEmpty() && !oldDatabaseType.equals(newDatabaseType) && !Claim.claims.isEmpty()) {
                    Util.log("&eDatabase type changed from " + oldDatabaseType + " to " + newDatabaseType +
                            ", migrating " + Claim.claims.size() + " claims...");

                    for (Claim claim : Claim.claims) {
                        try {
                            databaseManager.saveClaim(claim);
                        } catch (Exception e) {
                            Util.log("&cError migrating claim: " + e.getMessage());
                        }
                    }
                    Util.log("&aMigration completed! " + Claim.claims.size() + " claims migrated to " + newDatabaseType);
                } else {
                    Claim.claims.clear();
                    Claim.claims.addAll(claims);
                }

                Util.log("&aReloaded " + Claim.claims.size() + " claims from database.");
            } catch (Exception e) {
                Util.log("&cFailed to reload from database: " + e.getMessage());
                if (claimStorageManager != null) {
                    claimStorageManager.loadClaims();
                }
            }
        } else {
            if (claimStorageManager != null) {
                claimStorageManager.loadClaims();
            }
        }

        for (Player player : getServer().getOnlinePlayers()) {
            try {
                User.saveUser(player.getUniqueId());
                User.loadUser(player.getUniqueId());
            } catch (Exception e) {
                Util.log("&cError reloading user " + player.getName() + ": " + e.getMessage());
            }
        }

        claimBlockManager = new ClaimBlockManager();
        blockValueManager.reloadBlockValues();
        if (hologramManager != null) {
            hologramManager.forceCleanup();
        }

        startTasks();

        Util.log("&aPlugin reload completed!");
    }

    private void stopTasks() {
        if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        claimExpirationManager.stopExpirationChecker();
    }

    private void initializeManagers() {
        wrapper = new Wrapper(this);
        if (NClaim.inst().getServer().getPluginManager().getPlugin("Farmer") != null) {
            GeikFarmer.registerIntegration();
        }

        blockValueManager = new ClaimLevelManager(this);
        claimService = new ClaimService(this);
        claimStorageManager = new ClaimStorageManager(this);
        claimExpirationManager = new ClaimExpirationManager(this);
        claimCoopManager = new ClaimCoopManager(this);
        claimVisualizerService = new ClaimVisualizerService(this);
        claimSettingsManager = new ClaimSettingsManager(this);
        claimBlockManager = new ClaimBlockManager();
        guiLangManager = new GuiLangManager();
        langManager = new LangManager(this, configManager.getString("lang_file", "en-US"));
    }

    private void initializeHologramManager() {
        if (HoloEnum.getActiveHologram() == null) {
            Util.log("&cNo supported hologram plugin found (DecentHolograms or FancyHolograms). Disabling hologram functionality.");
            return;
        }

        try {
            hologramManager = new HologramManager(this);
            Util.log("&aHologramManager initialized successfully!");
        } catch (Exception e) {
            Util.log("&cFailed to initialize HologramManager: " + e.getMessage());
        }

        hologramManager.cleanupOrphanedHolograms();
    }

    private void registerEventHandlers() {
        getServer().getPluginManager().registerEvents(new UserManager(), this);
        getServer().getPluginManager().registerEvents(new ClaimManager(this, claimCoopManager), this);
        getServer().getPluginManager().registerEvents(new WorldLoadHandler(this), this);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("nclaim");
        if (command != null) {
            command.setExecutor(new AllCommandExecutor());
            command.setTabCompleter(new AllCommandExecutor());
        }
    }

    private void setupIntegrations() {
        setupHologramPlugin();
        setupWorldGuard();
        setupPlaceholderAPI();
        setupVault();
        setupAxsellwand();
        checkForUpdates();
    }

    private void setupAxsellwand() {
        if (getServer().getPluginManager().getPlugin("Axsellwand") != null) {
            getServer().getPluginManager().registerEvents(new AxSellWand(this), this);
            Util.log("&aAxsellwand integration enabled successfully!");
        }
    }

    private void setupHologramPlugin() {
        if (!HoloEnum.isHologramPluginEnabled()) {
            Util.log("&cYou need to have one of the &rDecentHolograms &cor &rFancyHolograms &cplugins installed!");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void setupHeadManager() {
        try {
            headManager = new HeadManager();
            Util.log("&aHeadManager initialized successfully!");
        } catch (Exception e) {
            Util.log("&cFailed to initialize HeadManager: " + e.getMessage());
        }
    }

    private boolean worldGuardEnabled;

    private void setupWorldGuard() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            Util.log("&aWorldGuard integration enabled successfully!");
        } else {
            worldGuardEnabled = false;
            Util.log("&eWorldGuard not found! Region protection features will be disabled.");
        }
    }

    private void setupPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Expansion(this).register();
        }
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            Util.log("&cVault plugin not found! Using PlayerData balance system.");
            balanceSystem = Balance.PLAYERDATA;
            return;
        }

        try {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                Util.log("&cVault plugin found but no economy provider found! Using playerdata balance system.");
                balanceSystem = Balance.PLAYERDATA;
                return;
            }

            econ = rsp.getProvider();
            balanceSystem = Balance.VAULT;
            Util.log("&aSuccessfully hooked into Vault economy!");
        } catch (Exception e) {
            Util.log("&cError setting up Vault economy: " + e.getMessage());
            balanceSystem = Balance.PLAYERDATA;
        }
    }

    private void setupMetrics() {
        new Metrics(this, 24693);
    }


    private void startTasks() {
        claimExpirationManager.startExpirationChecker();

        long MINUTES = getNconfig().getAutoSave() * 60 * 20L;

        if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
            autoSaveTask.cancel();
        }

        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();

                for (Claim claim : Claim.claims) {
                    long claimValue = blockValueManager.calculateClaimValue(claim);
                    claim.setClaimValue(claimValue);
                }

                if (nconfig.isDatabaseEnabled() && databaseManager != null) {
                    try {
                        databaseManager.saveClaimsBatch(new ArrayList<>(Claim.claims));
                    } catch (Exception e) {
                        Util.log("&cFailed to save claims to database: " + e.getMessage());
                    }
                } else if (claimStorageManager != null) {
                    claimStorageManager.saveClaims();
                }

                int claimCount = Claim.claims.size();

                Collection<? extends Player> onlinePlayers = getServer().getOnlinePlayers();
                for (Player player : onlinePlayers) {
                    User.saveUser(player.getUniqueId());
                }

                long duration = System.currentTimeMillis() - startTime;
                Util.log(String.format("&aAuto-save completed! &7(Saved &f%d claims &7and calculated values &7in &f%dms&7)",
                        claimCount, duration));
            }
        }.runTaskTimer(this, MINUTES, MINUTES);
    }

    private void loadData() {
        if (nconfig.isDatabaseEnabled() && databaseManager != null) {
            try {
                List<Claim> claims = databaseManager.loadAllClaims();
                Claim.claims.clear();
                Claim.claims.addAll(claims);
                Util.log("&aLoaded " + claims.size() + " claims from database.");

                if (claims.isEmpty()) {
                    checkForMigrationOpportunity();
                }

            } catch (Exception e) {
                Util.log("&cFailed to load from database: " + e.getMessage());
                claimStorageManager.loadClaims();
            }
        } else {
            claimStorageManager.loadClaims();
        }

        for (Player player : getServer().getOnlinePlayers()) {
            User.loadUser(player.getUniqueId());
        }
    }

    private void setupHexColors() {
        Map<String, String> colors = new HashMap<>();
        colors.put("{WHITE}", "<#FFF8E8>");
        colors.put("{DARKGREEN}", "<#0A6847>");
        colors.put("{GREEN}", "<#7ABA78>");
        colors.put("{DARKRED}", "<#6D2323>");
        colors.put("{RED}", "<#cf2525>");
        colors.put("{YELLOW}", "<#FFEC9E>");
        colors.put("{ORANGE}", "<#fa8443>");
        colors.put("{GRAY}", "<#ababab>");
        colors.put("{BROWN}", "<#825B32>");
        colors.put("{PURPLE}", "<#8D77AB>");
        colors.put("{prefix}", "&8[<#fa8443>NClaim&8]&r");

        HexUtil.PLACEHOLDERS.putAll(colors);
        Util.PREFIX = "&8[<#fa8443>NClaim&8]&r ";
    }

    @Override
    public void onDisable() {
        stopTasks();

        if (!Claim.claims.isEmpty()) {
            Util.log("&eUpdating claim values before save...");
            for (Claim claim : Claim.claims) {
                long claimValue = blockValueManager.calculateClaimValue(claim);
                claim.setClaimValue(claimValue);
            }
        }

        if (nconfig.isDatabaseEnabled() && databaseManager != null) {
            try {
                databaseManager.saveClaimsBatch(new ArrayList<>(Claim.claims));
                Util.log("&aShutdown: Saved " + Claim.claims.size() + " claims to database.");
            } catch (Exception e) {
                Util.log("&cFailed to save claims to database during shutdown: " + e.getMessage());
                e.printStackTrace();

                if (claimStorageManager != null) {
                    try {
                        claimStorageManager.saveClaims();
                        Util.log("&eFallback: Saved claims to YAML file.");
                    } catch (Exception yamlError) {
                        Util.log("&cFallback YAML save also failed: " + yamlError.getMessage());
                        yamlError.printStackTrace();
                    }
                }
            }
        } else if (claimStorageManager != null) {
            try {
                claimStorageManager.saveClaims();
                Util.log("&aShutdown: Saved " + Claim.claims.size() + " claims to YAML file.");
            } catch (Exception e) {
                Util.log("&cFailed to save claims to YAML during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }

        try {
            int userCount = 0;
            for (Player player : getServer().getOnlinePlayers()) {
                User.saveUser(player.getUniqueId());
                userCount++;
            }
            if (userCount > 0) {
                Util.log("&eSaved " + userCount + " user data.");
            }
        } catch (Exception e) {
            Util.log("&cError saving user data: " + e.getMessage());
        }

        try {
            if (mySQLManager != null) {
                mySQLManager.close();
            }
            if (sqLiteManager != null) {
                sqLiteManager.close();
            }
        } catch (Exception e) {
            Util.log("&cError closing database connections: " + e.getMessage());
        }

        instance = null;
    }

    public static boolean isChunkAdjacent(@NotNull Chunk chunk, @NotNull Chunk thatChunk, int radius) {
        return Math.abs(chunk.getX() - thatChunk.getX()) <= radius &&
                Math.abs(chunk.getZ() - thatChunk.getZ()) <= radius;
    }

    public static Material getMaterial(DMaterial dMaterial, DMaterial def) {
        Material mat = dMaterial.parseMaterial();
        return mat != null ? mat : (def != null ? def.parseMaterial() : Material.AIR);
    }

    public static Particle getParticle(@NotNull DParticle dParticle, @NotNull DParticle def) {
        Particle particle = dParticle.get();
        return particle != null ? particle : def.get();
    }

    public static Sound getSound(@NotNull DSound dSound, @NotNull DSound def) {
        Sound sound = dSound.parseSound();
        return sound != null ? sound : def.parseSound();
    }

    public static EntityType getEntityType(@NotNull DEntityType dEntityType, DEntityType def) {
        EntityType entityType = dEntityType.get();
        return entityType != null ? entityType : def.get();
    }

    static public String serializeDate(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(date);
    }

    static public Date deserializeDate(String date) {
        if (date == null || date.isEmpty()) return null;
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        try {
            return format.parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    static public String serializeLocation(Location location) {
        if (location == null) return null;
        World world = location.getWorld();
        if (world == null) return null;
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return world.getName() + "," + x + "," + y + "," + z;
    }

    static public Location deserializeLocation(String location) {
        if (location == null || location.isEmpty()) return null;
        String[] split = location.split(",");
        World world = Bukkit.getWorld(split[0]);
        if (world == null) return null;
        double x = Double.parseDouble(split[1]);
        double y = Double.parseDouble(split[2]);
        double z = Double.parseDouble(split[3]);
        return new Location(world, x, y, z);
    }

    static public String getCoordinates(@NotNull Chunk chunk) {
        int centerX = chunk.getX() * 16 + 8;
        int centerZ = chunk.getZ() * 16 + 8;
        return centerX + "," + centerZ;
    }

    static public String serializeChunk(@NotNull Chunk chunk) {
        return chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
    }

    static public Chunk deserializeChunk(@NotNull String chunk) {
        String[] chunkParts = chunk.split(",");
        if (chunkParts.length != 3) return null;
        World world = Bukkit.getWorld(chunkParts[0]);
        if (world == null) return null;
        return world.getChunkAt(Integer.parseInt(chunkParts[1]), Integer.parseInt(chunkParts[2]));
    }

    private void setupDatabase() {
        if (nconfig.isDatabaseEnabled()) {
            try {
                String dbType = nconfig.getDatabaseType().toLowerCase();
                if ("mysql".equals(dbType)) {
                    mySQLManager = new MySQLManager(nconfig);
                    databaseManager = mySQLManager;
                    Util.log("&aInitializing MySQL connection...");
                } else if ("sqlite".equals(dbType)) {
                    sqLiteManager = new SQLiteManager(nconfig);
                    databaseManager = sqLiteManager;
                    Util.log("&aInitializing SQLite connection...");
                } else {
                    throw new IllegalArgumentException("Unsupported database type: " + dbType);
                }

                int claimCount = databaseManager.getClaimCount();
                int userCount = databaseManager.getUserCount();
                Util.log("&aFound " + claimCount + " claims and " + userCount + " users in database");

                Util.log("&a" + dbType.toUpperCase() + " connection established successfully!");
            } catch (Exception e) {
                nconfig.setDatabaseEnabled(false);
                nconfig.save();
                Util.log("&cDatabase connection failed (Falling back to YAML): " + e.getMessage());
            }
        }
    }

    private void checkForMigrationOpportunity() {
        File claimsFile = new File(getDataFolder(), "claims.yml");
        File playersFolder = new File(getDataFolder(), "players");

        boolean hasClaimData = claimsFile.exists();
        boolean hasUserData = false;
        if(playersFolder.exists()) {
            File[] playerFiles = playersFolder.listFiles();
            hasUserData = playerFiles != null && playerFiles.length > 0;
        }

        if (hasClaimData || hasUserData) {
            String dbType = nconfig.getDatabaseType().toUpperCase();
            Util.log("&eEmpty " + dbType + " database detected but YAML data exists.");
        }
    }

    public Economy getEconomy() {
        return econ;
    }

    public void checkForUpdates() {
        if (configManager.getBoolean("check_for_updates", true)) {
            new UpdateChecker(this, 122527).getVersion(version -> {
                if (this.getDescription().getVersion().equals(version)) {
                    Util.log("&aPlugin is up-to-date.");
                } else {
                    Util.log("&fThere is a new version update. (&e" + version + "&f)");
                }
            });
        }
    }
}