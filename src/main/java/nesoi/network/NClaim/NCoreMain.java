package nesoi.network.NClaim;

import nesoi.network.NClaim.enums.Balance;
import nesoi.network.NClaim.executors.AdminCommandExecutor;
import nesoi.network.NClaim.executors.MainCommandExecutor;
import nesoi.network.NClaim.model.Claim;
import nesoi.network.NClaim.integrations.Expension;
import nesoi.network.NClaim.model.User;
import nesoi.network.NClaim.systems.claim.ClaimManager;
import nesoi.network.NClaim.utils.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.nandayo.DAPI.DAPI;
import org.nandayo.DAPI.HexUtil;
import org.nandayo.DAPI.Util;
import org.nandayo.DAPI.object.DMaterial;
import org.nandayo.DAPI.object.DParticle;

import java.text.SimpleDateFormat;
import java.util.*;

import static nesoi.network.NClaim.utils.HeadManager.getFromName;
import static org.nandayo.DAPI.HexUtil.parse;

public final class NCoreMain extends JavaPlugin implements Listener {

    private static NCoreMain instance;
    public static NCoreMain inst() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(this, this);
        pm.registerEvents(new ClaimManager(), this);

        getCommand("nclaim").setExecutor(new MainCommandExecutor());
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        //UPDATE VALUES
        updateVariables();

        // APIS
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Expension(this).register();
        }

        if (Bukkit.getPluginManager().getPlugin("NBTAPI") != null) {
            new HeadManager();
        }

        Util.PREFIX = "&8[<#fa8443>NClaim&8]&r ";

        if(Bukkit.getPluginManager().getPlugin("DecentHolograms") == null) {
            Util.log("&cYou need the use &4DecentHologram&c to continue running this plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
        }


        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            balanceSystem = Balance.VAULT;
            setupEconomy();
        } else {
            balanceSystem = Balance.PLAYERDATA;
        }

        new AdminCommandExecutor();

        //UPDATE CHECK
        if(configManager.getBoolean("check_for_updates", true)) {
            //SPIGOT RESOURCE ID
            int resourceId = 122527;
            new UpdateChecker(this, resourceId).getVersion(version -> {
                if (this.getDescription().getVersion().equals(version)) {
                    Util.log("&aPlugin is up-to-date.");
                } else {
                    Util.log("&fThere is a new version update. (&e" + version + "&f)");
                }
            });
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Claim.checkExpiredClaims();
            }
        }.runTaskTimer(NCoreMain.inst(), 0L, 20L * 10);

        //bStats
        int pluginId = 24693;
        new Metrics(this, pluginId);

        //DAPI
        DAPI dapi = new DAPI(this);
        dapi.registerMenuListener();

        HexUtil.placeholders.put("{WHITE}", "<#FFF8E8>");
        HexUtil.placeholders.put("{DARKGREEN}", "<#0A6847>");
        HexUtil.placeholders.put("{GREEN}", "<#7ABA78>");
        HexUtil.placeholders.put("{DARKRED}", "<#6D2323>");
        HexUtil.placeholders.put("{RED}", "<#cf2525>");
        HexUtil.placeholders.put("{YELLOW}", "<#FFEC9E>");
        HexUtil.placeholders.put("{ORANGE}", "<#fa8443>");
        HexUtil.placeholders.put("{GRAY}", "<#ababab>");
        HexUtil.placeholders.put("{BROWN}", "<#825B32>");
        HexUtil.placeholders.put("{PURPLE}", "<#8D77AB>");
        HexUtil.placeholders.put("{prefix}", "&8[<#fa8443>NClaim&8]&r");

        // Claim & User Model
        Claim.loadData();

        for (Player player : Bukkit.getOnlinePlayers()) {
            User.loadUser(player.getUniqueId());
        }
    }

    public ChunkBorderManager chunkBorderManager;
    public LangManager langManager;
    public ConfigManager configManager;
    public Config config;
    public Claim claim;

    public Balance balanceSystem;


    public void updateVariables() {
        config = new Config(this).load().updateConfig();
        config.load();

        configManager = new ConfigManager(config.get());
        chunkBorderManager = new ChunkBorderManager();
        langManager = new LangManager(this, configManager.getString("lang_file", "en-US")).updateLanguage();

    }

    @Override
    public void onDisable() {
        Claim.saveData();

        for(Player player : getServer().getOnlinePlayers()) {
            User.saveUser(player.getUniqueId());
        }

        instance = null;

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (HeadManager.api) {
            HeadManager.textureMap.put(player, getFromName(player.getName()));
        }

        User.loadUser(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (HeadManager.api) {
            HeadManager.textureMap.remove(player);
        }

        User.saveUser(player.getUniqueId());
    }

    // action bar sender
    public static void sendActionBar(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        String formattedText = parse(message);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formattedText));

    }

    // Vault
    public static Economy economy = null;
    private void setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
    }

    public Economy getEconomy() {
        return economy;
    }

    // GM

    static public boolean isChunkAdjacent(@NotNull Chunk chunk, @NotNull Chunk thatChunk, int radius) {
        return Math.abs(chunk.getX() - thatChunk.getX()) <= radius && Math.abs(chunk.getZ() - thatChunk.getZ()) <= radius;
    }

    static public Material getMaterial(DMaterial dMaterial, DMaterial def) {
        Material mat = dMaterial.get();
        if(mat != null) return mat;
        return def != null ? def.get() : null;
    }

    public Particle getParticle(@NotNull DParticle dParticle, @NotNull DParticle def) {
        Particle particle = dParticle.get();
        if(particle != null) return particle;
        return def.get();
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
        if(chunkParts.length != 3) return null;
        World world = Bukkit.getWorld(chunkParts[0]);
        if (world == null) return null;
        return world.getChunkAt(Integer.parseInt(chunkParts[1]), Integer.parseInt(chunkParts[2]));
    }
}
