package nesoi.aysihuniks.nclaim.commands.root;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LevelCommand extends BaseCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(NClaim.inst().getLangManager().getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.level")) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("queue")) {
            NClaim.inst().getBlockValueManager().checkQueueStatus(player.getUniqueId());
            return true;
        }

        Claim claim = Claim.getClaim(player.getLocation().getChunk());

        if (claim == null) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.not_in_claim"));
            return true;
        }

        if (!claim.getOwner().equals(player.getUniqueId()) && !player.hasPermission("nclaim.admin")) {
            player.sendMessage(NClaim.inst().getLangManager().getString("claim.not_yours"));
            return true;
        }

        NClaim.inst().getBlockValueManager().requestClaimCalculation(
                player.getUniqueId(),
                player.getName(),
                claim
        );

        return true;
    }
}