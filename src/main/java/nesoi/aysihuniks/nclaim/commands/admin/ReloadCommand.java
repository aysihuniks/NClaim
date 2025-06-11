package nesoi.network.NClaim.commands;

import nesoi.network.NClaim.NCoreMain;
import org.bukkit.entity.Player;

public class Reload {

    public void execute(Player player, String[] args) {
        if (args.length < 2) return;
        if (!player.hasPermission("nclaim.reload") || !player.hasPermission("nclaim.admin")) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
            return;
        }
        NCoreMain.inst().updateVariables();
        player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.success.reload"));
    }
}
