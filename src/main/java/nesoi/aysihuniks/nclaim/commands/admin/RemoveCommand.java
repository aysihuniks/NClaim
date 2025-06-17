package nesoi.aysihuniks.nclaim.commands.admin;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import nesoi.aysihuniks.nclaim.enums.Balance;
import nesoi.aysihuniks.nclaim.model.User;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveCommand extends BaseCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(NClaim.inst().getLangManager().getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.remove")) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.wrong_usage"));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "balance":
                handleBalanceRemove(player, args);
                break;
            case "blacklisted_world":
                handleBlacklistedWorldRemove(player, args[2]);
                break;
            case "blacklisted_region":
                handleBlacklistedRegionRemove(player, args[2]);
                break;
            case "blockvalue":
                handleBlockValueRemove(player, args);
                break;
            default:
                player.sendMessage(NClaim.inst().getLangManager().getString("command.wrong_usage"));
                break;
        }

        return true;
    }

    private void handleBalanceRemove(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.wrong_usage"));
            return;
        }

        try {
            int amount = Integer.parseInt(args[2]);
            String targetName = args[3];
            Player targetPlayer = NClaim.inst().getServer().getPlayer(targetName);

            if (targetPlayer == null) {
                player.sendMessage(NClaim.inst().getLangManager().getString("command.player.not_found").replace("{target}", targetName));
                return;
            }

            User user = User.getUser(targetPlayer.getUniqueId());

            if (NClaim.inst().getBalanceSystem() == Balance.VAULT) {
                double currentValue = NClaim.inst().getEconomy().getBalance(targetPlayer);
                double withdrawAmount = Math.min(currentValue, amount);
                NClaim.inst().getEconomy().withdrawPlayer(targetPlayer, withdrawAmount);
                double newBalance = NClaim.inst().getEconomy().getBalance(targetPlayer);

                targetPlayer.sendMessage(NClaim.inst().getLangManager().getString("command.remove.target_removed").replace("{amount}", String.valueOf(withdrawAmount)).replace("{balance}", String.valueOf(newBalance)));
                player.sendMessage(NClaim.inst().getLangManager().getString("command.remove.player_removed").replace("{amount}", String.valueOf(withdrawAmount)).replace("{target}", targetName));
            } else {
                user.setBalance(Math.max(0, user.getBalance() - amount));
                targetPlayer.sendMessage(NClaim.inst().getLangManager().getString("command.remove.target_removed").replace("{amount}", String.valueOf(amount)).replace("{balance}", String.valueOf(user.getBalance())));
                player.sendMessage(NClaim.inst().getLangManager().getString("command.remove.player_removed").replace("{amount}", String.valueOf(amount)).replace("{target}", targetName));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.enter_a_valid_number"));
        }
    }

    private void handleBlacklistedWorldRemove(Player player, String worldName) {
        List<String> blacklistedWorlds = NClaim.inst().getNconfig().getBlacklistedWorlds();
        if (!blacklistedWorlds.contains(worldName)) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.remove.not_in_blacklist").replace("{world}", worldName));
            return;
        }

        blacklistedWorlds.remove(worldName);
        NClaim.inst().getNconfig().setBlacklistedWorlds(blacklistedWorlds);
        NClaim.inst().getNconfig().save();

        player.sendMessage(NClaim.inst().getLangManager().getString("command.remove.world_removed_from_blacklist").replace("{world}", worldName));
    }

    private void handleBlacklistedRegionRemove(Player player, String regionName) {
        if (!NClaim.inst().isWorldGuardEnabled()) return;

        List<String> blacklistedRegions = NClaim.inst().getNconfig().getBlacklistedRegions();
        if (!blacklistedRegions.contains(regionName)) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.remove.region_not_in_blacklist")
                    .replace("{region}", regionName));
            return;
        }

        blacklistedRegions.remove(regionName);
        NClaim.inst().getNconfig().setBlacklistedRegions(blacklistedRegions);
        NClaim.inst().getNconfig().save();

        player.sendMessage(NClaim.inst().getLangManager().getString("command.remove.region_removed_from_blacklist")
                .replace("{region}", regionName));
    }

    private void handleBlockValueRemove(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.wrong_usage"));
            return;
        }

        String materialName = args[2].toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.invalid_material")
                    .replace("{material}", materialName));
            return;
        }

        if (!NClaim.inst().getBlockValueManager().isValidBlock(material)) {
            player.sendMessage(NClaim.inst().getLangManager().getString("command.remove.block_not_found")
                    .replace("{block}", material.name()));
            return;
        }

        NClaim.inst().getBlockValueManager().removeBlockValue(material);
        player.sendMessage(NClaim.inst().getLangManager().getString("command.remove.block_value_removed")
                .replace("{block}", material.name()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("nclaim.remove")) {
            return null;
        }

        if (args.length == 2) {
            List<String> options = new ArrayList<>(Arrays.asList("balance", "blacklisted_world", "blockvalue"));

            if (NClaim.inst().isWorldGuardEnabled()) {
                options.add("blacklisted_region");
            }

            return options;
        }

        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("blacklisted_world")) {
                return NClaim.inst().getNconfig().getBlacklistedWorlds();
            } else if (args[1].equalsIgnoreCase("blacklisted_region") && NClaim.inst().isWorldGuardEnabled()) {
                return NClaim.inst().getNconfig().getBlacklistedRegions();
            } else if (args[1].equalsIgnoreCase("blockvalue")) {
                return NClaim.inst().getBlockValueManager().getBlockValues().keySet().stream()
                        .map(Material::name)
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