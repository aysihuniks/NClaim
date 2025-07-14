package nesoi.aysihuniks.nclaim.commands.root;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.HexUtil;
import org.nandayo.dapi.message.ChannelType;

public class AboutCommand extends BaseCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            ChannelType.CHAT.send(sender, NClaim.inst().getLangManager().getString("command.must_be_player"));
            return true;
        }

        PluginDescriptionFile descriptionFile = NClaim.inst().getDescription();
        String pluginName = descriptionFile.getName();
        String pluginVersion = descriptionFile.getVersion();
        String pluginAuthor = String.join(", ", descriptionFile.getAuthors());
        String resourceLink = "https://www.spigotmc.org/resources/nclaim-advanced-claim-system.122527/";

        ChannelType.CHAT.send(sender, HexUtil.parse("{WHITE}This server is running {ORANGE}" + pluginName + " " + pluginVersion + " {WHITE}by {ORANGE}" + pluginAuthor));
        ChannelType.CHAT.send(sender, HexUtil.parse("{GRAY}" + resourceLink));

        return true;
    }
}