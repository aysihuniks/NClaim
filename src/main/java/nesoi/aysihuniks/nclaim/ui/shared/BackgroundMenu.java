package nesoi.aysihuniks.nclaim.ui.shared;

import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.ItemCreator;
import org.nandayo.dapi.guimanager.SingleSlotButton;

public class BackgroundMenu {

    static public SingleSlotButton getButton(int slot) {
        return (slot / 9) % 2 == 0 ? new SingleSlotButton() {
            @Override
            public int getSlot() {
                return slot;
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.BLACK_STAINED_GLASS_PANE).name("&8x").get();
            }
            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.FAIL.playSound(player);
            }
        }
        : new SingleSlotButton() {
            @Override
            public int getSlot() {
                return slot;
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(Material.GRAY_STAINED_GLASS_PANE).name("&8x").get();
            }

            @Override
            public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                MessageType.FAIL.playSound(player);
            }
        };
    }
}
