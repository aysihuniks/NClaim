package nesoi.aysihuniks.nclaim.commands.root;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.commands.BaseCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.nandayo.dapi.HexUtil;

public class AboutCommand extends BaseCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(NClaim.inst().getLangManager().getString("command.must_be_player"));
            return true;
        }

        PluginDescriptionFile descriptionFile = NClaim.inst().getDescription();
        String pluginName = descriptionFile.getName();
        String pluginVersion = descriptionFile.getVersion();
        String pluginAuthor = String.join(", ", descriptionFile.getAuthors());
        String resourceLink = "https://www.spigotmc.org/resources/nclaim-advanced-claim-system.122527/";

        sender.sendMessage(HexUtil.parse("{WHITE}This server is running {ORANGE}" + pluginName + " " + pluginVersion + " {WHITE}by {ORANGE}" + pluginAuthor));
        sender.sendMessage(HexUtil.parse("{GRAY}" + resourceLink));

        return true;
    }
}