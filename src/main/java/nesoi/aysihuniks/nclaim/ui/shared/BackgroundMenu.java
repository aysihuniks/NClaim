package nesoi.aysihuniks.nclaim.ui.shared;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nandayo.dapi.util.ItemCreator;
import org.nandayo.dapi.guimanager.button.SingleSlotButton;

public class BackgroundMenu {

    static public SingleSlotButton getButton(int slot) {
        if (NClaim.inst().getGuiLangManager().getBoolean("background_buttons")) {
            return (slot / 9) % 2 == 0 ? new SingleSlotButton() {
                @Override
                public int getSlot() {
                    return slot;
                }

                @Override
                public ItemStack getItem() {
                    return ItemCreator.of(NClaim.inst().getGuiLangManager().getMaterial("background_1")).name(NClaim.inst().getGuiLangManager().getString("background_1.display_name")).get();
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
                    return ItemCreator.of(NClaim.inst().getGuiLangManager().getMaterial("background_2")).name(NClaim.inst().getGuiLangManager().getString("background_2.display_name")).get();
                }

                @Override
                public void onClick(@NotNull Player player, @NotNull ClickType clickType) {
                    MessageType.FAIL.playSound(player);
                }
            };
        }

        return null;
    }
}
