package nesoi.network.NClaim.admin.commands;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.models.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Reload {

    public void execute(Player player, String[] args) {
        if (args.length < 2) return;
        if (!player.hasPermission("nclaim.reload") || !player.hasPermission("nclaim.admin")) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.dont-have-a-permission"));
            return;
        }
        for(Player p : Bukkit.getOnlinePlayers()){
            PlayerDataManager playerDataManager = NCoreMain.pdCache.get(p);
            playerDataManager.saveChanges();
        }
        NCoreMain.inst().updateVariables();
        player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.reload"));
    }
}
