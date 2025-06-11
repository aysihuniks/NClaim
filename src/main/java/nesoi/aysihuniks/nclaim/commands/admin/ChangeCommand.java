package nesoi.network.NClaim.commands;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.utils.LangManager;
import org.bukkit.entity.Player;

public class Change {

    public void execute(Player player, String[] args) {
        LangManager langManager = NCoreMain.inst().langManager;
        if (args.length < 4) {
            player.sendMessage(langManager.getMsg("messages.error.wrong-usage"));
            return;
        }

        if (!player.hasPermission("nclaim.admin") || !player.hasPermission("nclaim.change")) {
            player.sendMessage(langManager.getMsg("messages.error.not-enough-permission"));
            return;
        }

        String action = args[2];
        String value = args[3];

        if (action.equalsIgnoreCase("lang")) {
            if (value.equalsIgnoreCase("tr-TR") || value.equalsIgnoreCase("en-US")) {
                NCoreMain.inst().configManager.set("lang_file", value);
                NCoreMain.inst().configManager.saveConfig();
                NCoreMain.inst().updateVariables();
                player.sendMessage(langManager.getMsg("messages.success.lang-changed", value));
            } else {
                player.sendMessage(langManager.getMsg("messages.error.invalid-lang", value));
            }
        }

        else {
            player.sendMessage(langManager.getMsg("messages.error.wrong-usage"));
        }
    }
}
