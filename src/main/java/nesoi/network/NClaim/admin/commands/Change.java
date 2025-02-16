package nesoi.network.NClaim.admin.commands;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.utils.LangManager;
import org.bukkit.entity.Player;

public class Change {

    public void execute(Player player, String[] args) {
        LangManager langManager = NCoreMain.inst().langManager;
        if (args.length < 4) {
            player.sendMessage(langManager.getMsg("messages.wrong-usage"));
            return;
        }

        if (!player.hasPermission("nclaim.admin") || !player.hasPermission("nclaim.change")) {
            player.sendMessage(langManager.getMsg("messages.dont-have-a-permission"));
            return;
        }

        String action = args[2];
        String value = args[3];

        if (action.equalsIgnoreCase("money-data")) {
            String moneyData = NCoreMain.inst().configManager.getString("money-data", "PlayerData");

            if (value.equalsIgnoreCase(moneyData)) {
                player.sendMessage(langManager.getMsg("messages.value-already-set", value));
                return;
            }

            if (value.equalsIgnoreCase("PlayerData")) {
                NCoreMain.inst().configManager.set("money-data", "PlayerData");
                player.sendMessage(langManager.getMsg("messages.money-data-changed", "PlayerData"));
            }
            else if (value.equalsIgnoreCase("Vault")) {
                NCoreMain.inst().configManager.set("money-data", "Vault");
                player.sendMessage(langManager.getMsg("messages.money-data-changed", "Vault"));
            }
            else {
                player.sendMessage(langManager.getMsg("messages.invalid-data", value));
            }
        }

        else if (action.equalsIgnoreCase("lang")) {
            if (value.equalsIgnoreCase("tr-TR") || value.equalsIgnoreCase("en-US")) {
                NCoreMain.inst().configManager.set("lang_file", value);
                NCoreMain.inst().configManager.saveConfig();
                NCoreMain.inst().updateVariables();
                player.sendMessage(langManager.getMsg("messages.lang-changed", value));
            } else {
                player.sendMessage(langManager.getMsg("messages.invalid-lang", value));
            }
        }

        else {
            player.sendMessage(langManager.getMsg("messages.invalid-data", action));
        }
    }
}
