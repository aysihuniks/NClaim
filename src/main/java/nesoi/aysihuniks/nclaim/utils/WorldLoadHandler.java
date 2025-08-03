package nesoi.aysihuniks.nclaim.utils;

import nesoi.aysihuniks.nclaim.NClaim;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldLoadHandler implements Listener {

    private final NClaim plugin;

    public WorldLoadHandler(NClaim plugin) {
        this.plugin = plugin;
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onWorldLoad(WorldLoadEvent event) {
        String worldName = event.getWorld().getName();
        if (this.plugin.getHologramManager() != null) {
            this.plugin.getHologramManager().onWorldLoaded(worldName);
        }

        this.plugin.getClaimStorageManager().loadClaims();
    }
}
