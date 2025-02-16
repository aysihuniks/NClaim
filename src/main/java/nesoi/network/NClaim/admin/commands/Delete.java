package nesoi.network.NClaim.admin.commands;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.ConfirmMenu;
import nesoi.network.NClaim.models.ClaimDataManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public class Delete {

    public void execute(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.wrong-usage"));
            return;
        }

        if (!player.hasPermission("nclaim.delete") || !player.hasPermission("nclaim.admin")) {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.dont-have-a-permission"));
            return;
        }

        String value = args[2];


        if (value.equalsIgnoreCase("claim")) {
            ClaimDataManager claimDataManager = NCoreMain.inst().claimDataManager;

            Consumer<String> onFinish = (result) -> {
                if ("confirmed".equals(result)) {
                    claimDataManager.removeClaim(player);
                    player.closeInventory();
                } else if ("declined".equals(result))  {
                    player.closeInventory();
                }
            };

            new ConfirmMenu(player, "Delete Claim", List.of("", "{WHITE}If you {GRAY}approve {WHITE}this action,", "{WHITE}the claim {GRAY}you are on {WHITE}will be {GRAY}deleted{WHITE}."), onFinish);

        }

         else {
            player.sendMessage(NCoreMain.inst().langManager.getMsg("messages.enter-a-valid-data"));
        }

    }
}
