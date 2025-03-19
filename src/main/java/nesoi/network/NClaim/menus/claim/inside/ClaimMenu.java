package nesoi.network.NClaim.menus.claim.inside;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.ConfirmMenu;
import nesoi.network.NClaim.menus.claim.coop.ManageMenu;
import nesoi.network.NClaim.menus.claim.land.ExpandMenu;
import nesoi.network.NClaim.model.Claim;
import nesoi.network.NClaim.utils.ChunkBorderManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.Menu;
import org.nandayo.DAPI.ItemCreator;


import java.util.Arrays;
import java.util.function.Consumer;

import static nesoi.network.NClaim.utils.HeadManager.getPlayerHead;

public class ClaimMenu extends Menu {

    public ClaimMenu(Player p, @NotNull Claim claim) {
        createInventory(9*3, "NClaim - Manage Claim");

        ChunkBorderManager chunkBorderManager = NCoreMain.inst().chunkBorderManager;
        chunkBorderManager.closeChunkBorder(p);
        addButton(new Button(26) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BARRIER)
                        .name("{RED}Delete the Claim")
                        .lore("",
                                "{WHITE}If you click this button your",
                                "{WHITE}claim will be {GRAY}deleted forever{WHITE},",
                                "{WHITE}and there will be {GRAY}no price refund{WHITE}.")
                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                Consumer<String> onFinish = (result) -> {
                    if ("confirmed".equals(result)) {
                        p.closeInventory();
                        claim.remove();
                    } else if ("declined".equals(result)) {
                        new ClaimMenu(p, claim);
                    }
                };

                new ConfirmMenu(p, "Delete the Claim", Arrays.asList("", "{WHITE}If you {GRAY}approve {WHITE}this action,", "{WHITE}the claim {GRAY}permanently {WHITE}will be","{GRAY}deleted {WHITE}and {GRAY}cannot be restored{WHITE}."), onFinish);
            }
        });

        addButton(new Button(11) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRASS_BLOCK)
                        .name("{BROWN}Manage Chunks")
                        .lore(
                                "",
                                "{GRAY}Expand {WHITE}your {GRAY}Claim {WHITE}here.",
                                "{WHITE}No need to buy a new claim.",
                                "",
                                "{YELLOW}Click to expand."
                        )
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new ExpandMenu(p, claim, false);
            }
        });

        addButton(new Button(13) {
            @Override
            public ItemStack getItem() {
                ItemStack itemStack;
                itemStack = getPlayerHead(p.getPlayer());
                itemStack = ItemCreator.of(itemStack)
                        .name("{BROWN}Manage Members")
                        .lore(
                                "",
                                "{WHITE}Here you can manage your {GRAY}coop",
                                "{WHITE}members. Easily {GRAY}add {WHITE}or {GRAY}remove",
                                "{WHITE}members from your claim",
                                "",
                                "{YELLOW}Click to manage coops."
                        )
                        .get();
                return itemStack;
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new ManageMenu(p, claim, false);
            }
        });



        addButton(new Button(15) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.END_CRYSTAL)
                        .name("{BROWN}Manage Claim Settings")
                        .lore("",
                                "{WHITE}Manage your claim's {GRAY}settings.",
                                "{WHITE}Easily {GRAY}adjust {WHITE}permissions for everyone.",
                                "",
                                "{YELLOW}Click to manage settings."
                        )

                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                new SettingMenu(p, claim);
            }
        });

        displayTo(p);
    }
}
