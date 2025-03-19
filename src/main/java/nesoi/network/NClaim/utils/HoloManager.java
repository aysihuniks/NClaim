package nesoi.network.NClaim.utils;

import nesoi.network.NClaim.enums.DecentHoloManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HoloManager {

    public void createClaimHologram(Player p, Location location) {
        DecentHoloManager decentHoloManager = new DecentHoloManager();
        decentHoloManager.createDecentHologram(p, location);
    }
}
