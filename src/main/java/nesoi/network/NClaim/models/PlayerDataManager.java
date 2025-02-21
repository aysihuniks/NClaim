package nesoi.network.NClaim.models;

import nesoi.network.NClaim.NCoreMain;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerDataManager {

    private final File file;
    private final FileConfiguration config;
    private Player player;
    private double balance;
    private OfflinePlayer offlinePlayer;

    public PlayerDataManager(Player player) {
        this.file = new File(NCoreMain.inst().getDataFolder(), "players/" + player.getUniqueId() + ".yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.player = player;
        initializeData();
    }

    public PlayerDataManager(OfflinePlayer offlinePlayer) {
        this.file = new File(NCoreMain.inst().getDataFolder(), "players/" + offlinePlayer.getUniqueId() + ".yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.offlinePlayer = offlinePlayer;
        initializeData();
    }

    public static PlayerDataManager getData(String target) {
        if(Bukkit.getPlayer(target) != null) {
            return NCoreMain.pdCache.get(Bukkit.getPlayer(target));
        }else {
            return new PlayerDataManager(Bukkit.getOfflinePlayer(UUID.fromString(target)));
        }
    }

    private void initializeData() {
        this.balance = config.getDouble("balance", 0);
    }

    public void saveChanges() {
        config.set("name", getName());
        config.set("balance", getBalance());
        try {
            config.save(file);
        } catch (IOException e) {
            NCoreMain.inst().getLogger().warning("Could not save data for player: " + getName());
        }
    }

    public Player getPlayer() {
        return player;
    }
    public String getName() {
        return player != null ? player.getName() : offlinePlayer.getName();
    }
    public UUID getUniqueId() {
        return player != null ? player.getUniqueId() : offlinePlayer.getUniqueId();
    }

    public double getBalance() {
        return this.balance;
    }

    public void setBalance(double amount) {
        this.balance = amount;
    }

}
