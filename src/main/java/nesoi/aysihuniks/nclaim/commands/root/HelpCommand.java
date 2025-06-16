package nesoi.aysihuniks.nclaim.commands.root;

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

        if (!player.hasPermission("nclaim.help") || !player.hasPermission("nclaim.admin") || !player.hasPermission("nclaim.use")) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        String[] helpMessage = {
            "",
            "{BROWN}NClaim All General Commands",
            " {YELLOW}/nclaim balance {GRAY}- {WHITE}Show the player balance.",
            " {YELLOW}/nclaim about {GRAY}- {WHITE}Show information about this plugin.",
            " {YELLOW}/nclaim admin {GRAY}- {WHITE}Show all admin commands.",
            " {YELLOW}/nclaim help/? {GRAY}- {WHITE}Send this help messages.",
            " {YELLOW}/nclaim {GRAY}- {WHITE}Open the Claim menu.",
            ""
        };

        for (String line : helpMessage) {
            player.sendMessage(HexUtil.parse(line));
        }

        return true;
    }
}