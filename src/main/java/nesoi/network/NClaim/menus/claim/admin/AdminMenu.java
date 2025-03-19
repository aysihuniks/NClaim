package nesoi.network.NClaim.menus.claim.admin;

import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.menus.claim.admin.inside.AllClaim;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.guimanager.Menu;
import org.nandayo.DAPI.ItemCreator;
import org.nandayo.DAPI.object.DMaterial;

import java.util.ArrayList;

public class AdminMenu extends Menu {

    public AdminMenu(Player p) {
        createInventory(9*3, "NClaim - Admin Menu");

        setup();
        displayTo(p);
    }

    private void setup() {
        addButton(new Button(0) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(NCoreMain.getMaterial(DMaterial.AMETHYST_BLOCK, DMaterial.PURPUR_BLOCK))
                        .name("{BROWN}Manage All Claims")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                new AllClaim(p, null, true, 0, new ArrayList<>());
            }
        });
    }
}
