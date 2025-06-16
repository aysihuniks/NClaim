package nesoi.aysihuniks.nclaim.model;

import nesoi.aysihuniks.nclaim.enums.Setting;

import java.util.HashMap;

public class ClaimSetting {

    public final HashMap<Setting, Boolean> settings = new HashMap<Setting, Boolean>() {{
        put(Setting.CLAIM_PVP, false);
        put(Setting.TNT_DAMAGE, true);
        put(Setting.CREEPER_DAMAGE, true);
        put(Setting.MOB_ATTACKING, false);
        put(Setting.MONSTER_SPAWNING, true);
        put(Setting.ANIMAL_SPAWNING, true);
        put(Setting.VILLAGER_INTERACTION, false);
    }};

    public boolean isEnabled(Setting setting) {
        return settings.get(setting);
    }

    public void set(Setting setting, boolean value) {
        settings.put(setting, value);
    }

}