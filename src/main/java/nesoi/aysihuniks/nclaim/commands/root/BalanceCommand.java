package nesoi.aysihuniks.nclaim.commands.root;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import nesoi.aysihuniks.nclaim.enums.Balance;
import nesoi.aysihuniks.nclaim.model.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.message.ChannelType;

import java.text.DecimalFormat;

public class BalanceCommand extends BaseCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            ChannelType.CHAT.send(sender, NClaim.inst().getLangManager().getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.balance") && !player.hasPermission("nclaim.use")) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        Balance balanceSystem = NClaim.inst().getBalanceSystem();
        double balance = balanceSystem == Balance.PLAYERDATA
                ? User.getUser(player.getUniqueId()).getBalance()
                : NClaim.inst().getEconomy().getBalance(player);

        DecimalFormat df = new DecimalFormat("#.##");
        String formattedBalance = df.format(balance);

        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.balance.current").replace("{balance}", formattedBalance));
        return true;
    }
}