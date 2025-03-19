package nesoi.network.NClaim.enums;

import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nandayo.DAPI.HexUtil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DecentHoloManager {

    public void createDecentHologram(Player p, Location location) {
        Chunk chunk = location.getChunk();

        String name = "claim_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
        if (DHAPI.getHologram(name) != null) {
            return;
        }

        List<String> lines = Stream.of("{WHITE}Owner: {GRAY}%nclaim_owner_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%",
                "{WHITE}Time left: %nclaim_expiration_" + chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ() + "%", "", "{YELLOW}Right click for edit").map(HexUtil::parse).collect(Collectors.toList());

        DHAPI.createHologram(name, location, true, lines);

    }
}
