package nesoi.network.NClaim.admin.commands;

import nesoi.network.NClaim.NCoreMain;
import org.bukkit.entity.Player;

public class ChunkInfo {

    public void execute(Player player, String[] args) {
        if (args.length < 2) return;
        if (!player.hasPermission("nclaim.chunkinfo") || !player.hasPermission("nclaim.admin")) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.dont-have-a-permission"));
            return;
        }

        int chunkX = player.getLocation().getChunk().getX();
        int chunkZ = player.getLocation().getChunk().getZ();
        player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.chunk-info", chunkX, chunkZ));
    }
}
