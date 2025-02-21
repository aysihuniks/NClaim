package nesoi.network.NClaim.menus.claim;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.models.ClaimDataManager;
import nesoi.network.NClaim.utils.ChunkBorderManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.C;
import org.nandayo.DAPI.ItemCreator;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.GUIManager.Button;
import org.nandayo.DAPI.GUIManager.Menu;

import java.util.List;

public class MainMenu extends Menu {

    public MainMenu(Player p){
        this.createInventory(9*3, "NClaim - General");
        setup();
        ChunkBorderManager chunkBorderManager = new ChunkBorderManager();
        chunkBorderManager.closeChunkBorder(p);
        displayTo(p);
    }

    public void setup()  {
        this.addButton(new Button(12) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRASS_BLOCK)
                        .name("{BROWN}Buy a Claim")
                        .lore(List.of(
                                "",
                                "{GRAY}If you have enough money ({WHITE}" + NCoreMain.inst().configManager.getInt("claim-buy-price", 1500) + "${GRAY}), ",
                                "{GRAY}you can buy a {WHITE}new Claim{GRAY}!",
                                "",
                                "{YELLOW}Click to purchase a Claim."))
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                ClaimDataManager claimDataManager = NCoreMain.inst().claimDataManager;
                claimDataManager.checkClaim(p);
            }
        });

        this.addButton(new Button(14) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BEDROCK)
                        .name("{BROWN}Manage Claims")
                        .lore(List.of(
                                "",
                                "{GRAY}Do you want to {WHITE}manage {GRAY}it",
                                "{WHITE}remotely {GRAY}without going to {WHITE}Claim{GRAY}?",
                                "",
                                "{YELLOW}Just click here and manage."))
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new ClaimListMenu(p);
            }
        });
    }
}
