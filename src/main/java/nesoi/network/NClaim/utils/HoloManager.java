package nesoi.network.NClaim.utils;

import nesoi.network.NClaim.Decent.DecentHoloManager;
import nesoi.network.NClaim.enums.Holo;
import nesoi.network.NClaim.Fancy.FancyHoloManager;
import nesoi.network.NClaim.NCoreMain;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HoloManager {

    public void createClaimHologram(Player p, Location location) {
        if (NCoreMain.inst().hologramSystem == Holo.FANCY_HOLOGRAM) {
            FancyHoloManager fancyHoloManager = new FancyHoloManager();
            fancyHoloManager.createFancyHologram(p, location);
        } else if (NCoreMain.inst().hologramSystem == Holo.DECENT_HOLOGRAM) {
            DecentHoloManager decentHoloManager = new DecentHoloManager();
            decentHoloManager.createDecentHologram(p, location);
        }
    }
}
