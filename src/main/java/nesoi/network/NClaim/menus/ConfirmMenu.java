package nesoi.network.NClaim.menus;

import org.nandayo.DAPI.guimanager.Button;
import org.nandayo.DAPI.ItemCreator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.nandayo.DAPI.guimanager.LazyButton;
import org.nandayo.DAPI.guimanager.Menu;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ConfirmMenu extends Menu {

    private final Consumer<String> onFinish;
    private final String itemName;
    private final List<String> lore;

    public ConfirmMenu(Player p, String itemName, List<String> lore, Consumer<String> onFinish) {
        createInventory(9 * 5, "Do you approve this action");
        this.itemName = itemName;
        this.onFinish = onFinish;
        this.lore = lore;
        addLazyButton(new LazyButton(Arrays.asList(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)) {

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").get();
            }
        });
        setup();
        displayTo(p);
    }

    private void setup() {
        addButton(new Button(22) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BOOK)
                        .name(("{BROWN}" + itemName))
                        .lore(lore)
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {

            }
        });

        addButton(new Button(20) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GREEN_DYE)
                        .name("{GREEN}Confirm")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                onFinish.accept("confirmed");
            }
        });

        addButton(new Button(24) {
            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.RED_DYE)
                        .name("{RED}Decline")
                        .get();
            }

            @Override
            public void onClick(Player p, ClickType clickType) {
                onFinish.accept("declined");
            }
        });

    }
}
