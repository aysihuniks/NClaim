package nesoi.network.NClaim.Fancy;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.models.ClaimDataManager;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.nandayo.DAPI.HexUtil;

import java.util.stream.Stream;

import static org.nandayo.DAPI.Util.log;

public class FancyHoloManager {

    public void createFancyHologram(Player p, Location location) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        Chunk chunk = location.getChunk();
        ClaimDataManager claimDataManager = NCoreMain.inst().claimDataManager;

        if (claimDataManager == null) {
            p.sendMessage("Claim data manager was not found.");
            return;
        }

        String name = "claim_" + chunk.getX() + "_" + chunk.getZ();
        Hologram hologramGetName = manager.getHologram(name).orElse(null);
        if (hologramGetName != null) {
            log(hologramGetName + " cannot be spawn because already have.");
            return;
        }

        TextHologramData hologramData = new TextHologramData(name, location);
        hologramData.setBackground(Color.fromARGB(0, 0, 0, 0));
        hologramData.setBillboard(Display.Billboard.CENTER);
        hologramData.setTextUpdateInterval(20 * 30);
        hologramData.setText(
                Stream.of("{WHITE}Owner: {GRAY}%nclaim_owner_" + chunk.getX() + "_" + chunk.getZ() + "%",
                        "{WHITE}Time left: %nclaim_expiration_" + chunk.getX() + "_" + chunk.getZ() + "%", "", "{YELLOW}Right click for edit").map(HexUtil::parse).toList());

        String worldName = claimDataManager.getClaimWorld(chunk.getX(), chunk.getZ());
        if (worldName == null) {
            log("world name is null.");
            return;
        }

        Hologram hologram = manager.create(hologramData);

        manager.addHologram(hologram);
    }
}
