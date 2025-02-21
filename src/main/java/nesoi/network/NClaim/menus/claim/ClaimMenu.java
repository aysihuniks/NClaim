package nesoi.network.NClaim.menus.claim;


import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.claim.coop.ManageCoopsMenu;
import nesoi.network.NClaim.menus.claim.land.ExpandClaimMenu;
import nesoi.network.NClaim.utils.ChunkBorderManager;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.GUIManager.Button;
import org.nandayo.DAPI.GUIManager.Menu;
import org.nandayo.DAPI.ItemCreator;


import static nesoi.network.NClaim.utils.HeadManager.getPlayerHead;

public class ClaimMenu extends Menu {

    public ClaimMenu(Player p, Chunk chunk) {
        this.setSize(9*3);
        this.setTitle("NClaim - Manage Claim");

        ChunkBorderManager chunkBorderManager = new ChunkBorderManager();
        chunkBorderManager.closeChunkBorder(p);

        this.addButton(new Button(11) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRASS_BLOCK)
                        .name("{BROWN}Manage Chunks")
                        .lore(
                                "",
                                "{GRAY}Expand {WHITE}your {GRAY}Claim {WHITE}here.",
                                "{WHITE}No need to buy a new claim.",
                                "",
                                "{YELLOW}Click to expand your Claim."
                        )
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new ExpandClaimMenu(p, chunk);
            }
        });

        this.addButton(new Button(13) {
            @Override
            public ItemStack getItem() {
                ItemStack itemStack;
                itemStack = getPlayerHead(p.getPlayer());
                itemStack = ItemCreator.of(itemStack)
                        .name("{BROWN}Manage Members")
                        .lore(
                                "",
                                "{WHITE}Manage your claim's {GRAY}members{WHITE} here.",
                                "{WHITE}Easily {GRAY}add{WHITE} or {GRAY}remove{WHITE} members from your claim.",
                                "",
                                "{YELLOW}Click to manage your claim's members."
                        )
                        .get();
                return itemStack;
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new ManageCoopsMenu(NCoreMain.inst(), p, chunk);
            }
        });



        this.addButton(new Button(15) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.END_CRYSTAL)
                        .name("{BROWN}Manage Claim Settings")
                        .lore(
                                "",
                                "{WHITE}Manage your claim's {GRAY}settings.",
                                "{WHITE}Easily {GRAY}adjust {WHITE}permissions for everyone.",
                                "",
                                "{YELLOW}Click to manage claim settings."
                        )

                        .get();
            }

            @Override
            public void onClick(Player player, ClickType clickType) {
                new ClaimSettingsMenu(p, chunk);
            }
        });

        displayTo(p);
    }
}
