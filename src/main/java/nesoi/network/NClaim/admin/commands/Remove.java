package nesoi.network.NClaim.admin.commands;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.models.ClaimDataManager;
import nesoi.network.NClaim.models.PlayerDataManager;
import nesoi.network.NClaim.utils.ConfigManager;
import nesoi.network.NClaim.utils.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.List;

import static nesoi.network.NClaim.NCoreMain.economy;

public class Remove {

    public void execute(Player player, String[] args) {
        LangManager langManager = NCoreMain.inst().langManager;

        if (args.length < 3) {
            player.sendMessage(langManager.getMsg("messages.wrong-usage"));
            return;
        }

        if (!player.hasPermission("nclaim.remove") || !player.hasPermission("nclaim.admin")) {
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

            if (!blacklistedWorlds.contains(worldName)) {
                player.sendMessage(langManager.getMsg("messages.world-not-in-blacklist", worldName));
                return;
            }

            blacklistedWorlds.remove(worldName);
            configManager.set("blacklisted-worlds", blacklistedWorlds);
            configManager.saveConfig();

            player.sendMessage(langManager.getMsg("messages.world-removed-from-blacklist", worldName));
            return;
        }

        if (value.equalsIgnoreCase("balance")) {
            if (args.length < 4) {
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

            Player target = (args.length > 4) ? Bukkit.getPlayerExact(args[4]) : player;

            if (amount <= 0) {
                player.sendMessage(langManager.getMsg("messages.enter-a-valid-number"));
                return;
            }

            String moneyData = NCoreMain.inst().configManager.getString("money-data", "PlayerData");
            if (moneyData.equals("Vault")) {
                double currentValue = economy.getBalance(target);
                double withdrawAmount = Math.min(currentValue, amount);
                economy.withdrawPlayer(target, withdrawAmount);
                double newBalance = economy.getBalance(target);

                target.sendMessage(langManager.getMsg("messages.balance-removed-to-target", amount, newBalance));
                player.sendMessage(langManager.getMsg("messages.balance-removed-successfully", amount, target.getName()));
            } else if (moneyData.equals("PlayerData")) {
                PlayerDataManager playerDataManager = NCoreMain.pdCache.get(target);
                double currentValue = playerDataManager.getBalance();

                double newBalance = Math.max(0, currentValue - amount);
                playerDataManager.setBalance(newBalance);

                target.sendMessage(langManager.getMsg("messages.balance-removed-to-target", amount, newBalance));
                player.sendMessage(langManager.getMsg("messages.balance-removed-successfully", amount, target.getName()));
            } else {
                player.sendMessage(langManager.getMsg("messages.setup-your-config-file"));
            }
            return;
        }

        if (value.equalsIgnoreCase("claim-expiration-date")) {
            if (args.length < 6) {
                player.sendMessage(langManager.getMsg("messages.wrong-usage"));
                return;
            }

            int days, hours, minutes;
            try {
                days = Integer.parseInt(args[3]);
                hours = Integer.parseInt(args[4]);
                minutes = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                player.sendMessage(langManager.getMsg("messages.invalid-number-format"));
                return;
            }

            ClaimDataManager claimDataManager = NCoreMain.inst().claimDataManager;
            claimDataManager.subtractExpirationDate(player, days, hours, minutes);
            player.sendMessage(langManager.getMsg("messages.expiration-date-successfully-subtracted", days, hours, minutes));
            return;
        }

        player.sendMessage(langManager.getMsg("messages.enter-a-valid-data"));
    }
}
