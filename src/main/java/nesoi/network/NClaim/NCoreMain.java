package nesoi.network.NClaim;

import nesoi.network.NClaim.executors.AdminCommandExecutor;
import nesoi.network.NClaim.executors.MainCommandExecutor;
import nesoi.network.NClaim.integrations.Metrics;
import nesoi.network.NClaim.models.ClaimDataManager;
import nesoi.network.NClaim.models.PlaceholderManager;
import nesoi.network.NClaim.models.PlayerDataManager;
import nesoi.network.NClaim.systems.claim.ClaimManager;
import nesoi.network.NClaim.utils.UpdateChecker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.nandayo.DAPI.DAPI;
import org.nandayo.DAPI.HexUtil;

import java.util.HashMap;
import java.util.Map;

import static org.nandayo.DAPI.HexUtil.parse;
import static org.nandayo.DAPI.Util.log;

public final class NCoreMain extends JavaPlugin implements Listener {

    private static NCoreMain instance;
    public static NCoreMain inst() {
        return instance;
    }

    public static HashMap<Player, PlayerDataManager> pdCache = new HashMap<>();
    public ClaimDataManager claimDataManager;
    public Config config;
    public Messages messages;

    @Override
    public void onEnable() {
        instance = this;
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(new ClaimManager(), this);

        getCommand("nclaim").setExecutor(new MainCommandExecutor());
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        //UPDATE VALUES
        Bukkit.getOnlinePlayers().forEach(this::loadCache);
        updateVariables();
        setupEconomy();

        // APIS
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderManager(this).register();
        }
        new AdminCommandExecutor();

        //UPDATE CHECK
        if(config.getBoolean("check_for_updates")) {
            //SPIGOT RESOURCE ID
            int resourceId = 122527;
            new UpdateChecker(this, resourceId).getVersion(version -> {
                if (this.getDescription().getVersion().equals(version)) {
                    log("&8[&6NClaim&8] &aPlugin is up-to-date.");
                } else {
                    log("&8[&6NClaim&8] &fThere is a new version update. (&e" + version + "&f)");
                }
            });
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                claimDataManager.checkExpiredClaims();
            }
        }.runTaskTimer(NCoreMain.inst(), 0L, 20L * 10);

        //bStats
        int pluginId = 24693;
        new Metrics(this, pluginId);

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

    // DIPNOTE: elle değişilmeyenleri config.set yapmana gerek yok.
    public void updateVariables() {
        if(claimDataManager != null) {
            claimDataManager.saveAllChanges();
        }
        claimDataManager = new ClaimDataManager();
        config = new Config();
        messages = new Messages();
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(this::unloadCache);
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        loadCache(e.getPlayer());
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        unloadCache(e.getPlayer());
    }

    private void loadCache(Player p) {
        if (!pdCache.containsKey(p)) {
            pdCache.put(p, new PlayerDataManager(p));
        }

    }

    private void unloadCache(Player p) {
        if (pdCache.containsKey(p)) {
            pdCache.get(p).saveChanges();
            pdCache.remove(p);
        }
    }

    // utils

    // action bar sender
    public static void sendActionBar(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        String formattedText = parse(message);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formattedText));

    }

    // cooldown manager
    private static final Map<Player, Long> cooldowns = new HashMap<>();
    private static final Map<Player, Long> messageCooldowns = new HashMap<>();

    public static boolean isInCooldown(Player player, long cooldownTimeMillis) {
        long currentTime = System.currentTimeMillis();

        if (cooldowns.containsKey(player)) {
            long lastClickTime = cooldowns.get(player);
            if (currentTime - lastClickTime < cooldownTimeMillis) {
                return true;
            }
        }

        cooldowns.put(player, currentTime);
        return false;
    }

    public static boolean shouldSendMessage(Player player, long messageCooldownTimeMillis) {
        long currentTime = System.currentTimeMillis();

        if (messageCooldowns.containsKey(player)) {
            long lastMessageTime = messageCooldowns.get(player);
            if (currentTime - lastMessageTime < messageCooldownTimeMillis) {
                return false;
            }
        }

        messageCooldowns.put(player, currentTime);
        return true;
    }


}
