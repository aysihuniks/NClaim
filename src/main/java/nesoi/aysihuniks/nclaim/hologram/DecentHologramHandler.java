package nesoi.aysihuniks.nclaim.hologram;

import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Location;

import java.util.List;

public class DecentHologramHandler implements HologramHandler {
    @Override
    public void createHologram(String hologramId, Location location, List<String> lines) {
        if (DHAPI.getHologram(hologramId) == null) {
            DHAPI.createHologram(hologramId, location, true, lines);
        }
    }

    @Override
    public void deleteHologram(String hologramId) {
        if (DHAPI.getHologram(hologramId) != null) {
            DHAPI.removeHologram(hologramId);
        }
    }
}
