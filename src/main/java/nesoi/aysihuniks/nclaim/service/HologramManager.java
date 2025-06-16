package nesoi.aysihuniks.nclaim.service;

import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.HoloEnum;
import nesoi.aysihuniks.nclaim.hologram.DecentHologramHandler;
import nesoi.aysihuniks.nclaim.hologram.FancyHologramHandler;
import nesoi.aysihuniks.nclaim.model.Claim;
import nesoi.aysihuniks.nclaim.hologram.HologramHandler;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.nandayo.dapi.HexUtil;

import java.util.ArrayList;
import java.util.List;

public class HologramManager {
    private final NClaim plugin;
    private static final double DECENT_HOLO_OFFSET = 3;
    private static final double FANCY_HOLO_OFFSET = 1.5;
    private HologramHandler hologramHandler;

    public HologramManager(NClaim plugin) {
        this.plugin = plugin;
        initializeHologramHandler();
    }

    private void initializeHologramHandler() {
        if (HoloEnum.getActiveHologram() == HoloEnum.DECENT_HOLOGRAM) {
            hologramHandler = new DecentHologramHandler();
        } else if (HoloEnum.getActiveHologram() == HoloEnum.FANCY_HOLOGRAM) {
            hologramHandler = new FancyHologramHandler();
        } else {
            throw new IllegalStateException("No supported hologram plugin found!");
        }
    }

    public void createHologram(Location location) {
        Chunk chunk = location.getChunk();
        Claim claim = Claim.getClaim(chunk);
        if (claim == null) return;

        String hologramId = getHologramId(chunk);
        List<String> lines = generateHologramLines(claim);
        Location adjustedLocation = getAdjustedLocation(location.clone());

        deleteHologram(chunk);
        hologramHandler.createHologram(hologramId, adjustedLocation, lines);
    }

    public void deleteHologram(Chunk chunk) {
        String hologramId = getHologramId(chunk);
        hologramHandler.deleteHologram(hologramId);
    }

    private Location getAdjustedLocation(Location location) {
        double offset = HoloEnum.getActiveHologram() == HoloEnum.DECENT_HOLOGRAM ?
                DECENT_HOLO_OFFSET : FANCY_HOLO_OFFSET;
        return location.add(0.5, offset, 0.5);
    }

    private String getHologramId(Chunk chunk) {
        return "claim_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
    }

    private List<String> generateHologramLines(Claim claim) {
        List<String> lines = new ArrayList<>();
        Chunk chunk = claim.getChunk();

        lines.add(plugin.getLangManager().getString("hologram.title"));
        lines.add(plugin.getLangManager().getString("hologram.owner")
                .replace("{owner}", "%nclaim_owner_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));
        lines.add(plugin.getLangManager().getString("hologram.time_left")
                .replace("{time_left}", "%nclaim_expiration_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));

        int coopCount = claim.getCoopPlayers().size();
        if (coopCount > 0) {
            lines.add(plugin.getLangManager().getString("hologram.coop_count")
                    .replace("{coop_count}", "%nclaim_coop_count_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));
        }

        lines.add(plugin.getLangManager().getString("hologram.total_size")
                .replace("{total_size}", "%nclaim_total_size_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%"));
        lines.add("");
        lines.add(HexUtil.parse("{YELLOW}Right click for edit"));

        return lines.stream()
                .map(HexUtil::parse)
                .collect(java.util.stream.Collectors.toList());
    }
}