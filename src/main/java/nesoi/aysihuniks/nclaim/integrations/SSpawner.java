package nesoi.aysihuniks.nclaim.integrations;

import github.nighter.smartspawner.api.events.SpawnerPlaceEvent;
import github.nighter.smartspawner.api.events.SpawnerPlayerBreakEvent;
import github.nighter.smartspawner.api.events.SpawnerStackEvent;
import nesoi.aysihuniks.nclaim.NClaim;
import nesoi.aysihuniks.nclaim.enums.Permission;
import nesoi.aysihuniks.nclaim.model.Claim;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nandayo.dapi.message.ChannelType;

public class SSpawner implements Listener {

    private final NClaim plugin;

    public SSpawner(NClaim plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawnerBreak(SpawnerPlayerBreakEvent event) {
        if (checkSpawnerPermission(event.getPlayer(), event.getLocation(), Permission.BREAK_SPAWNER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpawnerStack(SpawnerStackEvent event) {
        if (checkSpawnerPermission(event.getPlayer(), event.getLocation(), Permission.PLACE_SPAWNER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpawnerPlace(SpawnerPlaceEvent event) {
        if (checkSpawnerPermission(event.getPlayer(), event.getLocation(), Permission.PLACE_SPAWNER)) {
            event.setCancelled(true);
        }
    }

    private boolean checkSpawnerPermission(Player player, Location location, Permission permission) {
        Chunk chunk = location.getChunk();
        Claim claim = Claim.getClaim(chunk);
        if (claim != null) {
            if (!plugin.getClaimCoopManager().hasPermission(player, claim, permission)) {
                ChannelType.CHAT.send(player, plugin.getLangManager().getString("command.permission_denied"));
                return true;
            }
        }
        return false;
    }
}
