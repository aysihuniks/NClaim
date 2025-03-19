package nesoi.network.NClaim.executors;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.claim.MainMenu;
import nesoi.network.NClaim.model.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.nandayo.DAPI.HexUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MainCommandExecutor implements CommandExecutor, TabCompleter {

    private final AdminCommandExecutor adminCommandExecutor = new AdminCommandExecutor();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            new MainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "admin":
                adminCommandExecutor.onCommand(sender, command, label, args);
                break;
            case "?":
            case "help":
                sendHelpCommands(player);
                break;
            case "about":
                sendAboutMessage(player);
                break;
            case "balance":
                getPlayerBalance(player);
                break;
            default:
                player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.wrong-usage"));
                break;
        }

        return true;
    }

    public void getPlayerBalance(Player p) {
        p.sendMessage(NCoreMain.inst().langManager.getMsg("messages.reminder.player-balance", User.getUser(p.getUniqueId()).getBalance()));
    }

    public void sendAboutMessage(Player player) {
        PluginDescriptionFile descriptionFile = NCoreMain.inst().getDescription();
        String pluginName = descriptionFile.getName();
        String pluginVersion = descriptionFile.getVersion();
        String pluginAuthor = String.join(", ", descriptionFile.getAuthors());
        String resourceLink = "https://www.spigotmc.org/resources/nclaim-advanced-claim-system.122527/";
        List<String> aboutMessages = Stream.of(
                "{WHITE}This server is running {ORANGE}" + pluginName + " " + pluginVersion + " {WHITE}by {ORANGE}" + pluginAuthor,
                "{GRAY}" + resourceLink
        ).map(HexUtil::parse).collect(Collectors.toList());

        for (String messages : aboutMessages) {
            player.sendMessage(messages);
        }
    }


    public void sendHelpCommands(Player player) {

        if (!player.hasPermission("nclaim.help") || !player.hasPermission("nclaim.admin")){
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.error.not-enough-permission"));
            return;
        }

        List<String> helpMessages = Stream.of(
                "",
                "{BROWN}NClaim All General Commands",
                " {YELLOW}/nclaim balance {GRAY}- {WHITE}Show the player balance.",
                " {YELLOW}/nclaim about {GRAY}- {WHITE}Show information about this plugin.",
                " {YELLOW}/nclaim admin {GRAY}- {WHITE}Show all admin commands.",
                " {YELLOW}/nclaim help/? {GRAY}- {WHITE}Send this help messages.",
                " {YELLOW}/nclaim {GRAY}- {WHITE}Open the Claim menu.",
                ""
        ).map(HexUtil::parse).collect(Collectors.toList());

        for (String messages : helpMessages) {
            player.sendMessage(messages);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (sender.hasPermission("nclaim.admin") || sender.isOp()) {
            /*
             * /nclaim
             */
            if (args.length == 1) {
                suggestions.add("admin");
                suggestions.add("balance");
                suggestions.add("about");
                suggestions.add("help");
                suggestions.add("?");
                return suggestions;
            }
            /*
             * /nclaim admin
             */
            if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
                suggestions.add("menu");
                suggestions.add("add");
                suggestions.add("remove");
                suggestions.add("reload");
                suggestions.add("change");
                return suggestions;
            }
            /*
             * /nclaim admin change
             */
            if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("change")) {
                suggestions.add("lang");
                return suggestions;
            }
            /*
             * /nclaim admin change lang
             */
            if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("change") && args[2].equalsIgnoreCase("lang")) {
                suggestions.add("en-US");
                suggestions.add("tr-TR");
                return suggestions;
            }
            /*
             * /nclaim admin add||remove
             */
            if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
                if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) {
                    suggestions.add("balance");
                    suggestions.add("blacklisted-world");
                }
                return suggestions;
            }
            /*
             * /nclaim admin add||remove balance
             */
            if (args.length == 4 && args[0].equalsIgnoreCase("admin") &&
                    (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) &&
                    args[2].equalsIgnoreCase("balance")) {
                suggestions.add("<amount>");
                return suggestions;
            }

            if (args.length == 5 && args[0].equalsIgnoreCase("admin") &&
                    (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) &&
                    args[2].equalsIgnoreCase("balance")) {
                return null;
            }
            /*
             * /nclaim admin remove blacklisted-world
             */
            if (args.length == 4 && args[0].equalsIgnoreCase("admin") &&
                    args[1].equalsIgnoreCase("remove") &&
                    args[2].equalsIgnoreCase("blacklisted-world")) {

                List<String> blacklistedWorlds = NCoreMain.inst().configManager.getStringList("blacklisted-worlds");
                return new ArrayList<>(blacklistedWorlds);
            }
        } else {
            if (args.length == 1) {
                suggestions.add("balance");
                suggestions.add("about");
                suggestions.add("help");
                suggestions.add("?");
                return suggestions;
            }
        }

        return suggestions;
    }

}
