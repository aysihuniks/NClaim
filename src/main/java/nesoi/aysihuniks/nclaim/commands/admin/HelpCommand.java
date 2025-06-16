package nesoi.aysihuniks.nclaim.commands.admin;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nandayo.dapi.HexUtil;

public class HelpCommand extends BaseCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(NClaim.inst().getLangManager().getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.admin")) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        String[] helpMessage = {
            "",
            "{BROWN}NClaim Admin Commands",
            " {YELLOW}/nclaim admin change lang <lang> {GRAY}- {WHITE}Change plugin language.",
            " {YELLOW}/nclaim admin add balance <amount> <player> {GRAY}- {WHITE}Add balance to player's account.",
            " {YELLOW}/nclaim admin add blockvalue <material> <value> {GRAY}- {WHITE}Add block to value list.",
            " {YELLOW}/nclaim admin add blacklisted_region <region> {GRAY}- {WHITE}Add block to value list.",
            " {YELLOW}/nclaim admin add blacklisted_world <world> - {WHITE}Add blacklisted world",
            " {YELLOW}/nclaim admin remove balance <amount> <player> {GRAY}- {WHITE}Remove balance from player's account.",
            " {YELLOW}/nclaim admin remove blacklisted-world <world> - {WHITE}Remove blacklisted world",
            " {YELLOW}/nclaim admin reload {GRAY}- {WHITE}Reload config files.",
            " {YELLOW}/nclaim admin help/? {GRAY}- {WHITE}Show this help message.",
            ""
        };

        for (String line : helpMessage) {
            player.sendMessage(HexUtil.parse(line));
        }

        return true;
    }
}