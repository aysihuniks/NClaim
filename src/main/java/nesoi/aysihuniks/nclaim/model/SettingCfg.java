package nesoi.aysihuniks.nclaim.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SettingCfg {
    private boolean defaultValue;
    private boolean changeable;
    private String permission;
    private String displayName;
    private String material;
    private List<String> lore;

    public SettingCfg(boolean defaultValue, boolean changeable, String permission, String displayName, String material, List<String> lore) {
        this.defaultValue = defaultValue;
        this.changeable = changeable;
        this.permission = permission;
        this.displayName = displayName;
        this.material = material;
        this.lore = lore;
    }
}
