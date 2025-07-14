package nesoi.aysihuniks.nclaim.commands.admin;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import nesoi.aysihuniks.nclaim.enums.Balance;
import nesoi.aysihuniks.nclaim.model.User;
import nesoi.aysihuniks.nclaim.utils.LangManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.message.ChannelType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SetCommand extends BaseCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        LangManager langManager = NClaim.inst().getLangManager();

        if (!(sender instanceof Player)) {
            ChannelType.CHAT.send(sender, langManager.getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.set") && !player.hasPermission("nclaim.admin")) {
            ChannelType.CHAT.send(player, langManager.getString("command.permission.denied"));
            return true;
        }

        if (args.length < 3) {
            ChannelType.CHAT.send(player, langManager.getString("command.wrong_usage"));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "balance":
                handleBalanceSet(player, args);
                break;
            case "blockvalue":
                handleBlockValueSet(player, args);
                break;
            default:
                ChannelType.CHAT.send(player, langManager.getString("command.wrong_usage"));
                break;
        }

        return true;
    }

    private void handleBalanceSet(Player player, String[] args) {
        if (args.length < 4) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.wrong_usage"));
            return;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            String targetName = args[3];
            Player targetPlayer = NClaim.inst().getServer().getPlayer(targetName);

            if (targetPlayer == null) {
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.player.not_found")
                        .replace("{target}", targetName));
                return;
            }

            User user = User.getUser(targetPlayer.getUniqueId());
            if (user == null) {
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.player_data_not_found"));
                return;
            }

            if (NClaim.inst().getBalanceSystem() == Balance.VAULT) {
                double currentBalance = NClaim.inst().getEconomy().getBalance(targetPlayer);
                NClaim.inst().getEconomy().withdrawPlayer(targetPlayer, currentBalance);
                NClaim.inst().getEconomy().depositPlayer(targetPlayer, amount);
                
                ChannelType.CHAT.send(targetPlayer, NClaim.inst().getLangManager().getString("command.set.target_balance_set")
                        .replace("{balance}", String.valueOf(amount)));
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.set.player_balance_set")
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{target}", targetName));
            } else {
                user.setBalance(amount);
                ChannelType.CHAT.send(targetPlayer, NClaim.inst().getLangManager().getString("command.set.target_balance_set")
                        .replace("{balance}", String.valueOf(amount)));
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.set.player_balance_set")
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{target}", targetName));
            }
        } catch (NumberFormatException e) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.enter_a_valid_number"));
        }
    }

    private void handleBlockValueSet(Player player, String[] args) {
        if (args.length < 4) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.wrong_usage"));
            return;
        }

        try {
            String materialName = args[2].toUpperCase();
            int value = Integer.parseInt(args[3]);
        
        if (value <= 0) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.enter_a_valid_number"));
            return;
        }

        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.invalid_material")
                    .replace("{material}", materialName));
            return;
        }

        if (!materialName.endsWith("_BLOCK") && !materialName.contains("ORE")) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.invalid_block_type")
                    .replace("{material}", materialName));
            return;
        }

        NClaim.inst().getBlockValueManager().setBlockValue(material, value);
        
        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.set.block_value_set")
                .replace("{block}", material.name())
                .replace("{value}", String.valueOf(value)));

    } catch (NumberFormatException e) {
        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.enter_a_valid_number"));
    }
}

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player) || (!sender.hasPermission("nclaim.set") && !sender.hasPermission("nclaim.admin"))) {
            return null;
        }

        if (args.length == 2) {
            return Arrays.asList("balance", "blockvalue");
        }

        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("blockvalue")) {
                return Arrays.stream(Material.values())
                        .map(Material::name)
                        .filter(name -> name.endsWith("_BLOCK") || name.contains("ORE"))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4 && args[1].equalsIgnoreCase("balance")) {
            return NClaim.inst().getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }

        return null;
    }
}