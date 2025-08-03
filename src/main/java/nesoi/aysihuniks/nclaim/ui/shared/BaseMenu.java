package nesoi.aysihuniks.nclaim.ui.shared;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.utils.GuiLangManager;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.nandayo.dapi.guimanager.menu.Menu;

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

}
