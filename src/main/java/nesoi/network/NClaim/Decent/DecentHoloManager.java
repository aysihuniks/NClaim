package nesoi.network.NClaim.Decent;

import eu.decentsoftware.holograms.api.DHAPI;
import nesoi.network.NClaim.NCoreMain;
import nesoi.network.NClaim.models.ClaimDataManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nandayo.DAPI.HexUtil;

import java.util.List;
import java.util.stream.Stream;

public class DecentHoloManager {

    public void createDecentHologram(Player p, Location location) {
        Chunk chunk = location.getChunk();
        ClaimDataManager claimDataManager = NCoreMain.inst().claimDataManager;

        if (claimDataManager == null) {
            p.sendMessage("Claim data manager was not found.");
            return;
        }

        String name = "claim_" + chunk.getX() + "_" + chunk.getZ();
        if (DHAPI.getHologram(name) != null) {
            return;
        }

        List<String> lines = Stream.of("{WHITE}Owner: {GRAY}%nclaim_owner_" + chunk.getX() + "_" + chunk.getZ() + "%",
                "{WHITE}Time left: %nclaim_expiration_" + chunk.getX() + "_" + chunk.getZ() + "%", "", "{YELLOW}Right click for edit").map(HexUtil::parse).toList();

        DHAPI.createHologram(name, location, true, lines);

    }
}
