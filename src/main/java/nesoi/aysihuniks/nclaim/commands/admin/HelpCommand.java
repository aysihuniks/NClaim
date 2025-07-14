package nesoi.aysihuniks.nclaim.commands.admin;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.HexUtil;
import org.nandayo.dapi.message.ChannelType;

public class HelpCommand extends BaseCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            ChannelType.CHAT.send(sender, NClaim.inst().getLangManager().getString("command.must_be_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nclaim.admin")) {
            ChannelType.CHAT.send(player, NClaim.inst().getLangManager().getString("command.permission_denied"));
            return true;
        }

        String[] helpMessage = {
            "",
            "{BROWN}NClaim Admin Commands",
            " {YELLOW}/nclaim admin menu {GRAY}- {WHITE}Open the Admin menu.",
            " {YELLOW}/nclaim admin change lang <lang> {GRAY}- {WHITE}Change plugin language.",
            " {YELLOW}/nclaim admin change blockvalue <material> <value> {GRAY}- {WHITE}Change block value.",
            " {YELLOW}/nclaim admin add balance <amount> <player> {GRAY}- {WHITE}Add balance to player's account.",
            " {YELLOW}/nclaim admin add blockvalue <material> <value> {GRAY}- {WHITE}Add block to value list.",
            " {YELLOW}/nclaim admin add blacklisted_region <regionId> {GRAY}- {WHITE}Add region to blacklist.",
            " {YELLOW}/nclaim admin add blacklisted_world <world> - {WHITE}Add blacklisted world.",
            " {YELLOW}/nclaim admin remove balance <amount> <player> {GRAY}- {WHITE}Remove balance from player's account.",
            " {YELLOW}/nclaim admin remove blacklisted_world <world> - {WHITE}Remove blacklisted world",
            " {YELLOW}/nclaim admin remove blacklisted_region <regionId> {GRAY}- {WHITE}Remove region from blacklist.",
            " {YELLOW}/nclaim admin remove blockvalue <material> <value> {GRAY}- {WHITE}Remove block from value list.",
            " {YELLOW}/nclaim admin set blockvalue <material> <value> {GRAY}- {WHITE}Set block value.",
            " {YELLOW}/nclaim admin set balance <amount> <player> {GRAY}- {WHITE}Set balance to player's account.",
            " {YELLOW}/nclaim admin reload {GRAY}- {WHITE}Reload config files.",
            " {YELLOW}/nclaim admin help/? {GRAY}- {WHITE}Show this help message.",
            ""
        };

        for (String line : helpMessage) {
            ChannelType.CHAT.send(player, HexUtil.parse(line));
        }

        return true;
    }
}