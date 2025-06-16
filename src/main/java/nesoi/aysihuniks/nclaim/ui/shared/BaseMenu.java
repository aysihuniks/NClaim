package nesoi.aysihuniks.nclaim.ui.shared;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.utils.LangManager;
import org.bukkit.configuration.ConfigurationSection;
import org.nandayo.dapi.guimanager.Menu;

import java.util.List;

public abstract class BaseMenu extends Menu {

    protected final LangManager langManager;
    protected final ConfigurationSection menuSection;
    protected final String configPath;

    protected BaseMenu(String menuSectionPath) {
        this.langManager = NClaim.inst().getLangManager();
        this.menuSection = langManager.getSection(menuSectionPath);
        this.configPath = menuSectionPath;
    }

    protected String getString(String path) {
        return langManager.getString(menuSection, path);
    }

    protected List<String> getStringList(String path) {
        return langManager.getStringList(menuSection, path);
    }

}
