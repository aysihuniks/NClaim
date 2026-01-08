package nesoi.aysihuniks.nclaim.ui.shared;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.utils.GuiLangManager;
import nesoi.aysihuniks.nclaim.utils.MessageType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nandayo.dapi.guimanager.button.SingleSlotButton;
import org.nandayo.dapi.guimanager.menu.Menu;
import org.nandayo.dapi.util.ItemCreator;

import java.util.List;

public abstract class BaseMenu extends Menu {

    protected final GuiLangManager guiLangManager;
    protected final ConfigurationSection menuSection;
    protected final String configPath;

    protected BaseMenu(String menuSectionPath) {
        this.guiLangManager = NClaim.inst().getGuiLangManager();
        this.menuSection = guiLangManager.getSection(menuSectionPath);
        this.configPath = menuSectionPath;
    }

    protected String getString(String path) {
        return guiLangManager.getString(configPath, path);
    }

    protected List<String> getStringList(String path) {
        return guiLangManager.getStringList(configPath, path);
    }

    protected ItemStack getMaterial(String path) {
        return guiLangManager.getMaterial(configPath, path);
    }

    protected ItemStack getMaterialFullPath(String fullPath) {
        return guiLangManager.getMaterial(fullPath);
    }

    @Override
    public @Nullable SingleSlotButton backgroundButton(int slot) {
        return getBackgroundButton(slot);
    }

    public static SingleSlotButton getBackgroundButton(int slot) {
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
