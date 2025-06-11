package nesoi.network.NClaim.commands;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.enums.Balance;
import nesoi.network.NClaim.model.User;
import nesoi.network.NClaim.utils.ConfigManager;
import nesoi.network.NClaim.utils.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static nesoi.network.NClaim.NCoreMain.economy;


public class Add {

    public void execute(Player player, String[] args) {
        LangManager langManager = NCoreMain.inst().langManager;

        if (!player.hasPermission("nclaim.add") || !player.hasPermission("nclaim.admin")) {
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
            if (blacklistedWorlds == null) {
                blacklistedWorlds = new ArrayList<>();
            }

            if (blacklistedWorlds.contains(worldName)) {
                player.sendMessage(langManager.getMsg("messages.error.already-blacklisted", worldName));
                return;
            }

            blacklistedWorlds.add(worldName);
            configManager.set("blacklisted-worlds", blacklistedWorlds);
            configManager.saveConfig();

            player.sendMessage(langManager.getMsg("messages.success.blacklisted", worldName));
            return;
        }

        if (value.equalsIgnoreCase("balance")) {
            if (args.length < 5) {
                player.sendMessage(langManager.getMsg("messages.error.wrong-usage"));
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(langManager.getMsg("messages.error.enter-a-number"));
                return;
            }

            Player target = Bukkit.getPlayerExact(args[4]);
            if (target == null) {
                target = player;
            }

            User user = User.getUser(target.getUniqueId());

            if (amount <= 0) {
                player.sendMessage(langManager.getMsg("messages.error.enter-a-number"));
                return;
            }

            if (NCoreMain.inst().balanceSystem == Balance.VAULT) {
                economy.depositPlayer(target, amount);
                double newBalance = economy.getBalance(target);

                target.sendMessage(langManager.getMsg("messages.success.target-balance-added", amount, newBalance));
                player.sendMessage(langManager.getMsg("messages.success.player-balance-added", amount, target.getName()));
            } else {
                user.addBalance(amount);

                target.sendMessage(langManager.getMsg("messages.success.target-balance-added", amount, user.getBalance()));
                player.sendMessage(langManager.getMsg("messages.success.player-balance-added", amount, target.getName()));
            }

            return;
        }

        player.sendMessage(langManager.getMsg("messages.error.wrong-usage"));
    }

}
