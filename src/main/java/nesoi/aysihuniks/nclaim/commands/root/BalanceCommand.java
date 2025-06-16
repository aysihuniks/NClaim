package nesoi.aysihuniks.nclaim.commands.root;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import nesoi.aysihuniks.nclaim.enums.Balance;
import nesoi.aysihuniks.nclaim.model.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand extends BaseCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(NClaim.inst().getLangManager().getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.balance") || !player.hasPermission("nclaim.use")) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        Balance balanceSystem = NClaim.inst().getBalanceSystem();
        double balance = balanceSystem == Balance.PLAYERDATA
                ? User.getUser(player.getUniqueId()).getBalance()
                : NClaim.inst().getEconomy().getBalance(player);

        player.sendMessage(NClaim.inst().getLangManager().getString("command.balance.current").replace("{amount}", String.valueOf(balance)));
        return true;
    }
}