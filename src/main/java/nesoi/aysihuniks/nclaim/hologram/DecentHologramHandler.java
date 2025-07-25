package nesoi.aysihuniks.nclaim.hologram;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHolograms;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.nandayo.dapi.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public List<String> getHologramIds() {
        return Hologram.getCachedHologramNames().stream()
                .filter(id -> id != null && id.startsWith("claim_"))
                .collect(Collectors.toList());
    }
}
