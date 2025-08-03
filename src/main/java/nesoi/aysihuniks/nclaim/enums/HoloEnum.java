package nesoi.aysihuniks.nclaim.enums;

import org.bukkit.Bukkit;
import org.nandayo.dapi.util.Util;

public enum HoloEnum {
    DECENT_HOLOGRAM,
    FANCY_HOLOGRAM;

    private static HoloEnum activeHologram = null;

    public static HoloEnum getActiveHologram() {
        if (activeHologram == null) {
            if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null) {
                Util.log("&aHologram plugin detected. Using DecentHolograms");
                activeHologram = DECENT_HOLOGRAM;
            } else if (Bukkit.getPluginManager().getPlugin("FancyHolograms") != null) {
                Util.log("&aHologram plugin detected. Using FancyHolograms");
                activeHologram = FANCY_HOLOGRAM;
            }
        }
        return activeHologram;
    }

    public static boolean isHologramPluginEnabled() {
        return getActiveHologram() != null;
    }

}
