package nesoi.aysihuniks.nclaim.model;

import lombok.Getter;
import lombok.Setter;
import nesoi.aysihuniks.nclaim.enums.Setting;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public class SettingData {

    private Setting setting;
    private String configKey;
    private ItemStack material;

    public SettingData(Setting setting, String configKey, ItemStack material) {
        this.setting = setting;
        this.configKey = configKey;
        this.material = material;
    }
}
