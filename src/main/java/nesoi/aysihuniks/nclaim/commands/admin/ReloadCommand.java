package nesoi.aysihuniks.nclaim.commands.admin;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand extends BaseCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.reload")) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        try {
            NClaim.inst().reloadPlugin();
            player.sendMessage(NClaim.inst().getLangManager().getString("command.reload.success"));
        } catch (Exception e) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.reload.failed"));
            e.printStackTrace();
        }

        return true;
    }
}