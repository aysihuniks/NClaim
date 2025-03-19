package nesoi.network.NClaim.model;

import java.util.HashMap;

public class ClaimSetting {

    public enum Setting {
        CLAIM_PVP,
        TNT_DAMAGE,
        CREEPER_DAMAGE,
        MOB_ATTACKING,
        MONSTER_SPAWNING,
        ANIMAL_SPAWNING,
        VILLAGER_INTERACTION
    }

    public ClaimSetting() {

    }

    public final HashMap<ClaimSetting.Setting, Boolean> settings = new HashMap<ClaimSetting.Setting, Boolean>() {{
        put(Setting.CLAIM_PVP, false);
        put(Setting.TNT_DAMAGE, true);
        put(Setting.CREEPER_DAMAGE, true);
        put(Setting.MOB_ATTACKING, false);
        put(Setting.MONSTER_SPAWNING, true);
        put(Setting.ANIMAL_SPAWNING, true);
        put(Setting.VILLAGER_INTERACTION, false);
    }};

    public boolean isEnabled(ClaimSetting.Setting setting) {
        return settings.get(setting);
    }

    public void set(ClaimSetting.Setting setting, boolean value) {
        settings.put(setting, value);
    }

    public void toggle(ClaimSetting.Setting setting) {
        set(setting, !isEnabled(setting));
    }

}
