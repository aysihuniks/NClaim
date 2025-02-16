package nesoi.network.NClaim.executors;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.admin.commands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.DAPI.HexUtil;

import java.util.List;
import java.util.stream.Stream;

public class AdminCommandExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 1) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.wrong-usage"));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "reload":
                new Reload().execute(player, args);
                break;
            case "add":
                new Add().execute(player, args);
                break;
            case "remove":
                new Remove().execute(player, args);
                break;
            case "chunkinfo":
                new ChunkInfo().execute(player, args);
                break;
            case "delete":
                new Delete().execute(player, args);
                break;
            case "change":
                new Change().execute(player, args);
                break;
            case "help":
            case "?":
                sendAdminCommands(player);
                break;
            default:
                player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.wrong-usage"));
                break;
        }

        return true;
    }

    public void sendAdminCommands(Player player) {

        if (!player.hasPermission("nclaim.admin")) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.dont-have-a-permission"));
            return;
        }

        List<String> helpMessages = Stream.of(
                "",
                "{BROWN}NClaim Admin Commands",
                " {YELLOW}/nclaim admin add balance <amount> <player> {GRAY}- {WHITE}Add balance to player's account.",
                " {YELLOW}/nclaim admin add claim-expiration-date <days> <hours> <minutes> {GRAY}- {WHITE}Extend claim expiration time.",
                " {YELLOW}/nclaim admin remove balance <amount> <player> {GRAY}- {WHITE}Remove balance from player's account.",
                " {YELLOW}/nclaim admin remove claim-expiration-date <days> <hours> <minutes> {GRAY}- {WHITE}Reduce claim expiration time.",
                " {YELLOW}/nclaim admin delete <claim> {GRAY}- {WHITE}Delete claim in current chunk.",
                " {YELLOW}/nclaim admin reload {GRAY}- {WHITE}Reload config files.",
                " {YELLOW}/nclaim admin chunkinfo {GRAY}- {WHITE}Show current chunk info (x, z) (for dev).",
                " {YELLOW}/nclaim admin help/? {GRAY}- {WHITE}Show this help message.",
                ""
        ).map(HexUtil::parse).toList();

        for (String messages : helpMessages) {
            player.sendMessage(messages);
        }
    }
}
