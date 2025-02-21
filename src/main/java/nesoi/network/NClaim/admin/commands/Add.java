package nesoi.network.NClaim.admin.commands;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.enums.Balance;
import nesoi.network.NClaim.models.ClaimDataManager;
import nesoi.network.NClaim.models.PlayerDataManager;
import nesoi.network.NClaim.utils.ConfigManager;
import nesoi.network.NClaim.utils.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static nesoi.network.NClaim.NCoreMain.economy;


public class Add {

    public void execute(Player player, String[] args) {
        LangManager langManager = NCoreMain.inst().langManager;

        if (!player.hasPermission("nclaim.add") && !player.hasPermission("nclaim.admin")) {
            player.sendMessage(langManager.getMsg("messages.dont-have-a-permission"));
            return;
        }

        String value = args[2];

        if (value.equalsIgnoreCase("blacklisted-world")) {
            if (args.length < 4) {
                player.sendMessage(langManager.getMsg("messages.wrong-usage"));
                return;
            }

            String worldName = args[3];
            ConfigManager configManager = NCoreMain.inst().configManager;

            List<String> blacklistedWorlds = configManager.getStringList("blacklisted-worlds");
            if (blacklistedWorlds == null) {
                blacklistedWorlds = new ArrayList<>();
            }

            if (blacklistedWorlds.contains(worldName)) {
                player.sendMessage(langManager.getMsg("messages.world-already-blacklisted", worldName));
                return;
            }

            blacklistedWorlds.add(worldName);
            configManager.set("blacklisted-worlds", blacklistedWorlds);
            configManager.saveConfig();

            player.sendMessage(langManager.getMsg("messages.world-blacklisted-successfully", worldName));
            return;
        }

        if (value.equalsIgnoreCase("balance")) {
            if (args.length < 5) {
                player.sendMessage(langManager.getMsg("messages.wrong-usage"));
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(langManager.getMsg("messages.enter-a-valid-number"));
                return;
            }

            Player target = Bukkit.getPlayerExact(args[4]);
            if (target == null) {
                target = player;
            }

            if (amount <= 0) {
                player.sendMessage(langManager.getMsg("messages.enter-a-valid-number"));
                return;
            }

            if (NCoreMain.inst().balanceSystem == Balance.VAULT) {
                economy.depositPlayer(target, amount);
                double newBalance = economy.getBalance(target);

                target.sendMessage(langManager.getMsg("messages.balance-added-to-target", amount, newBalance));
                player.sendMessage(langManager.getMsg("messages.balance-added-successfully", amount, target.getName()));
            } else {
                PlayerDataManager playerDataManager = NCoreMain.pdCache.get(target);
                double currentValue = playerDataManager.getBalance();
                playerDataManager.setBalance(currentValue + amount);
                double newValue = playerDataManager.getBalance();

                target.sendMessage(langManager.getMsg("messages.balance-added-to-target", amount, newValue));
                player.sendMessage(langManager.getMsg("messages.balance-added-successfully", amount, target.getName()));
            }

            return;
        }

        player.sendMessage(langManager.getMsg("messages.enter-a-valid-data"));
    }

}
