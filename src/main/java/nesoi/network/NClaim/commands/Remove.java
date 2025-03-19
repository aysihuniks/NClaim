package nesoi.network.NClaim.commands;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.enums.Balance;
import nesoi.network.NClaim.model.User;
import nesoi.network.NClaim.utils.ConfigManager;
import nesoi.network.NClaim.utils.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

import static nesoi.network.NClaim.NCoreMain.economy;

public class Remove {

    public void execute(Player player, String[] args) {
        LangManager langManager = NCoreMain.inst().langManager;

        if (args.length < 3) {
            player.sendMessage(langManager.getMsg("messages.error.wrong-usage"));
            return;
        }

        if (!player.hasPermission("nclaim.remove") || !player.hasPermission("nclaim.admin")) {
            player.sendMessage(langManager.getMsg("messages.error.not-enough-permission"));
            return;
        }

        String value = args[2];

        if (value.equalsIgnoreCase("blacklisted-world")) {
            if (args.length < 4) {
                player.sendMessage(langManager.getMsg("messages.error.wrong-usage"));
                return;
            }

            String worldName = args[3];
            ConfigManager configManager = NCoreMain.inst().configManager;

            List<String> blacklistedWorlds = configManager.getStringList("blacklisted-worlds");

            if (!blacklistedWorlds.contains(worldName)) {
                player.sendMessage(langManager.getMsg("messages.error.not-in-blacklist", worldName));
                return;
            }

            blacklistedWorlds.remove(worldName);
            configManager.set("blacklisted-worlds", blacklistedWorlds);
            configManager.saveConfig();

            player.sendMessage(langManager.getMsg("messages.success.removed-from-blacklist", worldName));
            return;
        }

        if (value.equalsIgnoreCase("balance")) {
            if (args.length < 4) {
                player.sendMessage(langManager.getMsg("messages.error.wrong-usage"));
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(langManager.getMsg("messages.error.wrong-usage"));
                return;
            }

            Player target = Bukkit.getPlayerExact(args[4]);

            if (target == null) target = player;

            User user = User.getUser(target.getUniqueId());

            if (amount <= 0) {
                player.sendMessage(langManager.getMsg("messages.error.enter-a-number"));
                return;
            }

            if (NCoreMain.inst().balanceSystem == Balance.VAULT) {
                double currentValue = economy.getBalance(target);
                double withdrawAmount = Math.min(currentValue, amount);
                economy.withdrawPlayer(target, withdrawAmount);
                double newBalance = economy.getBalance(target);

                target.sendMessage(langManager.getMsg("messages.success.target-balance-removed", amount, newBalance));
                player.sendMessage(langManager.getMsg("messages.success.player-balance-removed", amount, target.getName()));
            } else {
                user.setBalance(Math.max(0, user.getBalance() - amount));

                target.sendMessage(langManager.getMsg("messages.success.target-balance-removed", amount, user.getBalance()));
                player.sendMessage(langManager.getMsg("messages.success.player-balance-removed", amount, target.getName()));
            }
            return;
        }

        player.sendMessage(langManager.getMsg("messages.error.wrong-usage"));
    }
}
