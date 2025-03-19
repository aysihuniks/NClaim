package nesoi.network.NClaim.menus.claim;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.ConfirmMenu;
import nesoi.network.NClaim.menus.claim.admin.AdminMenu;
import nesoi.network.NClaim.model.Claim;
import nesoi.network.NClaim.model.User;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.nandayo.DAPI.ItemCreator;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.Menu;

import java.util.Arrays;

import java.util.function.Consumer;

public class MainMenu extends Menu {

    public MainMenu(Player p){
        createInventory(9*3, "NClaim - General");

        if (p.hasPermission("nclaim.admin")) {
            addButton(new Button(26) {
                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(Material.END_PORTAL_FRAME).name("{ORANGE}Admin Menu").get();
                }

                @Override
                public void onClick(Player player, ClickType clickType) {
                    new AdminMenu(player);
                }
            });
        }
        setup();
        displayTo(p);
    }

    public void setup()  {
        addButton(new Button(12) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRASS_BLOCK)
                        .name("{BROWN}Buy a Claim")
                        .lore("",
                                "{GRAY}If you have enough money ({WHITE}" + NCoreMain.inst().configManager.getInt("claim-buy-price", 1500) + "${GRAY}), ",
                                "{GRAY}you can buy a {WHITE}new Claim{GRAY}!",
                                "",
                                "{YELLOW}Click to purchase a Claim.")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                Consumer<String> onFinish = (result) -> {
                    if ("confirmed".equals(result)) {
                        p.closeInventory();
                        Claim.buy(p);
                    } else if ("declined".equals(result))  {
                        new MainMenu(p);
                    }
                };

                new ConfirmMenu(p, "Buy a Claim", Arrays.asList("", "{GRAY}If you approve this action,", "{WHITE}" + NCoreMain.inst().configManager.getInt("claim_buy_price", 1500) + "$ will be {WHITE}removed {GRAY}from", "{GRAY}your {WHITE}balance {GRAY}and you will", "{GRAY}buy a {WHITE}new claim{GRAY}."), onFinish);
            }
        });

        addButton(new Button(14) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BEDROCK)
                        .name("{BROWN}Manage Claims")
                        .lore("",
                                "{GRAY}Do you want to {WHITE}manage {GRAY}it",
                                "{WHITE}remotely {GRAY}without going to {WHITE}Claim{GRAY}?",
                                "",
                                "{YELLOW}Just click here and manage.")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                User user = User.getUser(p.getUniqueId());
                if (!user.getPlayerClaims().isEmpty() || !user.getCoopClaims().isEmpty()) {
                    new ListMenu(p, 0);
                } else {
                    p.closeInventory();
                }

            }
        });
    }
}
