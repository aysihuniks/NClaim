package nesoi.aysihuniks.nclaim.commands.root;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.util.HexUtil;
import org.nandayo.dapi.message.ChannelType;

public class HelpCommand extends BaseCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            ChannelType.CHAT.send(sender, NClaim.inst().getLangManager().getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.help") && !player.hasPermission("nclaim.use")) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        String[] helpMessage = {
            "",
            "{BROWN}NClaim All General Commands",
            " {YELLOW}/nclaim balance {GRAY}- {WHITE}Show the player balance.",
            " {YELLOW}/nclaim about {GRAY}- {WHITE}Show information about this plugin.",
            " {YELLOW}/nclaim admin {GRAY}- {WHITE}Show all admin commands.",
            " {YELLOW}/nclaim level {GRAY}- {WHITE}Show the level of the claim.",
            " {YELLOW}/nclaim help/? {GRAY}- {WHITE}Send this help messages.",
            " {YELLOW}/nclaim {GRAY}- {WHITE}Open the Claim menu.",
            ""
        };

        for (String line : helpMessage) {
            ChannelType.CHAT.send(player, HexUtil.parse(line));
        }

        return true;
    }
}