package nesoi.aysihuniks.nclaim.model;

import lombok.Getter;
import lombok.Setter;
import nesoi.aysihuniks.nclaim.NClaim;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.nandayo.dapi.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class User {
    private static final Collection<User> users = new ArrayList<>();
    private final UUID uuid;
    private double balance;
    private String skinTexture;
    private final Collection<Claim> playerClaims;
    private final Collection<Claim> coopClaims;

    public User(UUID uuid, double balance, String skinTexture, Collection<Claim> playerClaims, Collection<Claim> coopClaims) {
        this.uuid = uuid;
        this.balance = balance;
        this.skinTexture = skinTexture;
        this.playerClaims = playerClaims;
        this.coopClaims = coopClaims;
        users.add(this);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void addBalance(double amount) {
        this.balance += amount;
    }

    public static User getUser(UUID uuid) {
        return users.stream()
                .filter(user -> user.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public static void loadUser(UUID uuid) {
        if (getUser(uuid) != null) return;

        if (NClaim.inst().getNconfig().isDatabaseEnabled() && NClaim.inst().getMySQLManager() != null) {
            loadFromDatabase(uuid);
        } else {
            loadFromYaml(uuid);
        }
    }

    private static void loadFromDatabase(UUID uuid) {
        User user = NClaim.inst().getMySQLManager().loadUser(uuid);
        if (user == null) {
            user = createNewUser(uuid);
        }
        updateClaimCollections(user);
    }

    private static void loadFromYaml(UUID uuid) {
        File folder = new File(NClaim.inst().getDataFolder(), "players");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, uuid + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        double balance = config.getDouble("balance");
        String skinTexture = config.getString("skinTexture", null);

        User user = new User(uuid, balance, skinTexture, new ArrayList<>(), new ArrayList<>());
        updateClaimCollections(user);
    }

    private static User createNewUser(UUID uuid) {
        return new User(uuid, 0.0, null, new ArrayList<>(), new ArrayList<>());
    }

    private static void updateClaimCollections(User user) {
        user.getPlayerClaims().clear();
        user.getCoopClaims().clear();

        user.getPlayerClaims().addAll(Claim.getClaims().stream()
                .filter(c -> c.getOwner().equals(user.getUuid()))
                .collect(Collectors.toList()));

        user.getCoopClaims().addAll(Claim.getClaims().stream()
                .filter(c -> c.getCoopPlayers().contains(user.getUuid()))
                .collect(Collectors.toList()));
    }

    public static void saveUser(UUID uuid) {
        User user = getUser(uuid);
        if (user == null) return;

        if (NClaim.inst().getNconfig().isDatabaseEnabled()) {
            NClaim.inst().getMySQLManager().saveUser(user);
        } else {
            saveToYaml(user);
        }
    }

    private static void saveToYaml(User user) {
        File folder = new File(NClaim.inst().getDataFolder(), "players");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, user.getUuid() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("balance", user.getBalance());
        config.set("skinTexture", user.getSkinTexture());

        try {
            config.save(file);
        } catch (Exception e) {
            Util.log("&cFailed to save user data to YAML: " + e.getMessage());
        }
    }
}