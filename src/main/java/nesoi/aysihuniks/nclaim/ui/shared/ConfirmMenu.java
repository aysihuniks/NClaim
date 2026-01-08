package nesoi.aysihuniks.nclaim.ui.shared;

import com.google.common.collect.Sets;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.guimanager.MenuType;
import org.nandayo.dapi.guimanager.button.SingleSlotButton;
import org.nandayo.dapi.util.ItemCreator;
import org.nandayo.dapi.guimanager.button.Button;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ConfirmMenu extends BaseMenu {
    private final String itemName;
    private final List<String> lore;
    private final Consumer<String> onFinish;

    public ConfirmMenu(Player player, String itemName, List<String> lore, Consumer<String> onFinish) {
        super("confirm_menu");
        
        this.itemName = itemName;
        this.lore = lore;
        this.onFinish = onFinish;

        createInventory(MenuType.CHEST_3_ROWS, getString("title"));
        setup();
        MessageType.MENU_FORWARD.playSound(player);
        displayTo(player);
    }

    private void setup() {
        addInfoButton();
        addConfirmButton();
        addDeclineButton();
    }

    private void addInfoButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(13);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("center"))
                        .name(itemName)
                        .lore(lore)
                        .get();
            }
        });
    }

    private void addConfirmButton() {
        addButton(new Button() {

            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(11);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("confirm"))
                        .name(getString("confirm.display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                onFinish.accept("confirmed");
                MessageType.CONFIRM.playSound(player);
            }
        });
    }

    private void addDeclineButton() {
        addButton(new Button() {
            @Override
            public @NotNull Set<Integer> getSlots() {
                return Sets.newHashSet(15);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(getMaterial("decline"))
                        .name(getString("decline.display_name"))
                        .get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                onFinish.accept("declined");
                MessageType.WARN.playSound(player);
            }
        });
    }
}