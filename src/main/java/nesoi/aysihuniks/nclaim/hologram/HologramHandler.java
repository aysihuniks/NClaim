package nesoi.aysihuniks.nclaim.hologram;

import org.bukkit.Location;

import java.util.List;

public interface HologramHandler {
    void createHologram(String hologramId, Location location, List<String> lines);
    void deleteHologram(String hologramId);
    List<String> getHologramIds();
}
