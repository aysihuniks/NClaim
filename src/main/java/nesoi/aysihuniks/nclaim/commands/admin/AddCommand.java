package nesoi.aysihuniks.nclaim.commands.admin;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import nesoi.aysihuniks.nclaim.enums.Balance;
import nesoi.aysihuniks.nclaim.model.User;
import nesoi.aysihuniks.nclaim.utils.LangManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.message.ChannelType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AddCommand extends BaseCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        LangManager langManager = NClaim.inst().getLangManager();
        if (!(sender instanceof Player)) {
            ChannelType.CHAT.send(sender, langManager.getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.add") && !player.hasPermission("nclaim.admin")) {
            ChannelType.CHAT.send(player, langManager.getString("command.permission.denied"));
            return true;
        }

        if (args.length < 3) {
            ChannelType.CHAT.send(player, langManager.getString("command.wrong_usage"));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "balance":
                handleBalanceAdd(player, args);
                break;
            case "blacklisted_world":
                handleBlacklistedWorldAdd(player, args[2]);
                break;
            case "blacklisted_region":
                handleBlacklistedRegionAdd(player, args[2]);
                break;
            case "blockvalue":
                handleBlockValueAdd(player, args);
                break;
            default:
                ChannelType.CHAT.send(player, langManager.getString("command.wrong_usage"));
                break;
        }

        return true;
    }


    private void handleBalanceAdd(Player player, String[] args) {
        if (args.length < 4) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.wrong_usage"));
            return;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            String targetName = args[3];
            Player targetPlayer = NClaim.inst().getServer().getPlayer(targetName);

            if (targetPlayer == null) {
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.player.not_found").replace("{target}", targetName));
                return;
            }

            User user = User.getUser(targetPlayer.getUniqueId());
            if (user == null) {
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.player_data_not_found"));
                return;
            }

            if (NClaim.inst().getBalanceSystem() == Balance.VAULT) {
                NClaim.inst().getEconomy().depositPlayer(targetPlayer, amount);
                ChannelType.CHAT.send(targetPlayer, NClaim.inst().getLangManager().getString("command.add.target_added").replace("{amount}", String.valueOf(amount)).replace("{balance}", String.valueOf(NClaim.inst().getEconomy().getBalance(targetPlayer))));
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.add.player_added").replace("{amount}", String.valueOf(amount)).replace("{target}", targetName));
            } else {
                user.addBalance(amount);
                ChannelType.CHAT.send(targetPlayer, NClaim.inst().getLangManager().getString("command.add.target_added").replace("{amount}", String.valueOf(amount)).replace("{balance}", String.valueOf(user.getBalance())));
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.add.player_added").replace("{amount}", String.valueOf(amount)).replace("{target}", targetName));
            }
        } catch (NumberFormatException e) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.enter_a_valid_number"));
        }
    }

    private void handleBlacklistedWorldAdd(Player player, String worldName) {
        List<String> blacklistedWorlds = NClaim.inst().getNconfig().getBlacklistedWorlds();
        if (blacklistedWorlds == null) {
            blacklistedWorlds = new ArrayList<>();
        }

        if (blacklistedWorlds.contains(worldName)) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.add.world_already_in_blacklist").replace("{world}", worldName));
            return;
        }

        blacklistedWorlds.add(worldName);
        NClaim.inst().getNconfig().setBlacklistedWorlds(blacklistedWorlds);
        NClaim.inst().getNconfig().save();

        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.add.world_added_to_blacklist").replace("{world}", worldName));
    }

    private void handleBlacklistedRegionAdd(Player player, String regionName) {
        if (!NClaim.inst().isWorldGuardEnabled()) return;

        List<String> blacklistedRegions = NClaim.inst().getNconfig().getBlacklistedRegions();
        if (blacklistedRegions == null) {
            blacklistedRegions = new ArrayList<>();
        }

        if (blacklistedRegions.contains(regionName)) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.add.region_already_in_blacklist")
                    .replace("{region}", regionName));
            return;
        }

        blacklistedRegions.add(regionName);
        NClaim.inst().getNconfig().setBlacklistedRegions(blacklistedRegions);
        NClaim.inst().getNconfig().save();

        ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.add.region_added_to_blacklist")
                .replace("{region}", regionName));
    }


    private void handleBlockValueAdd(Player player, String[] args) {
        if (args.length < 4) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.wrong_usage"));
            return;
        }
        try {
            String materialName = args[2].toUpperCase();
            int value = Integer.parseInt(args[3]);

            if (value <= 0) {
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.invalid_value"));
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

            if (NClaim.inst().getBlockValueManager().isValidBlock(material)) {
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.add.block_already_exists")
                        .replace("{block}", material.name()));
                return;
            }

            if (!materialName.endsWith("_BLOCK") && !materialName.contains("ORE")) {
                ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.invalid_block_type")
                        .replace("{material}", materialName));
                return;
            }

            NClaim.inst().getBlockValueManager().setBlockValue(material, value);

            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.add.block_value_added")
                    .replace("{block}", material.name())
                    .replace("{value}", String.valueOf(value)));

        } catch (NumberFormatException e) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.enter_a_valid_number"));
        }
    }



    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("nclaim.add") || !sender.hasPermission("nclaim.admin")) {
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
                return NClaim.inst().getServer().getWorlds().stream()
                        .map(WorldInfo::getName)
                        .collect(Collectors.toList());
            } else if (args[1].equalsIgnoreCase("blockvalue")) {
                return Arrays.stream(Material.values())
                        .map(Material::name)
                        .filter(name -> name.endsWith("_BLOCK") || name.contains("ORE"))
                        .collect(Collectors.toList());
            } else if (args[1].equalsIgnoreCase("blacklisted_region") && NClaim.inst().isWorldGuardEnabled()) {
                try {
                    if (NClaim.inst().getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                        WorldGuard worldGuard = com.sk89q.worldguard.WorldGuard.getInstance();
                        RegionContainer container = worldGuard.getPlatform().getRegionContainer();
                        RegionManager regionManager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(((Player) sender).getWorld()));

                        if (regionManager != null) {
                            return new ArrayList<>(regionManager.getRegions().keySet());
                        }
                    }
                } catch (Exception e) {
                    return new ArrayList<>();
                }
                return new ArrayList<>();
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