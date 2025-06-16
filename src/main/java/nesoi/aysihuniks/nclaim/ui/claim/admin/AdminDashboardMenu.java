package nesoi.aysihuniks.nclaim.ui.claim.admin;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.ui.shared.BackgroundMenu;
import nesoi.aysihuniks.nclaim.ui.shared.BaseMenu;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.guimanager.Button;
import org.nandayo.dapi.ItemCreator;
import org.nandayo.dapi.guimanager.MenuType;
import org.nandayo.dapi.object.DMaterial;

import java.util.ArrayList;
import java.util.Set;

public class AdminDashboardMenu extends BaseMenu {

    public AdminDashboardMenu(Player player) {
        super("menu.admin.main_menu");

        setupMenu();
        displayTo(player);
    }

    private void setupMenu() {
        createInventory(MenuType.CHEST_3_ROWS, getString("title"));
        setBackgroundButton(BackgroundMenu::getButton);
        addManageClaimsButton();
    }

    private void addManageClaimsButton() {
        addButton(new Button() {
            final String buttonPath = "manage_claims";

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(NClaim.getMaterial(DMaterial.AMETHYST_BLOCK, DMaterial.SHULKER_BOX))
                        .name(getString(buttonPath + ".display_name"))
                        .lore(getStringList(buttonPath + ".lore"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, ClickType clickType) {
                MessageType.MENU_FORWARD.playSound(player);
                new AdminAllClaimMenu(player, null, true, 0, new ArrayList<>());
            }
        });
    }
}