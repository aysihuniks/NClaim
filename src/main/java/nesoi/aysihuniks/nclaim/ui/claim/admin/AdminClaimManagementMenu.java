package nesoi.network.NClaim.menus.claim.admin.inside;

import nesoi.network.NClaim.menus.ConfirmMenu;
import nesoi.network.NClaim.menus.claim.coop.ManageMenu;
import nesoi.network.NClaim.menus.claim.land.ExpandMenu;
import nesoi.network.NClaim.model.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.Menu;
import org.nandayo.DAPI.ItemCreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class ManageClaimMenu extends Menu {

    private final @NotNull Claim claim;

    public ManageClaimMenu(Player p, @NotNull Claim claim) {
        createInventory(9*4, "NClaim - " + Bukkit.getOfflinePlayer(claim.getOwner()).getName() + "'s Claim");
        this.claim = claim;

        setup();
        displayTo(p);
    }

    private void setup() {

        addButton(new Button(27) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.ARROW)
                        .name("{YELLOW}Go Back")
                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                new AllClaim(player, null, true, 0, new ArrayList<>());
            }
        });


        addButton(new Button(11) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BEEHIVE)
                        .name("{BROWN}Manage Coop Players")
                        .lore("",
                                "{WHITE}You can {GRAY}view{WHITE}, {GRAY}modify{WHITE}, and",
                                "{WHITE}add {GRAY}all the players {WHITE}added",
                                "{WHITE}to the claim {GRAY}within the menu{WHITE}.",
                                "",
                                "{YELLOW}Click to edit")
                        .hideFlag(ItemFlag.values())
                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                new ManageMenu(player, claim, true);
            }
        });

        addButton(new Button(13) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BRICKS)
                        .name("{BROWN}Manage Lands")
                        .lore("",
                                "{WHITE}You can {GRAY}view {WHITE}the claim's lands,",
                                "{GRAY}delete {WHITE}them, and {GRAY}add {WHITE}new ones.",
                                "",
                                "{YELLOW}Click to edit")
                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                new ExpandMenu(player, claim, true);
            }
        });

        addButton(new Button(15) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.CLOCK)
                        .name("{BROWN}Manage Expiration Time")
                        .lore("",
                                "{WHITE}You can {GRAY}view {WHITE}the {GRAY}expiration date",
                                "{WHITE}of the claim, {GRAY}modify {WHITE}it, and {GRAY}add",
                                "{WHITE}or {GRAY}remove {WHITE}time from the date.",
                                "",
                                "{YELLOW}Click to edit")
                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                new ManageTimeMenu(player, 0, 0,0, 0, claim);
            }
        });

        addButton(new Button(35) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BARRIER)
                        .name("{BROWN}Delete Claim")
                        .lore("",
                                "{WHITE}If you {GRAY}choose {WHITE}to perform",
                                "{WHITE}this action, the claim will",
                                "{WHITE}be {GRAY}permanently deleted",
                                "{WHITE}and {GRAY}cannot {WHITE}be {GRAY}restored{WHITE}.")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {

                Consumer<String> onFinish = (result) -> {
                    if ("confirmed".equals(result)) {
                        claim.remove();
                        new AllClaim(p, null, true, 0, new ArrayList<>());
                    } else if ("declined".equals(result)) {
                        new ManageClaimMenu(p, claim);
                    }
                };

                new ConfirmMenu(p, "Delete the Claim", Arrays.asList("", "{WHITE}If you {GRAY}approve {WHITE}this action,", "{WHITE}the claim {GRAY}permanently {WHITE}will be","{GRAY}deleted {WHITE}and {GRAY}cannot be restored{WHITE}."), onFinish);
            }
        });

    }
}
