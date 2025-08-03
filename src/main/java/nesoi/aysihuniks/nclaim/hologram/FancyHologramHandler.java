package nesoi.aysihuniks.nclaim.hologram;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.nandayo.dapi.util.Util;

import java.util.List;
import java.util.stream.Collectors;

public class FancyHologramHandler implements HologramHandler {
    @Override
    public void createHologram(String hologramId, Location location, List<String> lines) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        if (!manager.getHologram(hologramId).isPresent()) {
            TextHologramData hologramData = new TextHologramData(hologramId, location);
            hologramData.setText(lines);
            hologramData.setBillboard(Display.Billboard.CENTER);
            Hologram hologram = manager.create(hologramData);
            manager.addHologram(hologram);
        }
    }

    @Override
    public void deleteHologram(String hologramId) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        manager.getHologram(hologramId).ifPresent(manager::removeHologram);
    }

    @Override
    public List<String> getHologramIds() {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        return manager.getHolograms().stream()
                .map(Hologram::getName)
                .collect(java.util.stream.Collectors.toList());
    }

}
