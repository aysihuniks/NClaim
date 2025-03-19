package nesoi.network.NClaim.executors;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.commands.*;
import nesoi.network.NClaim.menus.claim.admin.AdminMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.DAPI.HexUtil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdminCommandExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.wrong-usage"));
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
            case "change":
                new Change().execute(player, args);
                break;
            case "menu":
                new AdminMenu(player);
                break;
            case "help":
            case "?":
                sendAdminCommands(player);
                break;
            default:
                player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.wrong-usage"));
                break;
        }

        return true;
    }

    public void sendAdminCommands(Player player) {

        if (!player.hasPermission("nclaim.admin")) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
            return;
        }

        List<String> helpMessages = Stream.of(
                "",
                "{BROWN}NClaim Admin Commands",
                " {YELLOW}/nclaim admin change lang <lang> {GRAY}- {WHITE}Change plugin language.",
                " {YELLOW}/nclaim admin add balance <amount> <player> {GRAY}- {WHITE}Add balance to player's account.",
                " {YELLOW}/nclaim admin remove balance <amount> <player> {GRAY}- {WHITE}Remove balance from player's account.",
                " {YELLOW}/nclaim admin add blacklisted-world <world> - {WHITE}Add blacklisted world",
                " {YELLOW}/nclaim admin remove blacklisted-world <world> - {WHITE}Remove blacklisted world",
                " {YELLOW}/nclaim admin reload {GRAY}- {WHITE}Reload config files.",
                " {YELLOW}/nclaim admin help/? {GRAY}- {WHITE}Show this help message.",
                ""
        ).map(HexUtil::parse).collect(Collectors.toList());

        for (String messages : helpMessages) {
            player.sendMessage(messages);
        }
    }
}
