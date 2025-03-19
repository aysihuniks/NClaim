package nesoi.network.NClaim.model;

import lombok.Getter;
import lombok.Setter;
import nesoi.network.NClaim.NCoreMain;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Getter
@Setter

public class User {

    public User(
            UUID uuid,
            double balance,
            Collection<Claim> playerClaims,
            Collection<Claim> coopClaims) {
        this.uuid = uuid;
        this.balance = balance;
        this.playerClaims = playerClaims;
        this.coopClaims = coopClaims;

        users.add(this);
    }

    private final UUID uuid;
    private Double balance;
    private final Collection<Claim> playerClaims;
    private final Collection<Claim> coopClaims;

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void addBalance(double amount) {
        this.balance += amount;
    }

    @Getter
    static private Collection<User> users = new ArrayList<>();

    static public User getUser(UUID uuid) {
        return users.stream()
                .filter(user -> user.getUuid().equals(uuid))
                .findFirst().orElse(null);
    }

    static public synchronized void loadUser(UUID uuid) {
        File folder = new File(NCoreMain.inst().getDataFolder(), "players");
        if(!folder.exists()) folder.mkdirs();

        if(getUser(uuid) != null) return;

        File file = new File(folder, uuid + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        double balance = config.getDouble("balance");
        Collection<Claim> playerClaims = new ArrayList<>(Claim.getClaims().stream()
                .filter(c -> c.getOwner().equals(uuid))
                .toList());

        Collection<Claim> coopClaims = new ArrayList<>(Claim.getClaims().stream()
                .filter(c -> c.getCoopPlayers().contains(uuid))
                .toList());

        new User(uuid, balance, playerClaims, coopClaims);
    }

    static public synchronized void saveUser(UUID uuid) {
        File folder = new File(NCoreMain.inst().getDataFolder(), "players");
        if(!folder.exists()) folder.mkdirs();

        User user = getUser(uuid);
        if (user == null) return;

        File file = new File(folder, uuid + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("balance", user.getBalance());

        User.getUsers().remove(user);

        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
